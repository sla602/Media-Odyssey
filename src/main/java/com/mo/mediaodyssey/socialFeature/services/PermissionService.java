package com.mo.mediaodyssey.socialFeature.services;

import com.mo.mediaodyssey.socialFeature.enums.Permission;
import com.mo.mediaodyssey.socialFeature.models.SocialSpaceRole;
import com.mo.mediaodyssey.socialFeature.repositories.SocialSpaceRoleRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PermissionService {

    private final SocialSpaceRoleRepository roleRepository;

    public PermissionService(SocialSpaceRoleRepository RoleRepository) {
        this.roleRepository = RoleRepository;
    }

    public void checkPermission(Integer userId, Integer socialSpaceId, Permission permission) {

        SocialSpaceRole role = roleRepository.findByUserIdAndSocialSpaceId(userId, socialSpaceId)
                .orElseThrow(() ->
                        new RuntimeException("User is not a member of this social space"));

        if (!role.getRoleType().hasPermission(permission)) {
            throw new RuntimeException("You do not have permission to perform this action");
        }
    }

    //members
    public void canCreatePost(Integer userId, Integer socialSpaceId) {
        checkPermission(userId, socialSpaceId, Permission.CREATE_POST);
    }

    public void canCreateComment(Integer userId, Integer socialSpaceId) {
        checkPermission(userId, socialSpaceId, Permission.CREATE_COMMENT);
    }


    public void canEditComment(Integer userId, Integer socialSpaceId) {
        checkPermission(userId, socialSpaceId, Permission.EDIT_COMMENT);
    }

    public void canDeleteComment(Integer userId, Integer socialSpaceId) {
        checkPermission(userId, socialSpaceId, Permission.DELETE_COMMENT);
    }

    public void canEditPost(Integer userId, Integer socialSpaceId) {
        checkPermission(userId, socialSpaceId, Permission.EDIT_POST);
    }


    // moderation

    public void canDeletePost(Integer userId, Integer socialSpaceId) {
        checkPermission(userId, socialSpaceId, Permission.DELETE_POST);
    }

    public void canKickMember(Integer userId, Integer socialSpaceId) {
        checkPermission(userId, socialSpaceId, Permission.KICK_MEMBER);
    }

    // owner management

    public void canPromoteMember(Integer userId, Integer socialSpaceId) {
        checkPermission(userId, socialSpaceId, Permission.PROMOTE_MEMBER);
    }

    public void canDemoteModerator(Integer userId, Integer socialSpaceId) {
        checkPermission(userId, socialSpaceId, Permission.DEMOTE_MODERATOR);
    }

    public void canTransferOwnership(Integer userId, Integer socialSpaceId) {
        checkPermission(userId, socialSpaceId, Permission.TRANSFER_OWNERSHIP);
    }


    //TODO: collapse with board
    public void canEditSocialSpace(Integer userId, Integer socialSpaceId) {
        checkPermission(userId, socialSpaceId, Permission.EDIT_COMMUNITY);
    }
    //TODO: collapse with board
    public void canDeleteSocialSpace(Integer userId, Integer socialSpaceId) {
        checkPermission(userId, socialSpaceId, Permission.DELETE_COMMUNITY);
    }

    public boolean hasPermission(Integer userId, Integer socialSpaceId, Permission permission) {

        Optional<SocialSpaceRole> roleOpt =
                roleRepository.findByUserIdAndSocialSpaceId(userId, socialSpaceId);

        if (roleOpt.isEmpty()) return false;

        return roleOpt.get().getRoleType().hasPermission(permission);
    }

    //TODO: collapse with board
    public boolean canModerateSocialSpace(Integer userId, Integer socialSpaceId) {
        return hasPermission(userId, socialSpaceId, Permission.KICK_MEMBER);
    }
}