package com.mo.mediaodyssey.socialFeature.models.DTO;


public class CommunityDTO {

    private final Integer communityId;
    private final String communityName;

    public CommunityDTO(Integer communityId, String communityName) {
        this.communityId = communityId;
        this.communityName = communityName;
    }

    public Integer getCommunityId() {
        return communityId;
    }

    public String getCommunityName() {
        return communityName;
    }
}