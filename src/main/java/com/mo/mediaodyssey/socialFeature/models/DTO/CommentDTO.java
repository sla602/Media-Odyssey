package com.mo.mediaodyssey.socialFeature.models.DTO;

public class CommentDTO {

    private Integer id;
    private Integer postId;
    private Integer authorId;
    private Integer parentId;
    private String content;
    private String username;
    private int depth;

    public CommentDTO(Integer id,
                      Integer postId,
                      Integer authorId,
                      Integer parentId,
                      String content,
                      String username,
                      int depth) {
        this.id = id;
        this.postId = postId;
        this.authorId = authorId;
        this.parentId = parentId;
        this.content = content;
        this.username = username;
        this.depth = depth;
    }

    public Integer getId() { return id; }
    public Integer getPostId() { return postId; }
    public Integer getAuthorId() { return authorId; }
    public Integer getParentId() { return parentId; }
    public String getContent() { return content; }
    public String getUsername() { return username; }
    public int getDepth() { return depth; }

    public void setDepth(int depth) {
        this.depth = depth;
    }
}