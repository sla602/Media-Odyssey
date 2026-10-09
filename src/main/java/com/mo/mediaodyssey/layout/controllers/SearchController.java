package com.mo.mediaodyssey.layout.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mo.mediaodyssey.recommendation.UserInteractionRepository;
import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.shared.services.CurrentAccountService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class SearchController {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final UserInteractionRepository userInteractionRepository;
    private final CurrentAccountService currentAccountService;

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    @Value("${rawg.api.key}")
    private String rawgApiKey;

    @Value("${lastfm.api.key}")
    private String lastfmApiKey;

    // mirrors the map in RecommendationService — TMDB genre ID -> genre name
    private static final Map<Integer, String> TMDB_GENRE_NAMES = new HashMap<>();
    static {
        TMDB_GENRE_NAMES.put(28, "Action");
        TMDB_GENRE_NAMES.put(12, "Adventure");
        TMDB_GENRE_NAMES.put(16, "Animation");
        TMDB_GENRE_NAMES.put(35, "Comedy");
        TMDB_GENRE_NAMES.put(80, "Crime");
        TMDB_GENRE_NAMES.put(99, "Documentary");
        TMDB_GENRE_NAMES.put(18, "Drama");
        TMDB_GENRE_NAMES.put(14, "Fantasy");
        TMDB_GENRE_NAMES.put(27, "Horror");
        TMDB_GENRE_NAMES.put(9648, "Mystery");
        TMDB_GENRE_NAMES.put(10749, "Romance");
        TMDB_GENRE_NAMES.put(878, "Science Fiction");
        TMDB_GENRE_NAMES.put(53, "Thriller");
        TMDB_GENRE_NAMES.put(10751, "Family");
        TMDB_GENRE_NAMES.put(36, "History");
        TMDB_GENRE_NAMES.put(10402, "Music");
        TMDB_GENRE_NAMES.put(10752, "War");
        TMDB_GENRE_NAMES.put(37, "Western");
    }

    public SearchController(UserInteractionRepository userInteractionRepository,
            CurrentAccountService currentAccountService) {
        this.userInteractionRepository = userInteractionRepository;
        this.currentAccountService = currentAccountService;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    // GET /search — renders the search page
    @GetMapping("/search")
    public String searchPage(@RequestParam(required = false) String q,
            @RequestParam(required = false, defaultValue = "MOVIE") String type,
            Model model,
            Authentication authentication) {

        User user = currentAccountService.getCurrentAccount(authentication);

        model.addAttribute("user", user);
        model.addAttribute("query", q != null ? q : "");
        model.addAttribute("type", type);
        return "boardsLayout/search";
    }

    // GET /api/search?q=...&type=MOVIE|GAME|SONG — returns JSON results
    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> searchApi(
            @RequestParam String q,
            @RequestParam(defaultValue = "MOVIE") String type,
            Authentication authentication) {

        User user = currentAccountService.getCurrentAccount(authentication);

        // load all liked media IDs for this user once — checked per result below
        Set<String> likedIds = new HashSet<>(
                userInteractionRepository.findLikedMediaApiIdsByUserId(user.getId()));

        List<Map<String, Object>> results = new ArrayList<>();

        try {
            switch (type.toUpperCase()) {
                case "MOVIE":
                    results = searchMovies(q, likedIds);
                    break;
                case "GAME":
                    results = searchGames(q, likedIds);
                    break;
                case "SONG":
                    results = searchSongs(q, likedIds);
                    break;
            }
        } catch (Exception e) {
            System.err.println("Search error: " + e.getMessage());
        }
        return ResponseEntity.ok(results);
    }

    private List<Map<String, Object>> searchMovies(String query, Set<String> likedIds) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();
        String encoded = URLEncoder.encode(query, "UTF-8");
        String url = "https://api.themoviedb.org/3/search/movie"
                + "?api_key=" + tmdbApiKey
                + "&query=" + encoded
                + "&page=1";

        String response = restTemplate.getForObject(url, String.class);
        JsonNode movies = objectMapper.readTree(response).path("results");

        for (JsonNode movie : movies) {
            String posterPath = movie.path("poster_path").asText("");
            String imageUrl = posterPath.isBlank() ? "" : "https://image.tmdb.org/t/p/w500" + posterPath;
            double score = movie.path("popularity").asDouble();

            String genre = "Movie";
            JsonNode genreIds = movie.path("genre_ids");
            if (genreIds.isArray() && genreIds.size() > 0) {
                genre = TMDB_GENRE_NAMES.getOrDefault(genreIds.get(0).asInt(), "Movie");
            }

            String mediaApiId = movie.path("id").asText();
            results.add(Map.of(
                    "mediaApiId", mediaApiId,
                    "title", movie.path("title").asText(),
                    "artist", "",
                    "mediaType", "MOVIE",
                    "genre", genre,
                    "imageUrl", imageUrl,
                    "score", score,
                    "userLiked", likedIds.contains(mediaApiId)));
        }
        return results;
    }

    private List<Map<String, Object>> searchGames(String query, Set<String> likedIds) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();
        String encoded = URLEncoder.encode(query, "UTF-8");
        String url = "https://api.rawg.io/api/games"
                + "?key=" + rawgApiKey
                + "&search=" + encoded
                + "&page_size=20";

        String response = restTemplate.getForObject(url, String.class);
        JsonNode games = objectMapper.readTree(response).path("results");

        for (JsonNode game : games) {
            String imageUrl = game.path("background_image").asText("");
            double score = game.path("metacritic").asDouble();

            String genre = "Game";
            JsonNode genres = game.path("genres");
            if (genres.isArray() && genres.size() > 0) {
                String raw = genres.get(0).path("name").asText();
                if (!raw.isBlank())
                    genre = raw.substring(0, 1).toUpperCase() + raw.substring(1);
            }

            String mediaApiId = game.path("id").asText();
            results.add(Map.of(
                    "mediaApiId", mediaApiId,
                    "title", game.path("name").asText(),
                    "artist", "",
                    "mediaType", "GAME",
                    "genre", genre,
                    "imageUrl", imageUrl,
                    "score", score,
                    "userLiked", likedIds.contains(mediaApiId)));
        }
        return results;
    }

    private List<Map<String, Object>> searchSongs(String query, Set<String> likedIds) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();
        String encoded = URLEncoder.encode(query, "UTF-8");
        String url = "https://ws.audioscrobbler.com/2.0/?method=track.search"
                + "&track=" + encoded
                + "&api_key=" + lastfmApiKey
                + "&format=json"
                + "&limit=20";

        String response = restTemplate.getForObject(url, String.class);
        JsonNode tracks = objectMapper.readTree(response)
                .path("results").path("trackmatches").path("track");

        for (JsonNode track : tracks) {
            String mediaApiId = track.path("url").asText();
            if (mediaApiId.isBlank())
                continue;

            String title = track.path("name").asText();
            String artist = track.path("artist").asText();
            String genre = resolveLastfmGenre(title, artist);

            results.add(Map.of(
                    "mediaApiId", mediaApiId,
                    "title", title,
                    "artist", artist,
                    "mediaType", "SONG",
                    "genre", genre != null ? genre : "",
                    "imageUrl", "",
                    "score", 0.0,
                    "userLiked", likedIds.contains(mediaApiId)));
        }
        return results;
    }

    // calls track.getTopTags and returns the first meaningful tag, or null if none
    // found
    private String resolveLastfmGenre(String trackName, String artist) {
        try {
            String encodedTrack = URLEncoder.encode(trackName, "UTF-8");
            String encodedArtist = URLEncoder.encode(artist, "UTF-8");
            String url = "https://ws.audioscrobbler.com/2.0/?method=track.getTopTags"
                    + "&track=" + encodedTrack
                    + "&artist=" + encodedArtist
                    + "&api_key=" + lastfmApiKey
                    + "&format=json";

            String response = restTemplate.getForObject(url, String.class);
            JsonNode tags = objectMapper.readTree(response).path("toptags").path("tag");

            if (tags.isArray()) {
                for (JsonNode tag : tags) {
                    String name = tag.path("name").asText().trim();
                    if (!name.isBlank()
                            && !name.equalsIgnoreCase("seen live")
                            && !name.equalsIgnoreCase("favourites")
                            && !name.equalsIgnoreCase("favorite")) {
                        return name.substring(0, 1).toUpperCase() + name.substring(1);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Last.fm tag lookup error: " + e.getMessage());
        }
        return null;
    }
}