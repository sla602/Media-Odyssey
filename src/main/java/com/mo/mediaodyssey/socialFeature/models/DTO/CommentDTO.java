package com.mo.mediaodyssey.socialFeature.models.DTO;

import java.time.Instant;

public class CommentDTO {

    private Long id;
    private Long postId;
    private Long authorId;
    private Long parentId;
    private String content;
    private boolean deleted;
    private Instant createdAt;
    private int depth;

    public CommentDTO(Long id,
                      Long postId,
                      Long authorId,
                      Long parentId,
                      String content,
                      boolean deleted, Instant createdAt) {
        this.id = id;
        this.postId = postId;
        this.authorId = authorId;
        this.parentId = parentId;
        this.content = content;
        this.deleted = deleted;
        this.createdAt = createdAt;
        this.depth = 0;
    }

    public Long getId()             { return id; }
    public Long getPostId()         { return postId; }
    public Long getAuthorId()       { return authorId; }
    public Long getParentId()       { return parentId; }
    public String getContent()      { return content; }
    public boolean isDeleted()      { return deleted; }
    public Instant getCreatedAt()   { return createdAt; }
    public int getDepth()           { return depth; }
    public void setDepth(int depth) { this.depth = depth; }
}