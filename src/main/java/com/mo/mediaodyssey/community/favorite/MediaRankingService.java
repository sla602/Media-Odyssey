package com.mo.mediaodyssey.community.favorite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mo.mediaodyssey.recommendation.UserInteractionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for building the Community Favourites page data.
 *
 * Current iteration scope:
 * 1. Top 10 by internal score
 * 2. Metadata enrichment from external APIs
 * 3. UI-only star rating derived from score
 *
 * Deferred to later iteration:
 * - Fast-Rising section
 * - user-submitted star ratings
 * - richer normalization / weighting between categories
 */
@Service
public class MediaRankingService {

    private final UserInteractionRepository userInteractionRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    @Value("${rawg.api.key}")
    private String rawgApiKey;

    @Value("${lastfm.api.key}")
    private String lastfmApiKey;

    public MediaRankingService(UserInteractionRepository userInteractionRepository) {
        this.userInteractionRepository = userInteractionRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Returns Top 10 ranked media across all categories.
     */
    public List<RankedMediaResponse> getTop10() {
        List<Object[]> rows = userInteractionRepository.findTop10ByScore();
        return enrichScoreRows(rows);
    }

    /**
     * Returns Top 10 ranked media for a specific category.
     */
    public List<RankedMediaResponse> getTop10ByMediaType(String mediaType) {
        List<Object[]> rows = userInteractionRepository.findTop10ByScoreAndMediaType(normalizeMediaType(mediaType));
        return enrichScoreRows(rows);
    }

    /**
     * Converts aggregated score rows into DTOs enriched with metadata.
     *
     * Row format:
     * [0] mediaApiId
     * [1] mediaType
     * [2] totalScore
     */
    private List<RankedMediaResponse> enrichScoreRows(List<Object[]> rows) {
        List<RankedMediaResponse> result = new ArrayList<>();

        for (Object[] row : rows) {
            String mediaApiId = String.valueOf(row[0]);
            String mediaType = normalizeMediaType(String.valueOf(row[1]));
            long totalScore = ((Number) row[2]).longValue();

            RankedMediaResponse enriched = fetchMetadata(mediaApiId, mediaType, totalScore);
            if (enriched != null) {
                result.add(enriched);
            } else {
                result.add(new RankedMediaResponse(
                        mediaApiId,
                        "Unknown",
                        "",
                        mediaType,
                        "",
                        totalScore,
                        toDisplayRating(totalScore)));
            }
        }

        return result;
    }

    /**
     * Maps internal score to a 1.0–5.0 display rating.
     *
     * This rating is only for UI readability.
     * Internal sorting still uses totalScore.
     *
     * TODO (later iteration):
     * Revisit this mapping after enough real interaction data is collected.
     * We may want a smoother formula or category-normalized scaling.
     */
    private double toDisplayRating(long totalScore) {
        if (totalScore <= 0)
            return 1.0;
        if (totalScore <= 10)
            return 1.5;
        if (totalScore <= 20)
            return 2.0;
        if (totalScore <= 35)
            return 2.5;
        if (totalScore <= 50)
            return 3.0;
        if (totalScore <= 70)
            return 3.5;
        if (totalScore <= 95)
            return 4.0;
        if (totalScore <= 130)
            return 4.5;
        return 5.0;
    }

    private RankedMediaResponse fetchMetadata(String mediaApiId, String mediaType, long totalScore) {
        return switch (mediaType) {
            case "MOVIE" -> fetchTmdbMetadata(mediaApiId, totalScore);
            case "GAME"  -> fetchRawgMetadata(mediaApiId, totalScore);
            case "SONG"  -> fetchLastfmMetadata(mediaApiId, totalScore);
            default      -> null;
        };
    }

    /**
     * Fetches movie metadata from TMDB.
     */
    private RankedMediaResponse fetchTmdbMetadata(String mediaApiId, long totalScore) {
        try {
            String url = "https://api.themoviedb.org/3/movie/" + mediaApiId + "?api_key=" + tmdbApiKey;
            String response = restTemplate.getForObject(url, String.class);
            JsonNode movie = objectMapper.readTree(response);

            String title = movie.path("title").asText("Unknown");
            String posterPath = movie.path("poster_path").asText("");
            String imageUrl = posterPath.isBlank() ? "" : "https://image.tmdb.org/t/p/w500" + posterPath;

            return new RankedMediaResponse(
                    mediaApiId,
                    title,
                    "",
                    "MOVIE",
                    imageUrl,
                    totalScore,
                    toDisplayRating(totalScore));
        } catch (Exception e) {
            System.err.println("TMDB metadata error for " + mediaApiId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Fetches game metadata from RAWG.
     */
    private RankedMediaResponse fetchRawgMetadata(String mediaApiId, long totalScore) {
        try {
            String url = "https://api.rawg.io/api/games/" + mediaApiId + "?key=" + rawgApiKey;
            String response = restTemplate.getForObject(url, String.class);
            JsonNode game = objectMapper.readTree(response);

            String title = game.path("name").asText("Unknown");
            String imageUrl = game.path("background_image").asText("");

            return new RankedMediaResponse(
                    mediaApiId,
                    title,
                    "",
                    "GAME",
                    imageUrl,
                    totalScore,
                    toDisplayRating(totalScore));
        } catch (Exception e) {
            System.err.println("RAWG metadata error for " + mediaApiId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Fetches track metadata from Last.fm.
     * mediaApiId is a Last.fm track URL e.g. https://www.last.fm/music/Artist/_/Track
     * Parses the URL to extract artist and track name, then calls track.getInfo.
     */
    private RankedMediaResponse fetchLastfmMetadata(String mediaApiId, long totalScore) {
        try {
            // Last.fm track URLs follow the pattern: https://www.last.fm/music/{artist}/_/{track}
            String path = mediaApiId.replaceFirst("https://www\\.last\\.fm/music/", "");
            String[] parts = path.split("/_/", 2);
            if (parts.length < 2) return null;

            String artist = URLDecoder.decode(parts[0].replace("+", " "), "UTF-8");
            String track  = URLDecoder.decode(parts[1].replace("+", " "), "UTF-8");

            String url = "https://ws.audioscrobbler.com/2.0/?method=track.getInfo"
                    + "&artist=" + java.net.URLEncoder.encode(artist, "UTF-8")
                    + "&track="  + java.net.URLEncoder.encode(track, "UTF-8")
                    + "&api_key=" + lastfmApiKey
                    + "&format=json";

            String response = restTemplate.getForObject(url, String.class);
            JsonNode trackNode = objectMapper.readTree(response).path("track");

            String title    = trackNode.path("name").asText("Unknown");
            String artistName = trackNode.path("artist").path("name").asText("");
            // Last.fm image array: index 3 is extralarge, fall back to index 2 (large)
            String imageUrl = trackNode.path("album").path("image").path(3).path("#text").asText("");
            if (imageUrl.isEmpty()) imageUrl = trackNode.path("album").path("image").path(2).path("#text").asText("");

            return new RankedMediaResponse(
                    mediaApiId,
                    title,
                    artistName,
                    "SONG",
                    imageUrl,
                    totalScore,
                    toDisplayRating(totalScore));
        } catch (Exception e) {
            System.err.println("Last.fm metadata error for " + mediaApiId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Normalizes category/mediaType values to one of:
     * MOVIE / GAME / SONG
     */
    public String normalizeMediaType(String value) {
        if (value == null)
            return "";
        String upper = value.trim().toUpperCase();

        return switch (upper) {
            case "MOVIE", "MOVIES" -> "MOVIE";
            case "GAME", "GAMES"   -> "GAME";
            case "SONG", "SONGS", "MUSIC" -> "SONG";
            default -> upper;
        };
    }
}