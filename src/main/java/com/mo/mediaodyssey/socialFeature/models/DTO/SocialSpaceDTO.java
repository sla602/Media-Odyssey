package com.mo.mediaodyssey.socialFeature.models.DTO;


public class SocialSpaceDTO {

    private final Integer socialSpaceId;
    private final String socialSpaceName;

    public SocialSpaceDTO(Integer socialSpaceId, String socialSpaceName) {
        this.socialSpaceId = socialSpaceId;
        this.socialSpaceName = socialSpaceName;
    }

    public Integer getSocialSpaceId() {
        return socialSpaceId;
    }

    public String getSocialSpaceName() {
        return socialSpaceName;
    }
}