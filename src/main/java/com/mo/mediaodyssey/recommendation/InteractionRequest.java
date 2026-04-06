package com.mo.mediaodyssey.recommendation;

import java.util.List;

// fields the frontend sends when recording a user interaction
public class InteractionRequest {
    private String mediaApiId;
    private String interactionType; // "VIEW" or "LIKE"
    private String mediaType;       // "MOVIE", "GAME", or "SONG"
    private List<String> genres;    // genres of the media item

    // Optional — sent on LIKE so the Liked Media page can display cards without extra API calls
    private String title;
    private String artist;
    private String imageUrl;

    public String getMediaApiId() { return mediaApiId; }
    public String getInteractionType() { return interactionType; }
    public String getMediaType() { return mediaType; }
    public List<String> getGenres() { return genres; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getImageUrl() { return imageUrl; }

    public void setMediaApiId(String mediaApiId) { this.mediaApiId = mediaApiId; }
    public void setInteractionType(String interactionType) { this.interactionType = interactionType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public void setGenres(List<String> genres) { this.genres = genres; }
    public void setTitle(String title) { this.title = title; }
    public void setArtist(String artist) { this.artist = artist; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}