package com.mo.mediaodyssey.recommendation;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "user_interaction",
        indexes = {
                @Index(name = "idx_ui_user_type_media", columnList = "user_id, interaction_type, media_api_id")
        })
public class UserInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String mediaApiId;
    private String interactionType; // "VIEW", "LIKE"
    private String mediaType;       // "MOVIE", "GAME", "SONG"
    private LocalDateTime timestamp;

    // Stored at like-time so the Liked Media page needs zero external API calls
    private String title;
    private String artist;
    private String imageUrl;

    @ElementCollection
    @CollectionTable(name = "user_interaction_genres", joinColumns = @JoinColumn(name = "interaction_id"),
            indexes = {
                    @Index(name = "idx_ui_genres_interaction", columnList = "interaction_id")
            })
    @Column(name = "genre")
    private List<String> genres;

    // getters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getMediaApiId() { return mediaApiId; }
    public String getInteractionType() { return interactionType; }
    public String getMediaType() { return mediaType; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public List<String> getGenres() { return genres; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getImageUrl() { return imageUrl; }

    // setters
    public void setUserId(Long userId) { this.userId = userId; }
    public void setMediaApiId(String mediaApiId) { this.mediaApiId = mediaApiId; }
    public void setInteractionType(String interactionType) { this.interactionType = interactionType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setGenres(List<String> genres) { this.genres = genres; }
    public void setTitle(String title) { this.title = title; }
    public void setArtist(String artist) { this.artist = artist; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
