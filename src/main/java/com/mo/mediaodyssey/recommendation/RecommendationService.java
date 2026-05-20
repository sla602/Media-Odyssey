package com.mo.mediaodyssey.recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
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
    private final Random random = new Random();

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
        TMDB_GENRE_IDS.put("Family", 10751);
        TMDB_GENRE_IDS.put("History", 36);
        TMDB_GENRE_IDS.put("Music", 10402);
        TMDB_GENRE_IDS.put("War", 10752);
        TMDB_GENRE_IDS.put("Western", 37);
    }

    // reverse map: TMDB genre ID -> genre name
    private static final Map<Integer, String> TMDB_GENRE_NAMES = new HashMap<>();
    static {
        TMDB_GENRE_IDS.forEach((name, id) -> TMDB_GENRE_NAMES.put(id, name));
    }

    // genre tags used for Last.fm tag.getTopTracks — must be valid Last.fm tags
    private static final List<String> SONG_GENRES = Arrays.asList(
            "rock", "electronic", "alternative", "pop", "indie",
            "metal", "jazz", "ambient", "folk", "punk",
            "Hip-Hop", "hard rock", "soul", "dance",
            "Classical", "Soundtrack", "blues", "rap",
            "chillout", "electronica", "House", "techno");

    // genre slugs used for RAWG — must match RAWG's accepted genre slugs
    private static final List<String> RAWG_GENRES = Arrays.asList(
            "Action", "Adventure", "Arcade", "Board Games", "Card", "Casual",
            "Educational", "Family", "Fighting", "Indie", "Massively Multiplayer",
            "Platformer", "Puzzle", "Racing", "Role Playing Games RPG", "Shooter",
            "Simulation", "Sports", "Strategy");

    public RecommendationService(UserInteractionRepository userInteractionRepository,
            BannedMediaRepository bannedMediaRepository, RestTemplate restTemplate) {
        this.userInteractionRepository = userInteractionRepository;
        this.bannedMediaRepository = bannedMediaRepository;
        this.restTemplate = restTemplate;
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

        // Store display fields on LIKE so Liked Media page needs zero external API
        // calls
        if ("LIKE".equals(request.getInteractionType())) {
            interaction.setTitle(request.getTitle());
            interaction.setArtist(request.getArtist());
            interaction.setImageUrl(request.getImageUrl());
        }

        userInteractionRepository.save(interaction);
    }

    // removes a LIKE interaction for the given user + media item
    public void unlikeMedia(Long userId, String mediaApiId) {
        userInteractionRepository.deleteLikeByUserIdAndMediaApiId(userId, mediaApiId);
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
                    case "VIEW":
                        points = 1;
                        break;
                    case "LIKE":
                        points = 10;
                        break;
                    default:
                        points = 0;
                        break;
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
        if (others.isEmpty())
            return genreList.get(0);
        Collections.shuffle(others);
        return others.get(0);
    }

    private List<RecommendationResponse> filterAndMarkLiked(List<RecommendationResponse> results, Long userId) {
        List<String> likedIds = userInteractionRepository.findLikedMediaApiIdsByUserId(userId);
        Set<String> likedSet = new HashSet<>(likedIds);
        return results.stream()
                .filter(r -> !likedSet.contains(r.getMediaApiId()))
                .collect(Collectors.toList());
    }

    // main recommendation method
    // returns 20 from the user's favourite genre + 10 from a random other genre
    // falls back to popular media for new users with no interaction history
    public List<RecommendationResponse> getRecommendations(Long userId, String mediaType) {
        String favoriteGenre = userFavoriteGenre(userId, mediaType);

        if (favoriteGenre == null) {
            List<RecommendationResponse> fallback;
            switch (mediaType) {
                case "MOVIE":
                    fallback = fetchTmdbPopular();
                    break;
                case "GAME":
                    fallback = fetchRawgPopular();
                    break;
                case "SONG":
                    fallback = fetchLastfmPopular();
                    break;
                default:
                    return List.of();
            }
            return filterAndMarkLiked(fallback, userId);
        }

        List<RecommendationResponse> results = new ArrayList<>();

        switch (mediaType) {
            case "MOVIE": {
                results.addAll(fetchTmdbRecommendations(favoriteGenre));
                String otherGenre = pickOtherGenre(favoriteGenre, new ArrayList<>(TMDB_GENRE_IDS.keySet()));
                List<RecommendationResponse> other = fetchTmdbRecommendations(otherGenre);
                results.addAll(other.subList(0, Math.min(10, other.size())));
                break;
            }
            case "GAME": {
                String otherGenre = pickOtherGenre(favoriteGenre, RAWG_GENRES);
                CompletableFuture<List<RecommendationResponse>> favFuture = CompletableFuture
                        .supplyAsync(() -> fetchRawgRecommendations(favoriteGenre, 20));
                CompletableFuture<List<RecommendationResponse>> otherFuture = CompletableFuture
                        .supplyAsync(() -> fetchRawgRecommendations(otherGenre, 10));
                results.addAll(favFuture.join());
                results.addAll(otherFuture.join());
                break;
            }
            case "SONG": {
                results.addAll(fetchLastfmRecommendations(favoriteGenre, 20));
                String otherGenre = pickOtherGenre(favoriteGenre, SONG_GENRES);
                results.addAll(fetchLastfmRecommendations(otherGenre, 10));
                break;
            }
            default:
                break;
        }

        Map<String, RecommendationResponse> seen = new LinkedHashMap<>();
        for (RecommendationResponse r : results) {
            seen.putIfAbsent(r.getMediaApiId(), r);
        }
        results = new ArrayList<>(seen.values());

        return filterAndMarkLiked(results, userId);
    }

    // Loads all banned mediaApiIds into a Set in one query — used by all fetch
    // methods
    // to replace N individual existsByMediaApiId calls with a single DB round trip
    private Set<String> loadBannedIds() {
        return bannedMediaRepository.findAll().stream()
                .map(BannedMedia::getMediaApiId)
                .collect(Collectors.toSet());
    }

    // fallback: top popular movies from TMDB — random page for variety
    private List<RecommendationResponse> fetchTmdbPopular() {
        List<RecommendationResponse> results = new ArrayList<>();
        Set<String> banned = loadBannedIds();
        int page = random.nextInt(5) + 1;
        String url = "https://api.themoviedb.org/3/movie/popular?api_key=" + tmdbApiKey
                + "&page=" + page;
        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode movies = objectMapper.readTree(response).path("results");
            for (JsonNode movie : movies) {
                String mediaApiId = movie.path("id").asText();
                if (banned.contains(mediaApiId))
                    continue;
                String title = movie.path("title").asText();
                String imageUrl = "https://image.tmdb.org/t/p/w500" + movie.path("poster_path").asText();
                double score = movie.path("popularity").asDouble();

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

    // fallback: top rated games from RAWG — random page for variety
    private List<RecommendationResponse> fetchRawgPopular() {
        List<RecommendationResponse> results = new ArrayList<>();
        Set<String> banned = loadBannedIds();
        int page = random.nextInt(5) + 1;
        String url = "https://api.rawg.io/api/games?key=" + rawgApiKey +
                "&metacritic=70,100&page_size=20&page=" + page;
                
        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode games = objectMapper.readTree(response).path("results");
            for (JsonNode game : games) {
                String mediaApiId = game.path("id").asText();
                if (banned.contains(mediaApiId))
                    continue;
                String title = game.path("name").asText();
                String imageUrl = game.path("background_image").asText();
                double score = game.path("metacritic").asDouble();

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

    // fallback: picks 3 random genres from SONG_GENRES and fetches 10 tracks each
    // so each track has a real genre recorded instead of a hardcoded "Pop"
    private List<RecommendationResponse> fetchLastfmPopular() {
        List<RecommendationResponse> results = new ArrayList<>();

        List<String> shuffled = new ArrayList<>(SONG_GENRES);
        Collections.shuffle(shuffled);
        List<String> picked = shuffled.subList(0, 3);

        for (String genre : picked) {
            results.addAll(fetchLastfmRecommendations(genre, 10));
        }

        return results;
    }

    // calls TMDB to get top movies in the user's favourite genre — random page for
    // variety
    private List<RecommendationResponse> fetchTmdbRecommendations(String genre) {
        List<RecommendationResponse> results = new ArrayList<>();

        Integer genreId = TMDB_GENRE_IDS.get(genre);
        if (genreId == null)
            return results;

        Set<String> banned = loadBannedIds();
        int page = random.nextInt(10) + 1;
        String url = "https://api.themoviedb.org/3/discover/movie"
                + "?api_key=" + tmdbApiKey
                + "&with_genres=" + genreId
                + "&sort_by=popularity.desc"
                + "&page=" + page;

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode movies = objectMapper.readTree(response).path("results");

            for (JsonNode movie : movies) {
                String mediaApiId = movie.path("id").asText();
                if (banned.contains(mediaApiId))
                    continue;
                String title = movie.path("title").asText();
                String imageUrl = "https://image.tmdb.org/t/p/w500" + movie.path("poster_path").asText();
                double score = movie.path("popularity").asDouble();
                results.add(new RecommendationResponse(mediaApiId, title, "", "MOVIE", genre, imageUrl, score));
            }
        } catch (Exception e) {
            System.err.println("TMDB API error: " + e.getMessage());
        }

        return results;
    }

    // calls RAWG to get top games in the user's favourite genre — random page for
    // variety
    private List<RecommendationResponse> fetchRawgRecommendations(String genre, int limit) {
        List<RecommendationResponse> results = new ArrayList<>();

        String genreSlug = genre.toLowerCase().replace(" ", "-");
        Set<String> banned = loadBannedIds();
        int page = random.nextInt(5) + 1;

        String url = "https://api.rawg.io/api/games"
                + "?key=" + rawgApiKey
                + "&genres=" + genreSlug
                + "&metacritic=70,100"
                + "&page_size=" + limit //10 or 20 depending on whether it's the fav genre or the random genre
                + "&page=" + page;

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode games = objectMapper.readTree(response).path("results");

            for (JsonNode game : games) {
                String mediaApiId = game.path("id").asText();
                if (banned.contains(mediaApiId))
                    continue;
                String title = game.path("name").asText();
                String imageUrl = game.path("background_image").asText();
                double score = game.path("metacritic").asDouble();
                results.add(new RecommendationResponse(mediaApiId, title, "", "GAME", genre, imageUrl, score));
            }
        } catch (Exception e) {
            System.err.println("RAWG API error: " + e.getMessage());
        }

        return results;
    }

    // uses Last.fm tag.getTopTracks to get top tracks for a genre tag — random page
    // for variety
    private List<RecommendationResponse> fetchLastfmRecommendations(String genre, int limit) {
        List<RecommendationResponse> results = new ArrayList<>();
        try {
            Set<String> banned = loadBannedIds();
            String tag = java.net.URLEncoder.encode(genre.toLowerCase(), "UTF-8");
            int page = random.nextInt(5) + 1;
            String lastfmUrl = "https://ws.audioscrobbler.com/2.0/?method=tag.getTopTracks"
                    + "&tag=" + tag
                    + "&limit=" + limit
                    + "&page=" + page
                    + "&api_key=" + lastfmApiKey
                    + "&format=json";
            String lastfmResponse = restTemplate.getForObject(lastfmUrl, String.class);
            JsonNode tracks = objectMapper.readTree(lastfmResponse).path("tracks").path("track");
            for (JsonNode track : tracks) {
                String mediaApiId = track.path("url").asText();
                if (mediaApiId.isEmpty())
                    continue;
                if (banned.contains(mediaApiId))
                    continue;
                String title = track.path("name").asText();
                String artist = track.path("artist").path("name").asText();
                double score = track.path("playcount").asDouble();
                results.add(new RecommendationResponse(mediaApiId, title, artist, "SONG", genre, "", score));
            }
        } catch (Exception e) {
            System.err.println("Last.fm recommendations error: " + e.getMessage());
        }
        return results;
    }

    // Returns all media the user has liked directly from the DB — no external API
    // calls.
    // Title, artist, and imageUrl were stored at like-time from the frontend card
    // data.
    public List<RecommendationResponse> getLikedMedia(Long userId) {
        List<UserInteraction> liked = userInteractionRepository
                .findByUserIdAndInteractionType(userId, "LIKE");

        List<RecommendationResponse> results = new ArrayList<>();
        for (UserInteraction interaction : liked) {
            String genre = interaction.getGenres() != null && !interaction.getGenres().isEmpty()
                    ? interaction.getGenres().get(0)
                    : "";
            RecommendationResponse response = new RecommendationResponse(
                    interaction.getMediaApiId(),
                    interaction.getTitle() != null ? interaction.getTitle() : "",
                    interaction.getArtist() != null ? interaction.getArtist() : "",
                    interaction.getMediaType(),
                    genre,
                    interaction.getImageUrl() != null ? interaction.getImageUrl() : "",
                    0);
            response.setUserLiked(true);
            results.add(response);
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