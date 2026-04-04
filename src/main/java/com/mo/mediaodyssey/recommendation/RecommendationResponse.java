package com.mo.mediaodyssey.recommendation;

// represents one recommended media item returned to the frontend
public class RecommendationResponse {
    private String mediaApiId;
    private String title;
    private String artist;
    private String mediaType;
    private String genre;
    private String imageUrl;
    private double score;
    private boolean userLiked;

    public RecommendationResponse(String mediaApiId, String title, String artist, String mediaType,
                                   String genre, String imageUrl, double score) {
        this.mediaApiId = mediaApiId;
        this.title = title;
        this.artist = artist;
        this.mediaType = mediaType;
        this.genre = genre;
        this.imageUrl = imageUrl;
        this.score = score;
        this.userLiked = false;
    }

    public String getMediaApiId() { return mediaApiId; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getMediaType() { return mediaType; }
    public String getGenre() { return genre; }
    public String getImageUrl() { return imageUrl; }
    public double getScore() { return score; }
    public boolean isUserLiked() { return userLiked; }
    public void setUserLiked(boolean userLiked) { this.userLiked = userLiked; }
}