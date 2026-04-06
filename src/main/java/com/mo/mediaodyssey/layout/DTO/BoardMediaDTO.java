package com.mo.mediaodyssey.layout.DTO;

public class BoardMediaDTO {
    private Long id; 
    private Long mediaApiId;
    private String mediaType;

    private String title;
    private String poster_path;
    private String artist;
    public BoardMediaDTO() {}; 
    public BoardMediaDTO(Long id, Long mediaApiId, String mediaType, String title, String poster_path) {
        this.id = id;
        this.mediaApiId = mediaApiId;
        this.mediaType = mediaType;
        this.title = title;
        this.poster_path = poster_path;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getMediaApiId() {
        return mediaApiId;
    }
    public void setMediaApiId(Long mediaApiId) {
        this.mediaApiId = mediaApiId;
    }
    public String getMediaType() {
        return mediaType;
    }
    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getPoster_path() {
        return poster_path;
    }
    public void setPoster_path(String poster_path) {
        this.poster_path = poster_path;
    }
    public String getArtist() {
        return artist;
    }
    public void setArtist(String artist) {
        this.artist = artist;
    }
}
