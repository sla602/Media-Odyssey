package com.mo.mediaodyssey.layout.services.MediaServices;

import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.mo.mediaodyssey.layout.DTO.MoviesTMDB.MovieResponse;
import com.mo.mediaodyssey.layout.DTO.MoviesTMDB.WatchProviders;

@Service
public class MovieService {

    @Value("${tmdb.api.key}")
    private String tmdbKey; 
    private final String baseURL = "https://api.themoviedb.org/3/movie/";

    private final RestTemplate restTemplate; 

    public MovieService (RestTemplate restTemplate) {
        this.restTemplate = restTemplate; 
    }

    /* Find movies:
    ** ==== GetMovieById: 
    *** This function is used to translate the web result to json object.
    *** This function would be used most for movies that are already displayed on the browser.
    *** Because fetching and finding by movieId would be more accurate for data displayed in movieDisplay.html
    *** Main focus: title, poster_path, genres (map), overview, released date, ...
    */
    public MovieResponse getMovieById(Long movieId) {
        String url = baseURL + movieId 
                    + "?api_key=" + tmdbKey; 

        return restTemplate.getForObject(url, MovieResponse.class);
    }

    // Get the movie + watch providers (2 api calls in total)
    public MovieResponse getMovieWithProviders(Long movieId) {
        try {
            MovieResponse movie = getMovieById(movieId);

            if (movie != null) {
                List<WatchProviders> providers = getStreamProvidersForMovie(movieId);
                movie.setWatch_providers(providers);
            }

            return movie;
        } catch (Exception e) {
            e.printStackTrace();
            return null; 
        }
    }

    /* This function will assist in getting a list of movies instead of just one.*/
    public List<MovieResponse> getMoviesByIds (List<Long> mediaApiIds) {

        List<MovieResponse> movies = new ArrayList<>(); 

        for (Long id: mediaApiIds) {
            MovieResponse movie = getMovieWithProviders(id); 
            if(movie != null) {
                movies.add(movie);
            }
        }

        return movies; 
    }
    
    // =================== Private functions =========================================
    // Get Streaming Platforms (watch_providers) provided from Canada specifically
    private List<WatchProviders> getStreamProvidersForMovie(Long movieId) {

        String url = baseURL + movieId + "/watch/providers?api_key=" + tmdbKey;
        List<WatchProviders> providers = new ArrayList<>();

        try {

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return providers;
            Object resultsObj = response.get("results");
            if (!(resultsObj instanceof Map)) return providers;

            Map<String, Object> results = (Map<String, Object>) resultsObj;
            Object caObj = results.get("CA"); 
            if (!(caObj instanceof Map)) return providers;

            Map<String, Object> caData = (Map<String, Object>) caObj;

            List<Map<String, Object>> allProviders = new ArrayList<>();

            if (caData.get("buy") instanceof List<?>) {
                allProviders.addAll((List<Map<String, Object>>) caData.get("buy"));
            }

            if (caData.get("rent") instanceof List<?>) {
                allProviders.addAll((List<Map<String, Object>>) caData.get("rent"));
            }

            for (Map<String, Object> provider : allProviders) {
                String name = (String) provider.get("provider_name");
                String logoPath = (String) provider.get("logo_path");

                if (name == null) continue;

                String logoUrl = logoPath != null
                        ? "https://image.tmdb.org/t/p/w200" + logoPath
                        : null;

                providers.add(new WatchProviders(name, logoUrl));
            }

        } catch (Exception e) {
            System.out.println("Provider fetch failed for movie " + movieId);
            e.printStackTrace();
        }

        return providers;
    }

    // This function avoids duplicate in getting the platforms (some offers both buy + rent)
    /* 
    private void addProvidersFromType(Map<String, Object> countryData,
                                  String type,
                                  List<WatchProviders> providers) {

        Object obj = countryData.get(type);

        if (!(obj instanceof List<?> list)) return;

        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) continue;

            String name = (String) map.get("provider_name");
            String logoPath = (String) map.get("logo_path");

            if (name == null) continue;

            String logoUrl = logoPath != null
                    ? "https://image.tmdb.org/t/p/w200" + logoPath
                    : null;

            boolean exists = providers.stream()
                    .anyMatch(p -> p.getProvider_name().equalsIgnoreCase(name));

            if (!exists) {
                providers.add(new WatchProviders(name, logoUrl));
            }
        }
    }
        */
}
