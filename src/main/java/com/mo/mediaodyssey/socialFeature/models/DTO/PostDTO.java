package com.mo.mediaodyssey.socialFeature.models.DTO;

public class PostDTO {

    private Integer id;
    private Integer communityId;
    private String title;
    private String content;
    private String username;

    public PostDTO(Integer id, Integer communityId, String title, String content, String username){
        this.id = id;
        this.communityId = communityId;
        this.title = title;
        this.content = content;
        this.username = username;
    }

    public Integer getId(){ return id; }
    public Integer getCommunityId(){ return communityId; }
    public String getTitle(){ return title; }
    public String getContent(){ return content; }
    public String getUsername(){ return username; }
}