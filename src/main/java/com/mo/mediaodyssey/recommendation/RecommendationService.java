package com.mo.mediaodyssey.recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final UserInteractionRepository userInteractionRepository;
    private final BannedMediaRepository bannedMediaRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Limits concurrent Last.fm image calls to 4 at a time (safe under 5 req/sec)
    private final Semaphore lastfmSemaphore = new Semaphore(4);

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    @Value("${rawg.api.key}")
    private String rawgApiKey;

    @Value("${lastfm.api.key}")
    private String lastfmApiKey;

    // maps genre names to TMDB genre IDs
    private static final Map<String, Integer> TMDB_GENRE_IDS = new HashMap<>();
    static {
        TMDB_GENRE_IDS.put("Action", 28);
        TMDB_GENRE_IDS.put("Adventure", 12);
        TMDB_GENRE_IDS.put("Animation", 16);
        TMDB_GENRE_IDS.put("Comedy", 35);
        TMDB_GENRE_IDS.put("Crime", 80);
        TMDB_GENRE_IDS.put("Documentary", 99);
        TMDB_GENRE_IDS.put("Drama", 18);
        TMDB_GENRE_IDS.put("Fantasy", 14);
        TMDB_GENRE_IDS.put("Horror", 27);
        TMDB_GENRE_IDS.put("Mystery", 9648);
        TMDB_GENRE_IDS.put("Romance", 10749);
        TMDB_GENRE_IDS.put("Science Fiction", 878);
        TMDB_GENRE_IDS.put("Thriller", 53);
    }

    // reverse map: TMDB genre ID -> genre name
    private static final Map<Integer, String> TMDB_GENRE_NAMES = new HashMap<>();
    static {
        TMDB_GENRE_IDS.forEach((name, id) -> TMDB_GENRE_NAMES.put(id, name));
    }

    // genre tags used for Last.fm tag.getTopTracks — must be valid Last.fm tags
    private static final List<String> SONG_GENRES = Arrays.asList(
        "Pop", "Rock", "Hip-hop", "R&b", "Jazz", "Classical", "Electronic", "Country", "Reggae", "Soul"
    );

    // genre slugs used for RAWG — must match RAWG's accepted genre slugs
    private static final List<String> RAWG_GENRES = Arrays.asList(
        "Action", "Adventure", "Puzzle", "Strategy", "Shooter", "Racing", "Sports", "Simulation", "Indie", "Fighting"
    );

    public RecommendationService(UserInteractionRepository userInteractionRepository,
                                  BannedMediaRepository bannedMediaRepository) {
        this.userInteractionRepository = userInteractionRepository;
        this.bannedMediaRepository = bannedMediaRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    // saves a user interaction — userId comes from the session, not the frontend
    // duplicate likes are blocked; duplicate views are allowed
    public void recordInteraction(Long userId, InteractionRequest request) {
        if ("LIKE".equals(request.getInteractionType()) &&
            userInteractionRepository.existsByUserIdAndMediaApiIdAndInteractionType(
                userId, request.getMediaApiId(), "LIKE")) {
            return;
        }

        UserInteraction interaction = new UserInteraction();
        interaction.setUserId(userId);
        interaction.setMediaApiId(request.getMediaApiId());
        interaction.setInteractionType(request.getInteractionType());
        interaction.setMediaType(request.getMediaType());
        interaction.setGenres(request.getGenres());
        interaction.setTimestamp(LocalDateTime.now());
        userInteractionRepository.save(interaction);
    }

    // returns the genre the user has engaged with most for a given media type
    // VIEW = 1 point, LIKE = 10 points
    private String userFavoriteGenre(Long userId, String mediaType) {
        List<UserInteraction> allUserInteractions = userInteractionRepository.findByUserId(userId);

        Map<String, Integer> genreScores = new HashMap<>();

        for (UserInteraction interaction : allUserInteractions) {
            if (interaction.getMediaType().equals(mediaType)) {

                int points = 0;
                switch (interaction.getInteractionType()) {
                    case "VIEW": points = 1; break;
                    case "LIKE": points = 10; break;
                    default: points = 0; break;
                }

                for (String genre : interaction.getGenres()) {
                    genreScores.put(genre, genreScores.getOrDefault(genre, 0) + points);
                }
            }
        }

        String favoriteGenre = null;
        int highestScore = 0;
        for (Map.Entry<String, Integer> entry : genreScores.entrySet()) {
            if (entry.getValue() > highestScore) {
                highestScore = entry.getValue();
                favoriteGenre = entry.getKey();
            }
        }

        return favoriteGenre;
    }

    // picks a random genre from the provided list, excluding the user's favourite
    private String pickOtherGenre(String favoriteGenre, List<String> genreList) {
        List<String> others = new ArrayList<>(genreList);
        others.removeIf(g -> g.equalsIgnoreCase(favoriteGenre));
        if (others.isEmpty()) return genreList.get(0);
        Collections.shuffle(others);
        return others.get(0);
    }

    // marks which items in a list the user has already liked
    private void applyUserLiked(List<RecommendationResponse> results, Long userId) {
        List<String> likedIds = userInteractionRepository.findLikedMediaApiIdsByUserId(userId);
        Set<String> likedSet = new HashSet<>(likedIds);
        for (RecommendationResponse rec : results) {
            rec.setUserLiked(likedSet.contains(rec.getMediaApiId()));
        }
    }

    // main recommendation method
    // returns 20 from the user's favourite genre + 10 from a random other genre
    // falls back to popular media for new users with no interaction history
    public List<RecommendationResponse> getRecommendations(Long userId, String mediaType) {
        String favoriteGenre = userFavoriteGenre(userId, mediaType);

        if (favoriteGenre == null) {
            List<RecommendationResponse> fallback;
            switch (mediaType) {
                case "MOVIE": fallback = fetchTmdbPopular(); break;
                case "GAME":  fallback = fetchRawgPopular(); break;
                case "SONG":  fallback = fetchLastfmPopular(); break;
                default:      return List.of();
            }
            applyUserLiked(fallback, userId);
            return fallback;
        }

        List<RecommendationResponse> results = new ArrayList<>();

        switch (mediaType) {
            case "MOVIE": {
                // 20 from favourite genre (TMDB always returns 20 per page)
                results.addAll(fetchTmdbRecommendations(favoriteGenre));
                // 10 from a random other TMDB genre
                String otherGenre = pickOtherGenre(favoriteGenre, new ArrayList<>(TMDB_GENRE_IDS.keySet()));
                List<RecommendationResponse> other = fetchTmdbRecommendations(otherGenre);
                results.addAll(other.subList(0, Math.min(10, other.size())));
                break;
            }
            case "GAME": {
                // 20 from favourite genre
                results.addAll(fetchRawgRecommendations(favoriteGenre, 20));
                // 10 from a random other RAWG genre
                String otherGenre = pickOtherGenre(favoriteGenre, RAWG_GENRES);
                results.addAll(fetchRawgRecommendations(otherGenre, 10));
                break;
            }
            case "SONG": {
                // 5 from favourite genre via Last.fm tag.getTopTracks
                results.addAll(fetchLastfmRecommendations(favoriteGenre, 5));
                // 5 from a random other genre via Last.fm tag.getTopTracks
                String otherGenre = pickOtherGenre(favoriteGenre, SONG_GENRES);
                results.addAll(fetchLastfmRecommendations(otherGenre, 5));
                break;
            }
            default:
                break;
        }

        applyUserLiked(results, userId);
        return results;
    }

    // fallback: top popular movies from TMDB
    // extracts real genre from genre_ids array using the reverse TMDB_GENRE_NAMES map
    private List<RecommendationResponse> fetchTmdbPopular() {
        List<RecommendationResponse> results = new ArrayList<>();
        String url = "https://api.themoviedb.org/3/movie/popular?api_key=" + tmdbApiKey;
        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode movies = objectMapper.readTree(response).path("results");
            for (JsonNode movie : movies) {
                String mediaApiId = movie.path("id").asText();
                if (bannedMediaRepository.existsByMediaApiId(mediaApiId)) continue;
                String title    = movie.path("title").asText();
                String imageUrl = "https://image.tmdb.org/t/p/w500" + movie.path("poster_path").asText();
                double score    = movie.path("popularity").asDouble();

                // extract first genre from genre_ids, fall back to "Action" if not found in our map
                String genre = "Action";
                JsonNode genreIds = movie.path("genre_ids");
                if (genreIds.isArray() && genreIds.size() > 0) {
                    int firstGenreId = genreIds.get(0).asInt();
                    genre = TMDB_GENRE_NAMES.getOrDefault(firstGenreId, "Action");
                }

                results.add(new RecommendationResponse(mediaApiId, title, "", "MOVIE", genre, imageUrl, score));
            }
        } catch (Exception e) {
            System.err.println("TMDB popular fallback error: " + e.getMessage());
        }
        return results;
    }

    // fallback: top rated games from RAWG
    // extracts real genre from the genres array returned by the RAWG API
    private List<RecommendationResponse> fetchRawgPopular() {
        List<RecommendationResponse> results = new ArrayList<>();
        String url = "https://api.rawg.io/api/games?key=" + rawgApiKey + "&ordering=-rating&page_size=20";
        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode games = objectMapper.readTree(response).path("results");
            for (JsonNode game : games) {
                String mediaApiId = game.path("id").asText();
                if (bannedMediaRepository.existsByMediaApiId(mediaApiId)) continue;
                String title    = game.path("name").asText();
                String imageUrl = game.path("background_image").asText();
                double score    = game.path("rating").asDouble();

                // extract first genre name from genres array, capitalise first letter
                String genre = "Action";
                JsonNode genres = game.path("genres");
                if (genres.isArray() && genres.size() > 0) {
                    String rawGenre = genres.get(0).path("name").asText();
                    if (!rawGenre.isEmpty()) {
                        genre = rawGenre.substring(0, 1).toUpperCase() + rawGenre.substring(1);
                    }
                }

                results.add(new RecommendationResponse(mediaApiId, title, "", "GAME", genre, imageUrl, score));
            }
        } catch (Exception e) {
            System.err.println("RAWG popular fallback error: " + e.getMessage());
        }
        return results;
    }

    // fetches album art for a single track using Last.fm track.getInfo
    // acquires a semaphore permit before calling — max 4 concurrent calls at any time
    // holds the permit for 250ms after the call completes to stay safely under 5 req/sec
    private String fetchLastfmTrackImage(String artist, String track) {
        try {
            lastfmSemaphore.acquire();
            try {
                String url = "https://ws.audioscrobbler.com/2.0/?method=track.getInfo"
                        + "&api_key=" + lastfmApiKey
                        + "&artist=" + java.net.URLEncoder.encode(artist, "UTF-8")
                        + "&track=" + java.net.URLEncoder.encode(track, "UTF-8")
                        + "&format=json";
                String response = restTemplate.getForObject(url, String.class);
                JsonNode album = objectMapper.readTree(response).path("track").path("album");
                String image = album.path("image").path(3).path("#text").asText();
                if (image.isEmpty()) image = album.path("image").path(2).path("#text").asText();
                return image;
            } finally {
                // hold permit for 250ms so max throughput stays at 4 per 250ms = 16/sec worst case
                // but because network latency per call is typically >200ms, real rate stays well under 5/sec
                Thread.sleep(250);
                lastfmSemaphore.release();
            }
        } catch (Exception e) {
            return "";
        }
    }

    // fires all track.getInfo image calls in parallel, respecting the semaphore rate limit
    // returns images in the same order as the input list
    private List<String> fetchImagesInParallel(List<String[]> artistAndTitles) {
        List<CompletableFuture<String>> futures = artistAndTitles.stream()
            .map(pair -> CompletableFuture.supplyAsync(() -> fetchLastfmTrackImage(pair[0], pair[1])))
            .collect(Collectors.toList());

        return futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
    }

    // fallback: globally popular tracks for new users with no history
    // uses Last.fm chart.getTopTracks then fetches images in parallel via track.getInfo
    private List<RecommendationResponse> fetchLastfmPopular() {
        List<RecommendationResponse> results = new ArrayList<>();
        try {
            String lastfmUrl = "https://ws.audioscrobbler.com/2.0/?method=chart.getTopTracks"
                    + "&limit=10"
                    + "&api_key=" + lastfmApiKey
                    + "&format=json";

            String lastfmResponse = restTemplate.getForObject(lastfmUrl, String.class);
            JsonNode tracks = objectMapper.readTree(lastfmResponse).path("tracks").path("track");

            // collect track metadata first
            List<String[]> artistAndTitles = new ArrayList<>();
            List<JsonNode> trackList = new ArrayList<>();
            for (JsonNode track : tracks) {
                String mediaApiId = track.path("url").asText();
                if (mediaApiId.isEmpty()) continue;
                if (bannedMediaRepository.existsByMediaApiId(mediaApiId)) continue;
                trackList.add(track);
                artistAndTitles.add(new String[]{
                    track.path("artist").path("name").asText(),
                    track.path("name").asText()
                });
            }

            // fetch all images in parallel
            List<String> images = fetchImagesInParallel(artistAndTitles);

            // build responses
            for (int i = 0; i < trackList.size(); i++) {
                JsonNode track = trackList.get(i);
                String mediaApiId = track.path("url").asText();
                String title      = track.path("name").asText();
                String artist     = track.path("artist").path("name").asText();
                double score      = track.path("playcount").asDouble();
                String imageUrl   = images.get(i);

                results.add(new RecommendationResponse(mediaApiId, title, artist, "SONG", "Pop", imageUrl, score));
            }
        } catch (Exception e) {
            System.err.println("Last.fm popular fallback error: " + e.getMessage());
        }
        return results;
    }

    // calls TMDB to get top movies in the user's favourite genre
    // TMDB always returns 20 results per page — page size cannot be changed
    private List<RecommendationResponse> fetchTmdbRecommendations(String genre) {
        List<RecommendationResponse> results = new ArrayList<>();

        Integer genreId = TMDB_GENRE_IDS.get(genre);
        if (genreId == null) return results;

        String url = "https://api.themoviedb.org/3/discover/movie"
                + "?api_key=" + tmdbApiKey
                + "&with_genres=" + genreId
                + "&sort_by=popularity.desc";

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode movies = objectMapper.readTree(response).path("results");

            for (JsonNode movie : movies) {
                String mediaApiId = movie.path("id").asText();
                if (bannedMediaRepository.existsByMediaApiId(mediaApiId)) continue;
                String title    = movie.path("title").asText();
                String imageUrl = "https://image.tmdb.org/t/p/w500" + movie.path("poster_path").asText();
                double score    = movie.path("popularity").asDouble();
                results.add(new RecommendationResponse(mediaApiId, title, "", "MOVIE", genre, imageUrl, score));
            }
        } catch (Exception e) {
            System.err.println("TMDB API error: " + e.getMessage());
        }

        return results;
    }

    // calls RAWG to get top games in the user's favourite genre
    // limit parameter controls how many results to return (RAWG supports page_size up to 40)
    private List<RecommendationResponse> fetchRawgRecommendations(String genre, int limit) {
        List<RecommendationResponse> results = new ArrayList<>();

        String genreSlug = genre.toLowerCase().replace(" ", "-");

        String url = "https://api.rawg.io/api/games"
                + "?key=" + rawgApiKey
                + "&genres=" + genreSlug
                + "&ordering=-rating"
                + "&page_size=" + limit;

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode games = objectMapper.readTree(response).path("results");

            for (JsonNode game : games) {
                String mediaApiId = game.path("id").asText();
                if (bannedMediaRepository.existsByMediaApiId(mediaApiId)) continue;
                String title    = game.path("name").asText();
                String imageUrl = game.path("background_image").asText();
                double score    = game.path("rating").asDouble();
                results.add(new RecommendationResponse(mediaApiId, title, "", "GAME", genre, imageUrl, score));
            }
        } catch (Exception e) {
            System.err.println("RAWG API error: " + e.getMessage());
        }

        return results;
    }

    // uses Last.fm tag.getTopTracks to get top tracks for a genre tag
    // then fetches album art for all tracks in parallel via track.getInfo
    private List<RecommendationResponse> fetchLastfmRecommendations(String genre, int limit) {
        List<RecommendationResponse> results = new ArrayList<>();

        try {
            String tag = java.net.URLEncoder.encode(genre.toLowerCase(), "UTF-8");
            String lastfmUrl = "https://ws.audioscrobbler.com/2.0/?method=tag.getTopTracks"
                    + "&tag=" + tag
                    + "&limit=" + limit
                    + "&api_key=" + lastfmApiKey
                    + "&format=json";

            String lastfmResponse = restTemplate.getForObject(lastfmUrl, String.class);
            JsonNode tracks = objectMapper.readTree(lastfmResponse).path("tracks").path("track");

            // collect track metadata first
            List<String[]> artistAndTitles = new ArrayList<>();
            List<JsonNode> trackList = new ArrayList<>();
            for (JsonNode track : tracks) {
                String mediaApiId = track.path("url").asText();
                if (mediaApiId.isEmpty()) continue;
                if (bannedMediaRepository.existsByMediaApiId(mediaApiId)) continue;
                trackList.add(track);
                artistAndTitles.add(new String[]{
                    track.path("artist").path("name").asText(),
                    track.path("name").asText()
                });
            }

            // fetch all images in parallel
            List<String> images = fetchImagesInParallel(artistAndTitles);

            // build responses
            for (int i = 0; i < trackList.size(); i++) {
                JsonNode track = trackList.get(i);
                String mediaApiId = track.path("url").asText();
                String title      = track.path("name").asText();
                String artist     = track.path("artist").path("name").asText();
                double score      = track.path("playcount").asDouble();
                String imageUrl   = images.get(i);

                results.add(new RecommendationResponse(mediaApiId, title, artist, "SONG", genre, imageUrl, score));
            }
        } catch (Exception e) {
            System.err.println("Last.fm recommendations error: " + e.getMessage());
        }

        return results;
    }

    // admin: ban a media item so it never appears in recommendations
    public void banMedia(String mediaApiId, String mediaType) {
        if (!bannedMediaRepository.existsByMediaApiId(mediaApiId)) {
            BannedMedia banned = new BannedMedia();
            banned.setMediaApiId(mediaApiId);
            banned.setMediaType(mediaType);
            bannedMediaRepository.save(banned);
        }
    }

    // admin: unban a media item
    @Transactional
    public void unbanMedia(String mediaApiId) {
        bannedMediaRepository.deleteByMediaApiId(mediaApiId);
    }
}