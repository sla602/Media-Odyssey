package com.mo.mediaodyssey.models;


import com.mo.mediaodyssey.enums.RoleType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "community_role",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_community",
                        columnNames = {"user_id", "community_id"}
                )
        },
        indexes = {
                @Index(name="idx_role_user", columnList = "user_id"),
                @Index(name = "idx_role_community", columnList = "community_id")
        }
)
public class CommunityRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;



    @Column(name = "user_id", nullable = false)
    private Integer userId;


    @Column(name = "community_id", nullable = false)
    private Integer communityId;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleType roleType;

    @Column(nullable = false, updatable = false)
    private Instant assignedAt = Instant.now();


    protected  CommunityRole(){}

    public CommunityRole(Integer userId, Integer communityId, RoleType role){
        this.userId = userId;
        this.communityId = communityId;
        this.roleType = role;
    }

    public Integer getUserId() {
        return userId;
    }

    public Integer getCommunityId() {
        return communityId;
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
