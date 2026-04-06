package com.mo.mediaodyssey.layout.models;

import com.mo.mediaodyssey.socialFeature.enums.RoleType;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Replaces SocialSpaceRole.
 * Tracks which user has which role in which board.
 */
@Entity
@Table(
        name = "board_role",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_board",
                        columnNames = {"user_id", "board_id"}
                )
        },
        indexes = {
                @Index(name = "idx_role_user", columnList = "user_id"),
                @Index(name = "idx_role_board", columnList = "board_id")
        }
)
public class BoardRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleType roleType;

    @Column(nullable = false, updatable = false)
    private Instant assignedAt = Instant.now();

    protected BoardRole() {}

    public BoardRole(Long userId, Long boardId, RoleType role) {
        this.userId = userId;
        this.boardId = boardId;
        this.roleType = role;
    }

    public Long getId()              { return id; }
    public Long getUserId()       { return userId; }
    public Long getBoardId()         { return boardId; }
    public RoleType getRoleType()    { return roleType; }
    public Instant getAssignedAt()   { return assignedAt; }

    public void setRoleType(RoleType roleType) {
        this.roleType = roleType;
    }
}