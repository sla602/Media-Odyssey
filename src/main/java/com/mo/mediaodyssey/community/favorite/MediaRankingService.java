package com.mo.mediaodyssey.community.favorite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mo.mediaodyssey.recommendation.UserInteractionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.stream.Collectors;

/**
 * Builds the data shown on the Community Favourites page.
 *
 * The page has two different ranking models:
 * - Top 10: built from all-time likes and views, with popularity score =
 * (views × 1) + (likes × 10).
 * - Fast-Rising: built from likes in the last 7 days only.
 *
 * The service also keeps metadata caching separate from ranking counts:
 * - Title, artist, and image metadata are cached to avoid repeated external
 * API calls.
 * - Like/view counters are always read fresh from the database so the next
 * page load reflects new activity.
 */
@Service
public class MediaRankingService {

    private final UserInteractionRepository userInteractionRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Cross-request metadata cache for external API responses.
     * Keys are normalized "MEDIA_TYPE|mediaApiId".
     */
    private final Map<String, MediaMetadata> metadataCache = new ConcurrentHashMap<>();

    /**
     * Per-request metadata cache to avoid repeated lookups in a single response.
     * This is cleared after each /community/data request.
     */
    private final ThreadLocal<Map<String, MediaMetadata>> requestCache = ThreadLocal
            .withInitial(ConcurrentHashMap::new);

    /**
     * Deduplicates DB backfill writes within a single request.
     */
    private final ThreadLocal<Set<String>> requestImageBackfills = ThreadLocal
            .withInitial(ConcurrentHashMap::newKeySet);

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    @Value("${rawg.api.key}")
    private String rawgApiKey;

    @Value("${lastfm.api.key}")
    private String lastfmApiKey;

