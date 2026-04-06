package com.mo.mediaodyssey.socialFeature.models.DTO;

import com.mo.mediaodyssey.socialFeature.enums.RoleType;

public class SocialSpaceMemberDTO {

    private final Long userId;
    private final String username;
    private final RoleType roleType;
    private final String socialSpaceName;

    public SocialSpaceMemberDTO(Long userId, String username, RoleType roleType, String socialSpaceName) {
        this.userId = userId;
        this.username = username;
        this.roleType = roleType;
        this.socialSpaceName = socialSpaceName;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public RoleType getRoleType() { return roleType; }
    public String getSocialSpaceName() { return socialSpaceName; }
}