package com.mo.mediaodyssey.socialFeature.models;



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
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, length = 5000)
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();




    @Column(nullable = false)
    private boolean deleted;

    protected Comment() {}

    public Comment(Long postId, Long authorId, Long parentId, String content, boolean deleted) {
        this.postId = postId;
        this.authorId = authorId;
        this.parentId = parentId;
        this.content = content;
        this.deleted = deleted;
    }

    public Long getId() {
        return id;
    }

    public Long getPostId() {
        return postId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public Long getParentId() {
        return parentId;
    }

    public String getContent() {
        return content;
    }


    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isDeleted(){return deleted;}
    public void setContent(String content) {
        this.content = content;
    }
}