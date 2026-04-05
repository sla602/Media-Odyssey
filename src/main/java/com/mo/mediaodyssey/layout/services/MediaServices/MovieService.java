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
        MovieResponse movie = getMovieById(movieId);

        if (movie != null) {
            List<WatchProviders> providers = getStreamProvidersForMovie(movieId);
            movie.setWatch_providers(providers);
        }

        return movie;
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
    // Get Streaming Platforms (watch_providers)
    private List<WatchProviders> getStreamProvidersForMovie (Long movieId) {
        String url = baseURL + movieId + "/watch/providers?api_key=" + tmdbKey; 

        Map<String, Object> response = restTemplate.getForObject(url, Map.class); 
        List<WatchProviders> providers = new ArrayList<>();

        // safety condition
        if (response == null || !response.containsKey("results")) {
            return providers; 
        }
        Map<String, Object> results = (Map<String, Object>) response.get("results");

        if (!results.containsKey("CA")) {
            return providers;
        }

        Map<String, Object> caData = (Map<String, Object>) results.get("CA");

        if (caData == null || !caData.containsKey("flatrate")) {
            return providers;
        }

        List<Map<String, Object>> flatrateList = (List<Map<String, Object>>) caData.get("flatrate");

        for (Map<String, Object> provider : flatrateList) {

            String name = (String) provider.get("provider_name");
            String logoPath = (String) provider.get("logo_path");

            String logoUrl = logoPath != null
                    ? "https://image.tmdb.org/t/p/w200" + logoPath
                    : null;

            providers.add(new WatchProviders(name, logoUrl));
        }

        return providers;
    }
}
