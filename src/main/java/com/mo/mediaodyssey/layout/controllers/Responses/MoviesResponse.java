package com.mo.mediaodyssey.layout.controllers.Responses;

import java.util.List;

import com.mo.mediaodyssey.layout.controllers.models.Movies;

public class MoviesResponse {

    private List<Movies> movieResults; 
    
    public List<Movies> getResults() {
        return movieResults;
    }

    public void setResults(List<Movies> movieResults) {
        this.movieResults = movieResults; 
    }
}
