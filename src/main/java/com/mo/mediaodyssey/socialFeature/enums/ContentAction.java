package com.mo.mediaodyssey.socialFeature.enums;

/**
 * Actions a viewer can take on a specific post or comment.
 * Used by PermissionService to build per-content action sets
 * based on the viewer's role and ownership of the content.
 */
public enum ContentAction {
    EDIT,
    DELETE,
    REPLY,
    REPORT
}