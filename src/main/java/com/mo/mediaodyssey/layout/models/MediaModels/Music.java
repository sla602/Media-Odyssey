package com.mo.mediaodyssey.layout.models.MediaModels;

import java.util.List;

public class Music {
    private String title; 
    private String artist; 
    private String album; 
    private List<String> genres;
    private String poster_path;
    private String release_date;
    private String overview;

    public Music(){}
    public Music(String title, String artist, String album, String poster_path, List<String> genres, String release_date, String overview) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.poster_path = poster_path;
        this.release_date = release_date;
        this.overview = overview;
        this.genres = genres;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getArtist() {
        return artist;
    }
    public void setArtist(String artist) {
        this.artist = artist;
    }
    public String getAlbum() {
        return album;
    }
    public void setAlbum(String album) {
        this.album = album;
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
    public String getOverview() {
        return overview;
    }
    public void setOverview(String overview) {
        this.overview = overview;
    }
    public List<String> getGenres() {
        return genres;
    }
    public void setGenres(List<String> genres) {
        this.genres = genres;
    }
}
