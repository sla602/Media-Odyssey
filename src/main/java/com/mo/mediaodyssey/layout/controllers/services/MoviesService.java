package com.mo.mediaodyssey.layout.controllers.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.mo.mediaodyssey.layout.controllers.Responses.MoviesResponse;
import com.mo.mediaodyssey.layout.controllers.models.Movies;

@Service
public class MoviesService {
    
    @Value("${tmdb.api.key}")
    private String apiKey;

    private RestTemplate restTemplate = new RestTemplate(); 

    public Movies getMovie(String title) {
        String url = "https://api.themoviedb.org/3/search/movie?api_key=" + apiKey + "&query=" + title; 

        MoviesResponse response = restTemplate.getForObject(url, MoviesResponse.class); 

        return response.getResults().get(0); 
    }
}
