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
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.stream.Collectors;

/**
 * Service responsible for building the Community Favourites page data.
 *
 * Popularity Score = views * 1 + likes * 10.
 * Likes and views are exposed separately for display on the UI.
 */
@Service
public class MediaRankingService {

    private final UserInteractionRepository userInteractionRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ThreadLocal<Map<String, RankedMediaResponse>> requestCache = ThreadLocal
            .withInitial(ConcurrentHashMap::new);

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
     * Returns Top 10 ranked media across all categories.
     */
    public List<RankedMediaResponse> getTop10() {
        List<Object[]> rows = userInteractionRepository.findTop10ByScoreWithCounts();
        return enrichScoreRows(rows);
    }

    /**
     * Returns Top 10 ranked media for a specific category.
     */
    public List<RankedMediaResponse> getTop10ByMediaType(String mediaType) {
        List<Object[]> rows = userInteractionRepository
                .findTop10ByScoreWithCountsAndMediaType(normalizeMediaType(mediaType));
        return enrichScoreRows(rows);
    }

    /**
     * Returns Top 10 for all three categories PLUS top 10 overall.
     * Used by Community Favourites page:
     * - "ALL" tab: top10 overall sorted by popularity score
     * - Category tabs: top10 per category
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
     * Returns Top 5 Fast-Rising items: most LIKE interactions in the past 7 days.
     *
     * Row format from findTop5TrendingLikesSince:
     * [0] mediaApiId (String)
     * [1] mediaType (String)
     * [2] weeklyLikes (Long)
     */
    public List<RankedMediaResponse> getFastRising5() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Object[]> rows = userInteractionRepository.findTop5TrendingLikesSince(since);

        List<RankedMediaResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            String mediaApiId = String.valueOf(row[0]);
            String mediaType = normalizeMediaType(String.valueOf(row[1]));
            long weeklyLikes = ((Number) row[2]).longValue();

