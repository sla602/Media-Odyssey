package com.mo.mediaodyssey.layout.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class BoardMedia {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 

    @ManyToOne
    @JoinColumn(name="board_id", nullable = false) 
    private Boards board; 

    // Store the Media in Boards Model:
    private Long mediaApiId;
    @Column(name="media_type")
    private String mediaType;
    private String artist; 
    private String track; 

    private String media_title; 
    private String media_poster_path; 

    public BoardMedia (){}
    public BoardMedia(Long id, Boards board, Long mediaApiId, String mediaType, String artist, String track,
            String media_title, String media_poster_path) {
        this.id = id;
        this.board = board;
        this.mediaApiId = mediaApiId;
        this.mediaType = mediaType;
        this.artist = artist;
        this.track = track;
        this.media_title = media_title;
        this.media_poster_path = media_poster_path;
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boards getBoard() {
        return board;
    }

    public void setBoard(Boards board) {
        this.board = board;
    }

    public Long getMediaApiId() {
        return mediaApiId;
    }

    public void setMediaApiId(Long mediaApiId) {
        this.mediaApiId = mediaApiId;
    }
    public String getMedia_title() {
        return media_title;
    }

    public void setMedia_title(String media_title) {
        this.media_title = media_title;
    }

    public String getMedia_poster_path() {
        return media_poster_path;
    }

    public void setMedia_poster_path(String media_poster_path) {
        this.media_poster_path = media_poster_path;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getTrack() {
        return track;
    }

    public void setTrack(String track) {
        this.track = track;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    } 

    
}
