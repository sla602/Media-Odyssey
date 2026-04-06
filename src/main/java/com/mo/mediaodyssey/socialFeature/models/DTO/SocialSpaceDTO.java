package com.mo.mediaodyssey.socialFeature.models.DTO;

public class SocialSpaceDTO {

    private final Integer socialSpaceId;
    private final String socialSpaceName;
    private final String description;

    public SocialSpaceDTO(Integer socialSpaceId, String socialSpaceName) {
        this(socialSpaceId, socialSpaceName, null);
    }

    public SocialSpaceDTO(Integer socialSpaceId, String socialSpaceName, String description) {
        this.socialSpaceId = socialSpaceId;
        this.socialSpaceName = socialSpaceName;
        this.description = description;
    }

    public Integer getSocialSpaceId() { return socialSpaceId; }
    public String getSocialSpaceName() { return socialSpaceName; }
    public String getDescription() { return description; }
}