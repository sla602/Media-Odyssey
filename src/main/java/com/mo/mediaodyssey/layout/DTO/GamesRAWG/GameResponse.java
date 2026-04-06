package com.mo.mediaodyssey.layout.DTO.GamesRAWG;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GameResponse {

    private Long id;

    @JsonProperty("name")
    private String title; 

    @JsonProperty("description_raw")
    private String description; 

    @JsonProperty("background_image")
    private String poster_path; 

    @JsonProperty("released")
    private String release_date;

    private List<RAWGGenres> genres;
    private List<RAWGdevelopers> developers;
    
    // Constructor, getters, and setters:
    public GameResponse() {};
    public GameResponse(Long id, String title, String description, String poster_path, List<RAWGGenres> genres, String release_date, List<RAWGdevelopers> developers) {
        this.id = id;
        this.title = title; 
        this.description = description; 
        this.poster_path = poster_path; 
        this.genres = genres; 
        this.release_date = release_date;
        this.developers = developers;
    }
    
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getPoster_path() {
        return poster_path;
    }
    public void setPoster_path(String poster_path) {
        this.poster_path = poster_path;
    }
    public List<RAWGGenres> getGenres() {
        return genres;
    }
    public void setGenres(List<RAWGGenres> genres) {
        this.genres = genres;
    }
    public String getRelease_date() {
        return release_date;
    }
    public void setRelease_date(String release_date) {
        this.release_date = release_date;
    }
    public List<RAWGdevelopers> getDevelopers() {
        return developers;
    }
    public void setDevelopers(List<RAWGdevelopers> developers) {
        this.developers = developers;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    
    
}
