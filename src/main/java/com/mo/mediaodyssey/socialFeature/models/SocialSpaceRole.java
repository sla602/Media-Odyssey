package com.mo.mediaodyssey.socialFeature.models;


import com.mo.mediaodyssey.socialFeature.enums.RoleType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "social_space_role",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_social_space",
                        columnNames = {"user_id", "social_space_id"}
                )
        },
        indexes = {
                @Index(name="idx_role_user", columnList = "user_id"),
                @Index(name = "idx_role_social_space", columnList = "social_space_id")
        }
)
public class SocialSpaceRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;



    @Column(name = "user_id", nullable = false)
    private Integer userId;


    @Column(name = "social_space_id", nullable = false)
    private Integer socialSpaceId;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleType roleType;

    @Column(nullable = false, updatable = false)
    private Instant assignedAt = Instant.now();


    protected SocialSpaceRole(){}

    public SocialSpaceRole(Integer userId, Integer socialSpaceId, RoleType role){
        this.userId = userId;
        this.socialSpaceId = socialSpaceId;
        this.roleType = role;
    }

    public Integer getUserId() {
        return userId;
    }

    public Integer getSocialSpaceId() {
        return socialSpaceId;
    }

    public RoleType getRoleType() {
        return roleType;
    }

    public void setRoleType(RoleType roleType) {
        this.roleType = roleType;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void changeRole(RoleType newRole){
        this.roleType = newRole;
    }
}
