package com.mo.mediaodyssey.models;


import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "comments",
        indexes = {
                @Index(name="idx_comment_post", columnList = "post_id"),
                @Index(name = "idx_comment_author", columnList = "author_id"),
                @Index(name = "idx_comment_parent", columnList = "parent_id")
        })
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "post_id", nullable = false)
    private Integer postId;

    @Column(name = "author_id", nullable = false)
    private Integer authorId;

    @Column(name = "parent_id")
    private Integer parentId;

    @Column(nullable = false, length = 5000)
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Comment() {}

    public Comment(Integer postId, Integer authorId, Integer parentId, String content) {
        this.postId = postId;
        this.authorId = authorId;
        this.parentId = parentId;
        this.content = content;
    }

    public Integer getId() {
        return id;
    }

    public Integer getPostId() {
        return postId;
    }

    public Integer getAuthorId() {
        return authorId;
    }

    public Integer getParentId() {
        return parentId;
    }

    public String getContent() {
        return content;
    }
}