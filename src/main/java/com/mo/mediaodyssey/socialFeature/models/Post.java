package com.mo.mediaodyssey.socialFeature.models;

import jakarta.persistence.*;

import java.time.Instant;


@Entity
@Table(name="post",
indexes = {
        @Index(name = "idx_post_social_space_id", columnList = "social_space_id"),
        @Index(name = "idx_post_author", columnList = "author_id")
})
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    @Column(name = "social_space_id", nullable = false)
    private Integer socialSpaceId;

    @Column(name = "author_id", nullable = false)
    private Integer authorId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 10000)
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Post() {}

    public Post(Integer socialSpaceId, Integer authorId, String title, String content) {
        this.socialSpaceId = socialSpaceId;
        this.authorId = authorId;
        this.title = title;
        this.content = content;
    }


    public Integer getId() {
        return id;
    }

    public Integer getSocialSpaceId() {
        return socialSpaceId;
    }

    public Integer getAuthorId() {
        return authorId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }


}
