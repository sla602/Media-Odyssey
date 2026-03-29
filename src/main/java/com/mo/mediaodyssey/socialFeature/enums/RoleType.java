package com.mo.mediaodyssey.socialFeature.enums;

import java.util.Set;

public enum RoleType {

    NONE(Set.of()), //No Permissions

    MEMBER(Set.of(
            Permission.CREATE_POST,
            Permission.CREATE_COMMENT,
            Permission.EDIT_COMMENT,
            Permission.DELETE_COMMENT
    )),

    MODERATOR(Set.of(
            Permission.CREATE_POST,
            Permission.CREATE_COMMENT,
            Permission.EDIT_COMMENT,
            Permission.DELETE_COMMENT,
            Permission.DELETE_POST,
            Permission.KICK_MEMBER
    )),

    OWNER(Set.of(
            Permission.CREATE_POST,
            Permission.CREATE_COMMENT,
            Permission.DELETE_POST,
            Permission.KICK_MEMBER,
            Permission.PROMOTE_MEMBER,
            Permission.DEMOTE_MODERATOR,
            Permission.TRANSFER_OWNERSHIP,
            Permission.EDIT_COMMUNITY,
            Permission.DELETE_COMMUNITY
    ));

    private final Set<Permission> permissions;

    RoleType(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public boolean isOwner() {
        return this == OWNER;
    }

    public boolean isModerator() {
        return this == MODERATOR;
    }

    public boolean isMember() {
        return this == MEMBER;
    }

    public boolean isNone()      { return this == NONE; }

    public boolean isStaff() {
        return this == MODERATOR || this == OWNER;
    }

}