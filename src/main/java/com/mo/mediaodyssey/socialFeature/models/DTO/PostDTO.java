package com.mo.mediaodyssey.socialFeature.models.DTO;

import java.time.Instant;

public class PostDTO {

    private Long id;
    private Long boardId;
    private Long authorId;
    private String title;
    private String content;
    private boolean deleted;
    private Instant createdAt;

    public PostDTO(Long id, Long boardId, Long authorId, String title, String content,boolean deleted, Instant createdAt){
        this.id = id;
        this.boardId = boardId;
        this.authorId = authorId;
        this.title = title;
        this.content = content;
        this.deleted = deleted;
    }

    public Long getId(){ return id; }
    public Long getBoardId(){ return boardId; }
    public Long getAuthorId(){return authorId;}
    public String getTitle(){ return title; }
    public String getContent(){ return content; }

    public boolean isDeleted() {
        return deleted;
    }
    public Instant getCreatedAt() { return createdAt;}
}