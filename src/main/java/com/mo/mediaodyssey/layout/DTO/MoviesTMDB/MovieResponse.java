package com.mo.mediaodyssey.layout.DTO.MoviesTMDB;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MovieResponse {

    private Long id;
    private String title;
    private String overview; 

    @JsonProperty("release_date")
    private String release_dates;
    
    private List<TMDBGenres> genres;
    private String poster_path;
    private List<WatchProviders> watch_providers;

    public MovieResponse () {}

    public MovieResponse (Long id, String title, String overview, String release_dates, List<TMDBGenres> genres, String poster_path, List<WatchProviders> watchProviders){
        this.id = id;
        this.title = title; 
        this.overview = overview; 
        this.release_dates = release_dates; 
        this.genres = genres; 
        this.poster_path = poster_path;
        this.watch_providers = watchProviders;
    }

    public Long getId() {
        return id;
    }

    public void setId (Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getRelease_dates() {
        return release_dates;
    }

    public void setRelease_dates(String release_dates) {
        this.release_dates = release_dates;
    }

    public List<TMDBGenres> getGenres() {
        return genres;
    }

    public void setGenres(List<TMDBGenres> genres) {
        this.genres = genres;
    }

    public String getPoster_path() {
        return poster_path;
    }

    public void setPoster_path(String poster_path) {
        this.poster_path = poster_path;
    }

    public List<WatchProviders> getWatch_providers() {
        return watch_providers;
    }

    public void setWatch_providers(List<WatchProviders> watch_providers) {
        this.watch_providers = watch_providers;
    }
    
}
