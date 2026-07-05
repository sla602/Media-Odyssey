package com.mo.mediaodyssey.shared.util;

/**
 * Escapes user input so it can be safely embedded in a SQL/JPQL LIKE pattern.
 *
 * The caller must pair this with an ESCAPE clause in the query.
 */
public final class SqlLikeEscaper {

    /**
     * Escapes LIKE metacharacters in the provided input.
     *
     * @param input raw user input
     * @return input with backslash, percent, and underscore escaped
     */
    public static String escape(String input) {
        if (input == null) {
            return null;
        }
        return input
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
