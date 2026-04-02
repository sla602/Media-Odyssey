package com.mo.mediaodyssey.socialFeature.enums;

import java.util.Set;

public enum RoleType {

    NONE(Set.of()), // No permissions

    BANNED(Set.of()), // ABSOLUTELY NO PERMISSIONS

    MEMBER(Set.of(
            Permission.CREATE_POST,
            Permission.EDIT_POST,       // can edit own post
            Permission.CREATE_COMMENT,
            Permission.EDIT_COMMENT,    // can edit own comment
            Permission.DELETE_COMMENT,  // can delete own comment
            Permission.REPORT           // can report others' content
    )),

    MODERATOR(Set.of(
            Permission.CREATE_POST,
            Permission.EDIT_POST,
            Permission.CREATE_COMMENT,
            Permission.EDIT_COMMENT,
            Permission.DELETE_COMMENT,
            Permission.DELETE_POST,     // can delete Members post
            Permission.KICK_MEMBER,
            Permission.BAN_MEMBER,
            Permission.REPORT
    )),

    OWNER(Set.of(
            Permission.CREATE_POST,
            Permission.EDIT_POST,
            Permission.CREATE_COMMENT,
            Permission.EDIT_COMMENT,
            Permission.DELETE_COMMENT,
            Permission.DELETE_POST,
            Permission.KICK_MEMBER,
            Permission.BAN_MEMBER,
            Permission.PROMOTE_MEMBER,
            Permission.DEMOTE_MODERATOR,
            Permission.TRANSFER_OWNERSHIP,
            Permission.EDIT_BOARD,
            Permission.DELETE_BOARD
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

    public boolean isOwner()     { return this == OWNER; }
    public boolean isModerator() { return this == MODERATOR; }
    public boolean isMember()    { return this == MEMBER; }
    public boolean isNone()      { return this == NONE; }
    public boolean isBanned()    { return this == BANNED; }
    public boolean isStaff() {
        return this == MODERATOR || this == OWNER;
    }
}