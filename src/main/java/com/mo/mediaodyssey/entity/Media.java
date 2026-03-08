package com.mo.mediaodyssey.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    // Category: "Movie", "Game", or "Song"
    private String category;

    // View count — incremented each time the item is visited
    private int views;

    // Current like count
    private int likes;

    // Like count from 7 days ago — used to calculate Fast-Rising growth %
    private int likesLastWeek;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt     = LocalDateTime.now();
        this.views         = 0;
        this.likes         = 0;
        this.likesLastWeek = 0;
    }

    // ── Computed fields (not stored in DB) ────────────────────────────────────

    /**
     * Point System:  totalScore = (views * 1) + (likes * 5)
     * Likes are weighted 5x because they indicate stronger interest than a passive view.
     * This score is what drives the Top 10 ranking order.
     */
    @Transient
    public int getTotalScore() {
        return (views * 1) + (likes * 5);
    }

    /**
     * Weekly growth rate = ((likes - likesLastWeek) / likesLastWeek) * 100
     * Used by the Fast-Rising section to show how much an item gained this week.
     * Returns 100.0 if likesLastWeek is 0 but likes exist (brand-new item with traction).
     */
    @Transient
    public double getTrendingGrowth() {
        if (likesLastWeek == 0) return likes > 0 ? 100.0 : 0.0;
        return ((double)(likes - likesLastWeek) / likesLastWeek) * 100.0;
    }

    /**
     * Returns true if weekly growth is >= 10%.
     * The Thymeleaf template uses this to decide whether to show the trending badge.
     */
    @Transient
    public boolean isTrending() {
        return getTrendingGrowth() >= 10.0;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }

    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public int getLikesLastWeek() { return likesLastWeek; }
    public void setLikesLastWeek(int likesLastWeek) { this.likesLastWeek = likesLastWeek; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}