    /**
     * Creates a ranking service backed by the interaction repository and
     * metadata client.
     */
    public MediaRankingService(UserInteractionRepository userInteractionRepository, RestTemplate restTemplate) {
        this.userInteractionRepository = userInteractionRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Returns the overall Top 10 list across all categories.
     *
     * The ranking is based on all-time likes and views. Each item is scored
     * using the database formula (views × 1) + (likes × 10).
     */
    public List<RankedMediaResponse> getTop10() {
        List<Object[]> rows = userInteractionRepository.findTop10ByScoreWithCounts();
        return enrichScoreRows(rows);
    }

    /**
     * Returns the Top 10 list for a single media category.
     *
     * The ranking is based on all-time likes and views for the requested
     * category only.
     */
    public List<RankedMediaResponse> getTop10ByMediaType(String mediaType) {
        List<Object[]> rows = userInteractionRepository
                .findTop10ByScoreWithCountsAndMediaType(normalizeMediaType(mediaType));
        return enrichScoreRows(rows);
    }

    /**
     * Returns the Top 10 response map for the Community Favourites page.
     *
     * The ALL bucket contains the overall Top 10 ranking. The category buckets
     * contain the Top 10 for MOVIE, GAME, and SONG.
     */
    public Map<String, List<RankedMediaResponse>> getTop10PerCategory() {
        Map<String, List<RankedMediaResponse>> result = new LinkedHashMap<>();

        // Top 10 overall (sorted by score across all categories)
        List<RankedMediaResponse> top10Overall = getTop10();
        result.put("ALL", top10Overall);

        // Top 10 per category
        result.put("MOVIE", getTop10ByMediaType("MOVIE"));
        result.put("GAME", getTop10ByMediaType("GAME"));
        result.put("SONG", getTop10ByMediaType("SONG"));
        return result;
    }

    /**
     * Returns the Fast-Rising response map for the Community Favourites page.
     *
     * Each category bucket is built from likes recorded in the last 7 days.
     * The ALL bucket is not fetched from a separate query. Instead, it is
     * created by combining the MOVIE, GAME, and SONG results, then sorting the
     * merged list by weekly likes and keeping the top 5.
     */
    public Map<String, List<RankedMediaResponse>> getFastRising5PerCategory() {
        Map<String, List<RankedMediaResponse>> result = new LinkedHashMap<>();
        LocalDateTime since = LocalDateTime.now().minusDays(7);

        // Build each category bucket first so the ALL bucket can be derived
        // from the same data without running a separate query.
        List<RankedMediaResponse> allCombined = new ArrayList<>();

        String[] categories = { "MOVIE", "GAME", "SONG" };
        for (String category : categories) {
            List<Object[]> rows = userInteractionRepository.findTop5TrendingLikesSinceAndMediaType(since, category);
            List<RankedMediaResponse> categoryTrending = enrichTrendingRows(rows);
            result.put(category, categoryTrending);
            allCombined.addAll(categoryTrending);
        }

        // Merge the category buckets, rank by weekly likes, and keep the top 5.
        List<RankedMediaResponse> allTrending = allCombined.stream()
                .sorted((a, b) -> Long.compare(b.getWeeklyLikes(), a.getWeeklyLikes()))
                .limit(5)
                .collect(Collectors.toList());

        result.put("ALL", allTrending);
        return result;
    }

    /**
     * Return a trimmed string value from a result row at the given index.
     *
     * @param row   the database result row
     * @param index the column index to read
     * @return the trimmed string value or empty string if missing
     */
    private String rowString(Object[] row, int index) {
        if (index >= row.length || row[index] == null) {
            return "";
        }
        return String.valueOf(row[index]).trim();
    }

    /**
     * Clears the per-request metadata cache.
     *
     * The shared metadata cache is intentionally kept so the next request can
     * reuse previously fetched titles, artists, and images.
     */
    public void clearRequestCache() {
        requestCache.get().clear();
        requestCache.remove();
        requestImageBackfills.get().clear();
        requestImageBackfills.remove();
    }

    /**
     * Fetches a JSON response body from the given URL and returns it as a
     * UTF-8 string. Handles gzip-compressed responses by inspecting the
     * Content-Encoding header and falling back to magic-bytes detection.
     *
     * @param url the URL to request
     * @return the response body as a String (empty string if no body)
     * @throws IOException if the request or decompression fails
     */
    private String fetchJsonBody(String url) throws IOException {
        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
        byte[] body = response.getBody();
        if (body == null || body.length == 0) {
            return "";
        }

        // Check Content-Encoding header to determine if response is gzip-compressed,
        // fall back to magic-byte detection (0x1f 0x8b) in case the header is missing
        String contentEncoding = response.getHeaders().getFirst("Content-Encoding");
        boolean isGzip = "gzip".equalsIgnoreCase(contentEncoding);

        if (!isGzip && body.length >= 2) {
            int b0 = body[0] & 0xFF;
            int b1 = body[1] & 0xFF;
            if (b0 == 0x1F && b1 == 0x8B) {
                isGzip = true;
            }
        }

        if (isGzip) {
            try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(body))) {
                body = gzipInputStream.readAllBytes();
            } catch (IOException e) {
                System.err.println("Failed to decompress metadata API gzip response: " + e.getMessage());
                throw e;
            }
        }