            RankedMediaResponse enriched = fetchMetadata(mediaApiId, mediaType, weeklyLikes, 0L, weeklyLikes);
            if (enriched != null) {
                result.add(new RankedMediaResponse(
                        enriched.getMediaApiId(), enriched.getTitle(), enriched.getArtist(),
                        enriched.getMediaType(), enriched.getImageUrl(),
                        enriched.getTotalScore(), enriched.getLikes(), enriched.getViews(),
                        weeklyLikes));
            }
        }
        return result;
    }

    /**
     * Returns Top 5 Fast-Rising items per category PLUS top 5 overall.
     * Used by Community Favourites page:
     * - "ALL" tab: top5 trending overall sorted by trending score
     * - Category tabs: top5 trending per category
     */
    public Map<String, List<RankedMediaResponse>> getFastRising5PerCategory() {
        Map<String, List<RankedMediaResponse>> result = new LinkedHashMap<>();
        LocalDateTime since = LocalDateTime.now().minusDays(7);

        // Collect category-specific results
        List<RankedMediaResponse> allCombined = new ArrayList<>();

        String[] categories = { "MOVIE", "GAME", "SONG" };
        for (String category : categories) {
            List<Object[]> rows = userInteractionRepository.findTop5TrendingLikesSinceAndMediaType(since, category);
            List<RankedMediaResponse> categoryTrending = enrichTrendingRows(rows);
            result.put(category, categoryTrending);
            allCombined.addAll(categoryTrending);
        }

        // Compute "ALL" from combined categories: sort by trending score DESC and take
        // top 5
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
     * Clears the per-request thread-local cache used to deduplicate external
     * metadata requests. Call this at the end of a request to avoid leaking
     * cached entries between calls.
     */
    public void clearRequestCache() {
        requestCache.get().clear();
        requestCache.remove();
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
                System.err.println("Failed to decompress gzip response from " + url + ": " + e.getMessage());
                throw e;
            }
        }

        return new String(body, StandardCharsets.UTF_8).trim();
    }

    /**
     * Build a stored/seed {@link RankedMediaResponse} used when the DB already
     * contains title/artist/image information. This keeps a single creation
     * point for responses constructed from stored metadata.
     *
     * @param mediaApiId external id
     * @param mediaType  normalized media type
     * @param totalScore popularity score
     * @param likes      likes count
     * @param views      views count
     * @param title      stored title
     * @param artist     stored artist
     * @param imageUrl   stored image URL
     * @return a populated {@link RankedMediaResponse}
     */
    private RankedMediaResponse buildStoredMetadataResponse(String mediaApiId, String mediaType,
            long totalScore, long likes, long views, String title, String artist, String imageUrl) {
        return new RankedMediaResponse(mediaApiId, title, artist, mediaType, imageUrl, totalScore, likes, views);
    }

    /**
     * When a cached response is available for a trending row we need to
     * convert it into a trending-specific DTO that uses the provided
     * weekly likes as the 'likes' and 'weeklyLikes' fields while preserving
     * the cached title/image information.
     *
     * @param cached      cached metadata response
     * @param weeklyLikes likes in the week (trending metric)
     * @return a trending-oriented {@link RankedMediaResponse}
     */
    private RankedMediaResponse buildCachedTrendingResponse(RankedMediaResponse cached, long weeklyLikes) {
        return new RankedMediaResponse(
                cached.getMediaApiId(), cached.getTitle(), cached.getArtist(), cached.getMediaType(),
                cached.getImageUrl(), cached.getTotalScore(), weeklyLikes, cached.getViews(), weeklyLikes);
    }

    /**
     * Helper used when stored metadata exists for a trending row. Uses the
     * weekly likes as the total/likes/views fields to match prior behaviour
     * for trending conversions.
     *
     * @param mediaApiId  the external id
     * @param mediaType   normalized media type
     * @param weeklyLikes likes in the week (trending metric)
     * @param title       stored title
     * @param artist      stored artist
     * @param imageUrl    stored image URL
     * @return a trending-oriented {@link RankedMediaResponse}
     */
    private RankedMediaResponse buildStoredTrendingResponse(String mediaApiId, String mediaType,
            long weeklyLikes, String title, String artist, String imageUrl) {
        return buildStoredMetadataResponse(mediaApiId, mediaType, weeklyLikes, 0L, weeklyLikes, title, artist,
                imageUrl);
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
     * @return a list of {@link RankedMediaResponse} with metadata populated
     */
    private List<RankedMediaResponse> enrichTrendingRows(List<Object[]> rows) {
        List<RankedMediaResponse> result = new ArrayList<>();
        Map<String, RankedMediaResponse> cache = requestCache.get();

        for (Object[] row : rows) {
            String mediaApiId = String.valueOf(row[0]);
            String mediaType = normalizeMediaType(String.valueOf(row[1]));
            long weeklyLikes = ((Number) row[2]).longValue();
            String storedTitle = rowString(row, 3);
            String storedArtist = rowString(row, 4);
            String storedImageUrl = rowString(row, 5);
            String cacheKey = mediaType + "|" + mediaApiId;

            RankedMediaResponse cached = cache.get(cacheKey);
            if (cached != null) {
                result.add(buildCachedTrendingResponse(cached, weeklyLikes));
                continue;
            }

            if (!storedTitle.isBlank()) {
                RankedMediaResponse response = buildStoredTrendingResponse(
                        mediaApiId, mediaType, weeklyLikes, storedTitle, storedArtist, storedImageUrl);
                result.add(response);
                cache.put(cacheKey, response);
                continue;
            }

            RankedMediaResponse enriched = fetchMetadata(mediaApiId, mediaType, weeklyLikes, 0L, weeklyLikes);
            if (enriched != null) {
                RankedMediaResponse response = buildCachedTrendingResponse(enriched, weeklyLikes);
                result.add(response);
                cache.put(cacheKey, response);
            }
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
     */
    private List<RankedMediaResponse> enrichScoreRows(List<Object[]> rows) {
        List<RankedMediaResponse> result = new ArrayList<>();
        Map<String, RankedMediaResponse> cache = requestCache.get();

        for (Object[] row : rows) {
            String mediaApiId = String.valueOf(row[0]);
            String mediaType = normalizeMediaType(String.valueOf(row[1]));
            long totalScore = ((Number) row[2]).longValue();
            long likes = ((Number) row[3]).longValue();
            long views = ((Number) row[4]).longValue();
            String storedTitle = rowString(row, 5);
            String storedArtist = rowString(row, 6);
            String storedImageUrl = rowString(row, 7);
            String cacheKey = mediaType + "|" + mediaApiId;

            RankedMediaResponse cached = cache.get(cacheKey);
            if (cached != null) {
                result.add(cached);
                continue;
            }

            if (!storedTitle.isBlank()) {
                RankedMediaResponse response = buildStoredMetadataResponse(
                        mediaApiId, mediaType, totalScore, likes, views,
                        storedTitle, storedArtist, storedImageUrl);
                result.add(response);
                cache.put(cacheKey, response);
                continue;
            }

            RankedMediaResponse enriched = fetchMetadata(mediaApiId, mediaType, totalScore, likes, views);
            if (enriched != null) {
                result.add(enriched);
                cache.put(cacheKey, enriched);
            }
        }

        return result;
    }

    /**
     * Dispatch helper that selects the appropriate external metadata
     * fetcher based on the media type.
     *
     * @param mediaApiId the external API id or identifier
     * @param mediaType  normalized media type (MOVIE/GAME/SONG)
     * @param totalScore aggregated popularity score from interactions
     * @param likes      aggregated likes count
     * @param views      aggregated views count
     * @return a {@link RankedMediaResponse} with metadata or null if not found
     */
    private RankedMediaResponse fetchMetadata(String mediaApiId, String mediaType,
            long totalScore, long likes, long views) {
        return switch (mediaType) {
            case "MOVIE" -> fetchTmdbMetadata(mediaApiId, totalScore, likes, views);
            case "GAME" -> fetchRawgMetadata(mediaApiId, totalScore, likes, views);
            case "SONG" -> fetchLastfmMetadata(mediaApiId, totalScore, likes, views);
            default -> null;
        };
    }

    /**
     * Fetches movie metadata from TMDB.
     *
     * @param mediaApiId external TMDB movie id
     * @param totalScore aggregated popularity score
     * @param likes      aggregated likes count
     * @param views      aggregated views count
     * @return a {@link RankedMediaResponse} with movie metadata or null on error
     */
    private RankedMediaResponse fetchTmdbMetadata(String mediaApiId, long totalScore, long likes, long views) {
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

            return new RankedMediaResponse(mediaApiId, title, "", "MOVIE", imageUrl, totalScore, likes, views);
        } catch (Exception e) {
            System.err.println("TMDB metadata error for " + mediaApiId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Fetches game metadata from RAWG.
     *
     * @param mediaApiId external RAWG game id
     * @param totalScore aggregated popularity score
     * @param likes      aggregated likes count
     * @param views      aggregated views count
     * @return a {@link RankedMediaResponse} with game metadata or null on error
     */
    private RankedMediaResponse fetchRawgMetadata(String mediaApiId, long totalScore, long likes, long views) {
        try {
            String url = "https://api.rawg.io/api/games/" + mediaApiId + "?key=" + rawgApiKey;
            String response = fetchJsonBody(url);
            JsonNode game = objectMapper.readTree(response);

            String title = game.path("name").asText("");
            if (title.isBlank()) {
                return null;
            }
            String imageUrl = game.path("background_image").asText("");

            return new RankedMediaResponse(mediaApiId, title, "", "GAME", imageUrl, totalScore, likes, views);
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
     * @param totalScore aggregated popularity score
     * @param likes      aggregated likes count
     * @param views      aggregated views count
     * @return a {@link RankedMediaResponse} with track metadata or null on error
     */
    private RankedMediaResponse fetchLastfmMetadata(String mediaApiId, long totalScore, long likes, long views) {
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
                return new RankedMediaResponse(mediaApiId, track, artist, "SONG", "", totalScore, likes, views);
            }

            String title = trackNode.path("name").asText(track);
            JsonNode artistNode = trackNode.path("artist");
            String artistName = artistNode.isTextual()
                    ? artistNode.asText(artist)
                    : artistNode.path("name").asText(artist);

            String imageUrl = trackNode.path("album").path("image").path(3).path("#text").asText("");
            if (imageUrl.isEmpty())
                imageUrl = trackNode.path("album").path("image").path(2).path("#text").asText("");

            return new RankedMediaResponse(mediaApiId, title, artistName, "SONG", imageUrl, totalScore, likes, views);
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
