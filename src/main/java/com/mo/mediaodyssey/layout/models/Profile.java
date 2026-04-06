package com.mo.mediaodyssey.layout.models;

import jakarta.persistence.*;

/**
 * User profile — username, description, pronouns.
 * Separate from the auth User so auth stays login-focused
 * and profile info can evolve independently.
 */
@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "username", length = 60)
    private String username;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "pronouns", length = 40)
    private String pronouns;

    public Profile() {}

    public Profile(Long userId) {
        this.userId = userId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPronouns() { return pronouns; }
    public void setPronouns(String pronouns) { this.pronouns = pronouns; }
}