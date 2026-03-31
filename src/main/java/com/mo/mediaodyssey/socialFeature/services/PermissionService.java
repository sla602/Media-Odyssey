package com.mo.mediaodyssey.socialFeature.services;

import com.mo.mediaodyssey.socialFeature.enums.ContentAction;
import com.mo.mediaodyssey.socialFeature.enums.Permission;
import com.mo.mediaodyssey.socialFeature.enums.RoleType;
import com.mo.mediaodyssey.layout.models.BoardRole;
import com.mo.mediaodyssey.layout.repositories.BoardRoleRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PermissionService {

    private final BoardRoleRepository roleRepository;

    public PermissionService(BoardRoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    // permission check

    public void checkPermission(Long userId, Long boardId, Permission permission) {
        BoardRole role = roleRepository.findByUserIdAndBoardId(userId, boardId)
                .orElseThrow(() ->
                        new RuntimeException("User is not a member of this board"));

        if (!role.getRoleType().hasPermission(permission)) {
            throw new RuntimeException("You do not have permission to perform this action");
        }
    }

    public boolean hasPermission(Long userId, Long boardId, Permission permission) {
        return roleRepository.findByUserIdAndBoardId(userId, boardId)
                .map(role -> role.getRoleType().hasPermission(permission))
                .orElse(false);
    }

    // checks

    public void canCreatePost(Long userId, Long boardId) {
        checkPermission(userId, boardId, Permission.CREATE_POST);
    }

    public void canCreateComment(Long userId, Long boardId) {
        checkPermission(userId, boardId, Permission.CREATE_COMMENT);
    }

    public void canEditPost(Long userId, Long boardId) {
        checkPermission(userId, boardId, Permission.EDIT_POST);
    }

    public void canEditComment(Long userId, Long boardId) {
        checkPermission(userId, boardId, Permission.EDIT_COMMENT);
    }

    public void canDeletePost(Long userId, Long boardId) {
        checkPermission(userId, boardId, Permission.DELETE_POST);
    }

    public void canDeleteComment(Long userId, Long boardId) {
        checkPermission(userId, boardId, Permission.DELETE_COMMENT);
    }

    public void canKickMember(Long userId, Long boardId) {
        checkPermission(userId, boardId, Permission.KICK_MEMBER);
    }

    public void canPromoteMember(Long userId, Long boardId) {
        checkPermission(userId, boardId, Permission.PROMOTE_MEMBER);
    }

    public void canDemoteModerator(Long userId, Long boardId) {
        checkPermission(userId, boardId, Permission.DEMOTE_MODERATOR);
    }

    public void canTransferOwnership(Long userId, Long boardId) {
        checkPermission(userId, boardId, Permission.TRANSFER_OWNERSHIP);
    }

    public void canEditBoard(Long userId, Long boardId) {
        checkPermission(userId, boardId, Permission.EDIT_BOARD);
    }

    public void canDeleteBoard(Long userId, Long boardId) {
        checkPermission(userId, boardId, Permission.DELETE_BOARD);
    }

    // ─── Membership checks ──────────────────────────────────────────

    public boolean isUserInBoard(Long userId, Long boardId) {
        return roleRepository.existsByUserIdAndBoardId(userId, boardId);
    }

    public boolean canModerateBoard(Long userId, Long boardId) {
        return hasPermission(userId, boardId, Permission.KICK_MEMBER);
    }

    // ─── Content action resolution ──────────────────────────────────
    //
    // Returns the set of actions the viewing user can take on a piece
    // of content (post or comment) authored by authorId inside boardId.
    //
    //  OP viewing own content  → EDIT, DELETE, REPLY  (no REPORT)
    //  Mod/Owner viewing other → DELETE, REPLY, REPORT (no EDIT)
    //  Regular member viewing  → REPLY, REPORT         (no EDIT, no DELETE)
    //

    public Set<ContentAction> getContentActions(Long viewerId, Long authorId, Long boardId) {

        RoleType viewerRole = roleRepository.findByUserIdAndBoardId(viewerId, boardId)
                .map(BoardRole::getRoleType)
                .orElse(RoleType.NONE);

        if (viewerRole == RoleType.NONE) {
            return Collections.emptySet();   // not a member, no actions
        }

        Set<ContentAction> actions = EnumSet.noneOf(ContentAction.class);
        boolean isAuthor = viewerId.equals(authorId);

        if (isAuthor) {
            // OP viewing their own content
            actions.add(ContentAction.EDIT);
            actions.add(ContentAction.DELETE);
            actions.add(ContentAction.REPLY);
            // cannot report yourself
        } else if (viewerRole.isStaff()) {
            // Moderator or Owner viewing someone else's content
            actions.add(ContentAction.DELETE);
            actions.add(ContentAction.REPLY);
            actions.add(ContentAction.REPORT);
            // cannot edit another user's content
        } else {
            // Regular member viewing someone else's content
            actions.add(ContentAction.REPLY);
            actions.add(ContentAction.REPORT);
        }

        return actions;
    }
}