package com.mo.mediaodyssey.socialFeature.models.DTO;

import com.mo.mediaodyssey.shared.model.User;

/**
 * Enriched friend suggestion carrying the signals that led to it.
 * A single user can be suggested via multiple signals (e.g. shares a
 * board AND likes the same movies) — all matching flags are set true.
 *
 * Used by the toggle filters on friends.html so the template can
 * show/hide entries by signal without extra requests.
 */
public class SuggestedFriend {
    private final User user;
    private final boolean fromSharedBoard;
    private final boolean fromSharedMovie;
    private final boolean fromSharedGame;
    private final boolean fromSharedSong;
    private final int overlapCount;

    public SuggestedFriend(User user,
                           boolean fromSharedBoard,
                           boolean fromSharedMovie,
                           boolean fromSharedGame,
                           boolean fromSharedSong,
                           int overlapCount) {
        this.user = user;
        this.fromSharedBoard = fromSharedBoard;
        this.fromSharedMovie = fromSharedMovie;
        this.fromSharedGame = fromSharedGame;
        this.fromSharedSong = fromSharedSong;
        this.overlapCount = overlapCount;
    }

    public User getUser()              { return user; }
    public Long getUserId()            { return user.getId(); }
    public boolean isFromSharedBoard() { return fromSharedBoard; }
    public boolean isFromSharedMovie() { return fromSharedMovie; }
    public boolean isFromSharedGame()  { return fromSharedGame; }
    public boolean isFromSharedSong()  { return fromSharedSong; }
    public int getOverlapCount()       { return overlapCount; }
}