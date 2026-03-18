package com.mo.mediaodyssey.socialFeature.services;

import com.mo.mediaodyssey.socialFeature.enums.Permission;
import com.mo.mediaodyssey.socialFeature.models.CommunityRole;
import com.mo.mediaodyssey.socialFeature.repositories.CommunityRoleRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PermissionService {

    private final CommunityRoleRepository communityRoleRepository;

    public PermissionService(CommunityRoleRepository communityRoleRepository) {
        this.communityRoleRepository = communityRoleRepository;
    }

    public void checkPermission(Integer userId, Integer communityId, Permission permission) {

        CommunityRole role = communityRoleRepository
                .findByUserIdAndCommunityId(userId, communityId)
                .orElseThrow(() ->
                        new RuntimeException("User is not a member of this community"));

        if (!role.getRoleType().hasPermission(permission)) {
            throw new RuntimeException("You do not have permission to perform this action");
        }
    }

    //members
    public void canCreatePost(Integer userId, Integer communityId) {
        checkPermission(userId, communityId, Permission.CREATE_POST);
    }

    public void canCreateComment(Integer userId, Integer communityId) {
        checkPermission(userId, communityId, Permission.CREATE_COMMENT);
    }


    public void canEditComment(Integer userId, Integer communityId) {
        checkPermission(userId, communityId, Permission.EDIT_COMMENT);
    }

    public void canDeleteComment(Integer userId, Integer communityId) {
        checkPermission(userId, communityId, Permission.DELETE_COMMENT);
    }

    public void canEditPost(Integer userId, Integer communityId) {
        checkPermission(userId, communityId, Permission.EDIT_POST);
    }


    // moderation

    public void canDeletePost(Integer userId, Integer communityId) {
        checkPermission(userId, communityId, Permission.DELETE_POST);
    }

    public void canKickMember(Integer userId, Integer communityId) {
        checkPermission(userId, communityId, Permission.KICK_MEMBER);
    }

    // owner management

    public void canPromoteMember(Integer userId, Integer communityId) {
        checkPermission(userId, communityId, Permission.PROMOTE_MEMBER);
    }

    public void canDemoteModerator(Integer userId, Integer communityId) {
        checkPermission(userId, communityId, Permission.DEMOTE_MODERATOR);
    }

    public void canTransferOwnership(Integer userId, Integer communityId) {
        checkPermission(userId, communityId, Permission.TRANSFER_OWNERSHIP);
    }

    public void canEditCommunity(Integer userId, Integer communityId) {
        checkPermission(userId, communityId, Permission.EDIT_COMMUNITY);
    }

    public void canDeleteCommunity(Integer userId, Integer communityId) {
        checkPermission(userId, communityId, Permission.DELETE_COMMUNITY);
    }

    public boolean hasPermission(Integer userId, Integer communityId, Permission permission) {

        Optional<CommunityRole> roleOpt =
                communityRoleRepository.findByUserIdAndCommunityId(userId, communityId);

        if (roleOpt.isEmpty()) return false;

        return roleOpt.get().getRoleType().hasPermission(permission);
    }

    public boolean canModerateCommunity(Integer userId, Integer communityId) {
        return hasPermission(userId, communityId, Permission.KICK_MEMBER);
    }
}