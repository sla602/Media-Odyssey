package com.mo.mediaodyssey.community.favorite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mo.mediaodyssey.recommendation.UserInteractionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.concurrent.ConcurrentHashMap;
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

    public MediaRankingService(UserInteractionRepository userInteractionRepository, RestTemplate restTemplate) {
        this.userInteractionRepository = userInteractionRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void clearRequestCache() {
        requestCache.get().clear();
        requestCache.remove();
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
    public java.util.Map<String, List<RankedMediaResponse>> getTop10PerCategory() {
        java.util.Map<String, List<RankedMediaResponse>> result = new java.util.LinkedHashMap<>();

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
     * [2] likeCount (Long)
     */
    public List<RankedMediaResponse> getFastRising5() {
        java.time.LocalDateTime since = java.time.LocalDateTime.now().minusDays(7);
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
    public java.util.Map<String, List<RankedMediaResponse>> getFastRising5PerCategory() {
        java.util.Map<String, List<RankedMediaResponse>> result = new java.util.LinkedHashMap<>();
        java.time.LocalDateTime since = java.time.LocalDateTime.now().minusDays(7);

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

    private String rowString(Object[] row, int index) {
        if (index >= row.length || row[index] == null) {
            return "";
        }
        return String.valueOf(row[index]).trim();
    }

    private RankedMediaResponse buildStoredMetadataResponse(String mediaApiId, String mediaType,
            long totalScore, long likes, long views, String title, String artist, String imageUrl) {
        return new RankedMediaResponse(mediaApiId, title, artist, mediaType, imageUrl, totalScore, likes, views);
    }

    private String fetchJsonBody(String url) throws IOException {
        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
        byte[] body = response.getBody();
        if (body == null || body.length == 0) {
            return "";
        }

        if (body.length >= 2 && body[0] == (byte) 0x1f && body[1] == (byte) 0x8b) {
            try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(body))) {
                body = gzipInputStream.readAllBytes();
            }
        }

        return new String(body, StandardCharsets.UTF_8).trim();
    }

    /**
     * Enriches trending rows.
     * Row format: [0] mediaApiId, [1] mediaType, [2] likeCount, [3] title, [4]
     * artist, [5] imageUrl
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
                result.add(new RankedMediaResponse(
                        cached.getMediaApiId(), cached.getTitle(), cached.getArtist(),
                        cached.getMediaType(), cached.getImageUrl(),
                        cached.getTotalScore(), weeklyLikes, cached.getViews(), weeklyLikes));
                continue;
            }

            if (!storedTitle.isBlank()) {
                RankedMediaResponse response = buildStoredMetadataResponse(
                        mediaApiId, mediaType, weeklyLikes, 0L, weeklyLikes,
                        storedTitle, storedArtist, storedImageUrl);
                result.add(response);
                cache.put(cacheKey, response);
                continue;
            }

            RankedMediaResponse enriched = fetchMetadata(mediaApiId, mediaType, weeklyLikes, 0L, weeklyLikes);
            if (enriched != null) {
                RankedMediaResponse response = new RankedMediaResponse(
                        enriched.getMediaApiId(), enriched.getTitle(), enriched.getArtist(),
                        enriched.getMediaType(), enriched.getImageUrl(),
                        enriched.getTotalScore(), weeklyLikes, enriched.getViews(), weeklyLikes);
                result.add(response);
                cache.put(cacheKey, response);
            }
        }
        return result;
    }

    /**
     * Converts aggregated score rows into DTOs.
     * Row format: [0] mediaApiId, [1] mediaType, [2] totalScore, [3] likeCount,
     * [4] viewCount, [5] title, [6] artist, [7] imageUrl
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
     * mediaApiId is a Last.fm track URL e.g.
     * https://www.last.fm/music/Artist/_/Track
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
                    + "&artist=" + java.net.URLEncoder.encode(artist, "UTF-8")
                    + "&track=" + java.net.URLEncoder.encode(track, "UTF-8")
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
