package com.mo.mediaodyssey.layout.models.MediaModels;

import java.util.List;

public class Games {
    private Long id; 
    private List<Integer> genres;
    private String title; 
    private String description;
    private String poster_path;
    private String release_date;
    private String publishers;

    public Games () {}
    public Games (Long id, List<Integer> genres, String title, String description, String poster_path, String release_date, String publishers){
        this.id = id;
        this.genres = genres; 
        this.title = title; 
        this.description = description; 
        this.poster_path = poster_path;
        this.release_date = release_date; 
        this.publishers = publishers;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public List<Integer> getGenres() {
        return genres;
    }
    public void setGenres(List<Integer> genres) {
        this.genres = genres;
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
    public String getRelease_date() {
        return release_date;
    }
    public void setRelease_date(String release_date) {
        this.release_date = release_date;
    }
    public String getPublishers() {
        return publishers;
    }
    public void setPublishers(String publishers) {
        this.publishers = publishers;
    }

}
