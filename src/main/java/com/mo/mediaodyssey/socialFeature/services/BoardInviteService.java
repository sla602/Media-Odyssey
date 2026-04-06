package com.mo.mediaodyssey.socialFeature.services;

import com.mo.mediaodyssey.layout.models.BoardRole;
import com.mo.mediaodyssey.layout.models.Boards;
import com.mo.mediaodyssey.layout.repositories.BoardRoleRepository;
import com.mo.mediaodyssey.layout.repositories.BoardsRepository;
import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.socialFeature.enums.RoleType;
import com.mo.mediaodyssey.socialFeature.models.BoardInvite;
import com.mo.mediaodyssey.socialFeature.repositories.BoardInviteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages invites from owner/moderators of private boards to their friends.
 *
 * Rules enforced here:
 *  - Only owner/moderator of the board can send invites.
 *  - Invitee must be a friend of the inviter.
 *  - Invitee cannot already be a member / already invited / BANNED from the board.
 *  - Accepting an invite creates a MEMBER role and deletes the invite row.
 */
@Service
@Transactional
public class BoardInviteService {

    private final BoardInviteRepository inviteRepo;
    private final BoardRoleRepository roleRepo;
    private final BoardsRepository boardsRepo;
    private final FriendshipService friendshipService;

    public BoardInviteService(BoardInviteRepository inviteRepo,
                              BoardRoleRepository roleRepo,
                              BoardsRepository boardsRepo,
                              FriendshipService friendshipService) {
        this.inviteRepo = inviteRepo;
        this.roleRepo = roleRepo;
        this.boardsRepo = boardsRepo;
        this.friendshipService = friendshipService;
    }

    // ─── Send ────────────────────────────────────────────────────────

    /**
     * Send a board invite. Validates every rule above and throws
     * IllegalStateException with a user-facing message on any failure.
     */
    public void sendInvite(Long inviterUserId, Long inviteeUserId, Long boardId) {
        if (inviterUserId.equals(inviteeUserId)) {
            throw new IllegalStateException("You cannot invite yourself.");
        }

        // 1. Board must exist
        Boards board = boardsRepo.findById(boardId)
                .orElseThrow(() -> new IllegalStateException("Board not found."));

        // 2. Inviter must be owner or moderator
        RoleType inviterRole = roleRepo.findByUserIdAndBoardId(inviterUserId, boardId)
                .map(BoardRole::getRoleType)
                .orElse(RoleType.NONE);
        if (!inviterRole.isOwner() && !inviterRole.isModerator()) {
            throw new IllegalStateException("Only the owner or a moderator can send invites.");
        }

        // 3. Invitee must be a friend of the inviter
        List<User> friends = friendshipService.getFriends(inviterUserId);
        boolean isFriend = friends.stream().anyMatch(f -> f.getId().equals(inviteeUserId));
        if (!isFriend) {
            throw new IllegalStateException("You can only invite your friends.");
        }

        // 4. Invitee cannot already be in the board, including BANNED
        Optional<BoardRole> existingRole = roleRepo.findByUserIdAndBoardId(inviteeUserId, boardId);
        if (existingRole.isPresent()) {
            RoleType existing = existingRole.get().getRoleType();
            if (existing.isBanned()) {
                throw new IllegalStateException("This user is banned from the board.");
            }
            if (existing.isOwner() || existing.isModerator() || existing == RoleType.MEMBER) {
                throw new IllegalStateException("This user is already in the board.");
            }
            // If they previously LEFT, they're eligible for re-invite — fall through.
        }

        // 5. No duplicate pending invite
        if (inviteRepo.existsByBoardIdAndInviteeUserId(boardId, inviteeUserId)) {
            throw new IllegalStateException("This user already has a pending invite to the board.");
        }

        inviteRepo.save(new BoardInvite(boardId, inviterUserId, inviteeUserId));
    }

    // ─── Accept / Decline / Cancel ───────────────────────────────────

