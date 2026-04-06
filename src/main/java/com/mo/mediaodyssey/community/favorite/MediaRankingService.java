package com.mo.mediaodyssey.community.favorite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mo.mediaodyssey.recommendation.UserInteractionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

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
            } else {
                result.add(new RankedMediaResponse(
                        mediaApiId, "Unknown", "", mediaType, "",
                        0L, weeklyLikes, 0L, weeklyLikes));
            }
        }
        return result;
    }

    /**
     * Converts aggregated score rows into DTOs enriched with metadata.
     *
     * Row format from findTop10ByScoreWithCounts:
     * [0] mediaApiId (String)
     * [1] mediaType (String)
     * [2] totalScore (Long)
     * [3] likeCount (Long)
     * [4] viewCount (Long)
     */
    private List<RankedMediaResponse> enrichScoreRows(List<Object[]> rows) {
        List<RankedMediaResponse> result = new ArrayList<>();

        for (Object[] row : rows) {
            String mediaApiId = String.valueOf(row[0]);
            String mediaType = normalizeMediaType(String.valueOf(row[1]));
            long totalScore = ((Number) row[2]).longValue();
            long likes = ((Number) row[3]).longValue();
            long views = ((Number) row[4]).longValue();

            RankedMediaResponse enriched = fetchMetadata(mediaApiId, mediaType, totalScore, likes, views);
            if (enriched != null) {
                result.add(enriched);
            } else {
                result.add(new RankedMediaResponse(
                        mediaApiId, "Unknown", "", mediaType, "",
                        totalScore, likes, views));
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
            String response = restTemplate.getForObject(url, String.class);
            JsonNode movie = objectMapper.readTree(response);

            String title = movie.path("title").asText("Unknown");
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
            String response = restTemplate.getForObject(url, String.class);
            JsonNode game = objectMapper.readTree(response);

            String title = game.path("name").asText("Unknown");
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

            String url = "https://ws.audioscrobbler.com/2.0/?method=track.getInfo"
                    + "&artist=" + java.net.URLEncoder.encode(artist, "UTF-8")
                    + "&track=" + java.net.URLEncoder.encode(track, "UTF-8")
                    + "&autocorrect=1"
                    + "&api_key=" + lastfmApiKey
                    + "&format=json";

            String response = restTemplate.getForObject(url, String.class);
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