package com.mo.mediaodyssey.socialFeature.models.DTO;

public class CommentDTO {

    private Long id;
    private Long postId;
    private Long authorId;
    private Long parentId;
    private String content;
    private String username;
    private boolean deleted;
    private int depth;

    public CommentDTO(Long id,
                      Long postId,
                      Long authorId,
                      Long parentId,
                      String content,
                      String username,
                      int depth,
                      boolean deleted) {
        this.id = id;
        this.postId = postId;
        this.authorId = authorId;
        this.parentId = parentId;
        this.content = content;
        this.username = username;
        this.depth = depth;
        this.deleted = deleted;
    }

    public Long getId() { return id; }
    public Long getPostId() { return postId; }
    public Long getAuthorId() { return authorId; }
    public Long getParentId() { return parentId; }
    public String getContent() { return content; }
    public String getUsername() { return username; }
    public int getDepth() { return depth; }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }
}