    /**
     * Accept an invite: delete the invite row and create/reactivate a MEMBER role.
     * The acting user must be the invitee.
     */
    public void acceptInvite(Long inviteId, Long actingUserId) {
        BoardInvite invite = inviteRepo.findById(inviteId)
                .orElseThrow(() -> new IllegalStateException("Invite not found."));

        if (!invite.getInviteeUserId().equals(actingUserId)) {
            throw new IllegalStateException("This invite is not for you.");
        }

        Long boardId = invite.getBoardId();

        // Handle the three cases: no existing role, previously LEFT, or somehow already in.
        Optional<BoardRole> existing = roleRepo.findByUserIdAndBoardId(actingUserId, boardId);
        if (existing.isPresent()) {
            BoardRole role = existing.get();
            RoleType type = role.getRoleType();
            if (type.isBanned()) {
                // Shouldn't happen — sendInvite blocks this — but guard anyway.
                inviteRepo.delete(invite);
                throw new IllegalStateException("You are banned from this board.");
            }
            if (type == RoleType.LEFT) {
                role.setRoleType(RoleType.MEMBER);
                roleRepo.save(role);
            }
            // If already MEMBER/MOD/OWNER we just drop the invite silently.
        } else {
            roleRepo.save(new BoardRole(actingUserId, boardId, RoleType.MEMBER));
        }

        inviteRepo.delete(invite);
    }

    /**
     * Decline an invite. The acting user must be the invitee.
     */
    public void declineInvite(Long inviteId, Long actingUserId) {
        BoardInvite invite = inviteRepo.findById(inviteId)
                .orElseThrow(() -> new IllegalStateException("Invite not found."));
        if (!invite.getInviteeUserId().equals(actingUserId)) {
            throw new IllegalStateException("This invite is not for you.");
        }
        inviteRepo.delete(invite);
    }

    /**
     * Cancel an invite you sent. The acting user must be the original inviter
     * (or an owner/mod of the board — handy if a mod wants to clean up).
     */
    public void cancelInvite(Long inviteId, Long actingUserId) {
        BoardInvite invite = inviteRepo.findById(inviteId)
                .orElseThrow(() -> new IllegalStateException("Invite not found."));

        boolean isInviter = invite.getInviterUserId().equals(actingUserId);
        RoleType actorRole = roleRepo.findByUserIdAndBoardId(actingUserId, invite.getBoardId())
                .map(BoardRole::getRoleType)
                .orElse(RoleType.NONE);
        boolean isBoardStaff = actorRole.isOwner() || actorRole.isModerator();

        if (!isInviter && !isBoardStaff) {
            throw new IllegalStateException("You cannot cancel this invite.");
        }
        inviteRepo.delete(invite);
    }

    // ─── Queries ─────────────────────────────────────────────────────

    /** All pending invites addressed to the given user. */
    public List<BoardInvite> getInvitesForUser(Long userId) {
        return inviteRepo.findByInviteeUserId(userId);
    }

    /** All pending invites for a given board (useful for the board settings page). */
    public List<BoardInvite> getInvitesForBoard(Long boardId) {
        return inviteRepo.findByBoardId(boardId);
    }

    /**
     * Returns the set of friend ids that are eligible to be invited to the
     * given board right now — i.e. friends of the inviter who are not already
     * in the board, not banned, and don't already have a pending invite.
     */
    public List<User> getInvitableFriends(Long inviterUserId, Long boardId) {
        List<User> friends = friendshipService.getFriends(inviterUserId);
        List<User> invitable = new ArrayList<>();

        for (User friend : friends) {
            Long friendId = friend.getId();

            // Already in board (any state except LEFT)
            Optional<BoardRole> existingRole = roleRepo.findByUserIdAndBoardId(friendId, boardId);
            if (existingRole.isPresent()) {
                RoleType type = existingRole.get().getRoleType();
                if (type.isBanned() || type.isOwner() || type.isModerator() || type == RoleType.MEMBER) {
                    continue;
                }
            }

            // Already has a pending invite
            if (inviteRepo.existsByBoardIdAndInviteeUserId(boardId, friendId)) {
                continue;
            }

            invitable.add(friend);
        }
        return invitable;
    }
}