        return new String(body, StandardCharsets.UTF_8).trim();
    }

    /**
     * Lightweight metadata container.
     *
     * It stores only display metadata, not likes or views, so cached API
     * results can be safely reused with fresh ranking counts.
     *
     * TODO: move MediaMetadata into its own file if appropriate.
     */
    private static final class MediaMetadata {
        private final String mediaApiId;
        private final String title;
        private final String artist;
        private final String mediaType;
        private final String imageUrl;

        private MediaMetadata(String mediaApiId, String title, String artist, String mediaType, String imageUrl) {
            this.mediaApiId = mediaApiId;
            this.title = title;
            this.artist = artist;
            this.mediaType = mediaType;
            this.imageUrl = imageUrl;
        }
    }

    /**
     * Builds immutable metadata for a media item.
     */
    private MediaMetadata buildMetadata(String mediaApiId, String mediaType,
            String title, String artist, String imageUrl) {
        return new MediaMetadata(mediaApiId, title, artist, mediaType, imageUrl);
    }

    /**
     * Resolves metadata from the per-request cache first, then the shared
     * cache if the item has already been fetched in a previous request.
     */
    private MediaMetadata getCachedMetadata(String cacheKey) {
        Map<String, MediaMetadata> localCache = requestCache.get();
        MediaMetadata cached = localCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        cached = metadataCache.get(cacheKey);
        if (cached != null) {
            localCache.put(cacheKey, cached);
        }
        return cached;
    }

    /**
     * Stores metadata in both the per-request cache and the shared cache.
     */
    private void cacheMetadata(String cacheKey, MediaMetadata metadata) {
        requestCache.get().put(cacheKey, metadata);
        metadataCache.put(cacheKey, metadata);
    }

    /**
     * Persists a fetched image URL if the DB row does not already have one.
     */
    private void persistImageIfMissing(String mediaApiId, String mediaType, String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        String cacheKey = mediaType + "|" + mediaApiId;
        if (!requestImageBackfills.get().add(cacheKey)) {
            return;
        }

        userInteractionRepository.updateImageUrlForMediaIfMissing(mediaApiId, mediaType, imageUrl);
    }

    /**
     * Builds a Top 10 response with all-time score and counts.
     */
    private RankedMediaResponse buildScoreResponse(MediaMetadata metadata,
            long totalScore, long likes, long views) {
        return new RankedMediaResponse(metadata.mediaApiId, metadata.title, metadata.artist,
                metadata.mediaType, metadata.imageUrl, totalScore, likes, views);
    }

    /**
     * Builds a Fast-Rising response that only exposes weekly likes.
     */
    private RankedMediaResponse buildTrendingResponse(MediaMetadata metadata, long weeklyLikes) {
        return new RankedMediaResponse(metadata.mediaApiId, metadata.title, metadata.artist,
                metadata.mediaType, metadata.imageUrl, 0L, 0L, 0L, weeklyLikes);
    }

    /**
     * Builds a best-effort metadata record when the external API is unavailable.
     */
    private MediaMetadata fallbackMetadata(String mediaApiId, String mediaType,
            String storedTitle, String storedArtist, String storedImageUrl) {
        String title = storedTitle.isBlank() ? mediaApiId : storedTitle;
        return buildMetadata(mediaApiId, mediaType, title, storedArtist, storedImageUrl);
    }

    /**
     * Resolves metadata for a ranked row, backfilling missing artwork on demand
     * so the Community Favourites page can render images without adding cost to
     * the home-page recommendation flow.
     */
    private MediaMetadata resolveMetadata(String mediaApiId, String mediaType,
            String storedTitle, String storedArtist, String storedImageUrl) {
        String cacheKey = mediaType + "|" + mediaApiId;
        boolean missingTitle = storedTitle.isBlank();
        boolean missingImage = storedImageUrl.isBlank();

        MediaMetadata cached = getCachedMetadata(cacheKey);
        if (cached != null) {
            String title = missingTitle ? cached.title : storedTitle;
            String artist = storedArtist.isBlank() ? cached.artist : storedArtist;
            String imageUrl = missingImage ? cached.imageUrl : storedImageUrl;

            if (!title.isBlank()) {
                if (missingImage && !imageUrl.isBlank()) {
                    persistImageIfMissing(mediaApiId, mediaType, imageUrl);
                }

                MediaMetadata metadata = buildMetadata(mediaApiId, mediaType, title, artist, imageUrl);
                cacheMetadata(cacheKey, metadata);
                return metadata;
            }
        }

        if (missingTitle || missingImage) {
            MediaMetadata fetched = fetchMetadata(mediaApiId, mediaType);
            if (fetched != null) {
                String title = missingTitle ? fetched.title : storedTitle;
                String artist = storedArtist.isBlank() ? fetched.artist : storedArtist;
                String imageUrl = missingImage ? fetched.imageUrl : storedImageUrl;

                if (missingImage && !imageUrl.isBlank()) {
                    persistImageIfMissing(mediaApiId, mediaType, imageUrl);
                }

                MediaMetadata metadata = buildMetadata(mediaApiId, mediaType, title, artist, imageUrl);
                cacheMetadata(cacheKey, metadata);
                return metadata;
            }
        }

        if (!storedTitle.isBlank()) {
            MediaMetadata metadata = buildMetadata(mediaApiId, mediaType, storedTitle, storedArtist, storedImageUrl);
            cacheMetadata(cacheKey, metadata);
            return metadata;
        }

        MediaMetadata fallback = fallbackMetadata(mediaApiId, mediaType, storedTitle, storedArtist, storedImageUrl);
        cacheMetadata(cacheKey, fallback);
        return fallback;
    }

    /**
     * Enriches trending rows.
     *
     * Row format:
     * [0] mediaApiId (String)
     * [1] mediaType (String)
     * [2] weeklyLikes (Long)
     * [3] storedTitle (String)
     * [4] storedArtist (String)
     * [5] storedImageUrl (String)
     *
     * @param rows list of raw DB rows matching the shape described above
     * @return a list of {@link RankedMediaResponse} with weeklyLikes populated
     */
    private List<RankedMediaResponse> enrichTrendingRows(List<Object[]> rows) {
        List<RankedMediaResponse> result = new ArrayList<>();

        for (Object[] row : rows) {
            String mediaApiId = String.valueOf(row[0]);
            String mediaType = normalizeMediaType(String.valueOf(row[1]));
            long weeklyLikes = ((Number) row[2]).longValue();
            String storedTitle = rowString(row, 3);
            String storedArtist = rowString(row, 4);
            String storedImageUrl = rowString(row, 5);

            if (weeklyLikes <= 0) {
                continue;
            }

            MediaMetadata metadata = resolveMetadata(mediaApiId, mediaType, storedTitle, storedArtist, storedImageUrl);
            result.add(buildTrendingResponse(metadata, weeklyLikes));
        }
        return result;
    }

    /**
     * Converts aggregated score rows into DTOs.
     *
     * Row format:
     * [0] mediaApiId (String)
     * [1] mediaType (String)
     * [2] totalScore (Long)
     * [3] likes (Long)
     * [4] views (Long)
     * [5] storedTitle (String)
     * [6] storedArtist (String)
     * [7] storedImageUrl (String)
     *
     * @param rows list of raw DB rows matching the shape described above
     * @return a list of {@link RankedMediaResponse} enriched with metadata
     *         and all-time counts
     */
    private List<RankedMediaResponse> enrichScoreRows(List<Object[]> rows) {
        List<RankedMediaResponse> result = new ArrayList<>();

        for (Object[] row : rows) {
            String mediaApiId = String.valueOf(row[0]);
            String mediaType = normalizeMediaType(String.valueOf(row[1]));
            long totalScore = ((Number) row[2]).longValue();
            long likes = ((Number) row[3]).longValue();
            long views = ((Number) row[4]).longValue();
            String storedTitle = rowString(row, 5);
            String storedArtist = rowString(row, 6);
            String storedImageUrl = rowString(row, 7);
            MediaMetadata metadata = resolveMetadata(mediaApiId, mediaType, storedTitle, storedArtist, storedImageUrl);
            result.add(buildScoreResponse(metadata, totalScore, likes, views));
        }

        return result;
    }

    /**
     * Dispatch helper that selects the appropriate external metadata
     * fetcher based on the media type.
     *
     * @param mediaApiId the external API id or identifier
     * @param mediaType  normalized media type (MOVIE/GAME/SONG)
     * @return a {@link MediaMetadata} with metadata or null if not found
     */
    private MediaMetadata fetchMetadata(String mediaApiId, String mediaType) {
        return switch (mediaType) {
            case "MOVIE" -> fetchTmdbMetadata(mediaApiId);
            case "GAME" -> fetchRawgMetadata(mediaApiId);
            case "SONG" -> fetchLastfmMetadata(mediaApiId);
            default -> null;
        };
    }

    /**
     * Fetches movie metadata from TMDB.
     *
     * @param mediaApiId external TMDB movie id
     * @return a {@link MediaMetadata} with movie metadata or null on error
     */
    private MediaMetadata fetchTmdbMetadata(String mediaApiId) {
        try {
            String url = "https://api.themoviedb.org/3/movie/" + mediaApiId + "?api_key=" + tmdbApiKey;
            String response = fetchJsonBody(url);
            JsonNode movie = objectMapper.readTree(response);

            String title = movie.path("title").asText("");
            if (title.isBlank()) {
                return null;
            }
            String posterPath = movie.path("poster_path").asText("");
            String imageUrl = posterPath.isBlank() ? "" : "https://image.tmdb.org/t/p/w500" + posterPath;

            return buildMetadata(mediaApiId, "MOVIE", title, "", imageUrl);
        } catch (Exception e) {
            System.err.println("TMDB metadata error for " + mediaApiId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Fetches game metadata from RAWG.
     *
     * @param mediaApiId external RAWG game id
     * @return a {@link MediaMetadata} with game metadata or null on error
     */
    private MediaMetadata fetchRawgMetadata(String mediaApiId) {
        try {
            String url = "https://api.rawg.io/api/games/" + mediaApiId + "?key=" + rawgApiKey;
            String response = fetchJsonBody(url);
            JsonNode game = objectMapper.readTree(response);

            String title = game.path("name").asText("");
            if (title.isBlank()) {
                return null;
            }
            String imageUrl = game.path("background_image").asText("");

            return buildMetadata(mediaApiId, "GAME", title, "", imageUrl);
        } catch (Exception e) {
            System.err.println("RAWG metadata error for " + mediaApiId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Fetches track metadata from Last.fm.
     *
     * Expect {@code mediaApiId} to be a Last.fm track URL of the form:
     * {@code https://www.last.fm/music/Artist/_/Track}.
     *
     *
     * @param mediaApiId Last.fm track URL
     * @return a {@link MediaMetadata} with track metadata or null on error
     */
    private MediaMetadata fetchLastfmMetadata(String mediaApiId) {
        try {
            String path = mediaApiId.replaceFirst("^https://www\\.last\\.fm/music/", "");
            String[] parts = path.split("/_/", 2);
            if (parts.length < 2)
                return null;

            String artist = URLDecoder.decode(parts[0], "UTF-8").replace("+", " ").trim();
            String track = URLDecoder.decode(parts[1], "UTF-8").replace("+", " ").trim();
            if (track.isBlank()) {
                return null;
            }

            String url = "https://ws.audioscrobbler.com/2.0/?method=track.getInfo"
                    + "&artist=" + URLEncoder.encode(artist, "UTF-8")
                    + "&track=" + URLEncoder.encode(track, "UTF-8")
                    + "&autocorrect=1"
                    + "&api_key=" + lastfmApiKey
                    + "&format=json";

            String response = fetchJsonBody(url);
            JsonNode trackNode = objectMapper.readTree(response).path("track");

            if (trackNode.isMissingNode() || trackNode.isEmpty()) {
                return buildMetadata(mediaApiId, "SONG", track, artist, "");
            }

            String title = trackNode.path("name").asText(track);
            JsonNode artistNode = trackNode.path("artist");
            String artistName = artistNode.isTextual()
                    ? artistNode.asText(artist)
                    : artistNode.path("name").asText(artist);

            String imageUrl = trackNode.path("album").path("image").path(3).path("#text").asText("");
            if (imageUrl.isEmpty())
                imageUrl = trackNode.path("album").path("image").path(2).path("#text").asText("");

            return buildMetadata(mediaApiId, "SONG", title, artistName, imageUrl);
        } catch (Exception e) {
            System.err.println("Last.fm metadata error for " + mediaApiId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Normalizes category/mediaType values to one of: MOVIE / GAME / SONG
     */
    public String normalizeMediaType(String value) {
        if (value == null)
            return "";
        String upper = value.trim().toUpperCase();
        return switch (upper) {
            case "MOVIE", "MOVIES" -> "MOVIE";
            case "GAME", "GAMES" -> "GAME";
            case "SONG", "SONGS", "MUSIC" -> "SONG";
            default -> upper;
        };
    }
}
