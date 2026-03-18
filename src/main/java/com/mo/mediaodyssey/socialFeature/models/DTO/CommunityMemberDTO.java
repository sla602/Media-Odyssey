package com.mo.mediaodyssey.socialFeature.models.DTO;

import com.mo.mediaodyssey.socialFeature.enums.RoleType;

public class CommunityMemberDTO {

    private final Long userId;
    private final String username;
    private final RoleType roleType;
    private final String communityName;

    public CommunityMemberDTO(Long userId, String username, RoleType roleType, String communityName) {
        this.userId = userId;
        this.username = username;
        this.roleType = roleType;
        this.communityName = communityName;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public RoleType getRoleType() { return roleType; }
    public String getCommunityName() { return communityName; }
}