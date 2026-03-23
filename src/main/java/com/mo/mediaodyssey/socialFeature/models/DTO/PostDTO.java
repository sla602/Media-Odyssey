package com.mo.mediaodyssey.socialFeature.models.DTO;

public class PostDTO {

    private Integer id;
    private Integer socialSpaceId;
    private String title;
    private String content;
    private String username;
    private boolean deleted;

    public PostDTO(Integer id, Integer socialSpaceId, String title, String content, String username, boolean deleted){
        this.id = id;
        this.socialSpaceId = socialSpaceId;
        this.title = title;
        this.content = content;
        this.username = username;
        this.deleted = deleted;
    }

    public Integer getId(){ return id; }
    public Integer getSocialSpaceId(){ return socialSpaceId; }
    public String getTitle(){ return title; }
    public String getContent(){ return content; }
    public String getUsername(){ return username; }

    public boolean isDeleted() {
        return deleted;
    }
}