package com.mo.mediaodyssey.models;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name="post",
indexes = {
        @Index(name = "idx_post_community", columnList = "community_id"),
        @Index(name = "idx_post_author", columnList = "author_id")
})
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "community_id", nullable = false)
    private Integer communityId;

    @Column(name = "author_id", nullable = false)
    private Integer authorId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 10000)
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Post() {}

    public Post(Integer communityId, Integer authorId, String title, String content) {
        this.communityId = communityId;
        this.authorId = authorId;
        this.title = title;
        this.content = content;
    }


    public Integer getId() {
        return id;
    }

    public Integer getCommunityId() {
        return communityId;
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
}
