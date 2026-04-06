package com.mo.mediaodyssey.socialFeature.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A pending invitation from an owner/moderator of a private board
 * to a friend who isn't yet in the board.
 *
 * Lifecycle:
 *   - created when an owner/mod sends an invite
 *   - deleted when the invitee accepts (a BoardRole row is created at the same time)
 *   - deleted when the invitee declines
 *   - deleted when the inviter cancels
 *
 * There's a uniqueness constraint on (boardId, inviteeUserId) — a user
 * can only have one pending invite per board at a time.
 */
@Entity
@Table(name = "board_invites",
        uniqueConstraints = @UniqueConstraint(columnNames = {"board_id", "invitee_user_id"}))
public class BoardInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    /** The user who sent the invite — owner or moderator of the board. */
    @Column(name = "inviter_user_id", nullable = false)
    private Long inviterUserId;

    /** The user being invited. */
    @Column(name = "invitee_user_id", nullable = false)
    private Long inviteeUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public BoardInvite() {
        this.createdAt = LocalDateTime.now();
    }

    public BoardInvite(Long boardId, Long inviterUserId, Long inviteeUserId) {
        this.boardId = boardId;
        this.inviterUserId = inviterUserId;
        this.inviteeUserId = inviteeUserId;
        this.createdAt = LocalDateTime.now();
    }

    // Getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBoardId() { return boardId; }
    public void setBoardId(Long boardId) { this.boardId = boardId; }

    public Long getInviterUserId() { return inviterUserId; }
    public void setInviterUserId(Long inviterUserId) { this.inviterUserId = inviterUserId; }

    public Long getInviteeUserId() { return inviteeUserId; }
    public void setInviteeUserId(Long inviteeUserId) { this.inviteeUserId = inviteeUserId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}