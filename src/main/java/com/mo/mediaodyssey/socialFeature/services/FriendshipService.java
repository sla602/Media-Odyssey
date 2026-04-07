package com.mo.mediaodyssey.socialFeature.services;

import com.mo.mediaodyssey.layout.models.Profile;
import com.mo.mediaodyssey.recommendation.UserInteractionRepository;
import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.auth.repository.UserRepository;
import com.mo.mediaodyssey.layout.models.BoardRole;
import com.mo.mediaodyssey.layout.repositories.BoardRoleRepository;
import com.mo.mediaodyssey.socialFeature.models.DTO.FriendRequestDTO;
import com.mo.mediaodyssey.socialFeature.models.Friendship;
import com.mo.mediaodyssey.socialFeature.repositories.FriendshipRepository;
import com.mo.mediaodyssey.socialFeature.repositories.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages friendships between users.
 *
 *
 *  - Friend suggestions are now drawn from users who share at least one
 *    Board with the current user (via BoardRoleRepository).
 *
 */
@Service
@Transactional
public class FriendshipService {

    private final FriendshipRepository friendshipRepo;
    private final BoardRoleRepository boardRoleRepo;
    private final UserRepository userRepo;
    private final ProfileService profileService;
    private final ProfileRepository profileRepo;
    private final UserInteractionRepository interactionRepo;

    public FriendshipService(FriendshipRepository friendshipRepo,
                             BoardRoleRepository boardRoleRepo,
                             UserRepository userRepo, ProfileService profileService, ProfileRepository profileRepo, UserInteractionRepository interactionRepo) {
        this.friendshipRepo = friendshipRepo;
        this.boardRoleRepo = boardRoleRepo;
        this.userRepo = userRepo;
        this.profileService = profileService;
        this.profileRepo = profileRepo;
        this.interactionRepo = interactionRepo;
    }

    /**
     * Enriched friend suggestion carrying the signals that led to it.
     * A single user can be suggested via multiple signals (e.g. shares a
     * board AND likes the same movies) — all matching flags are set true.
     *
     * Used by the toggle filters on friends.html so the template can
     * show/hide entries by signal without extra requests.
     */
    public static class SuggestedFriend {
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

        public User getUser() {
            return user;
        }

        public Long getUserId() {
            return user.getId();
        }

        public boolean isFromSharedBoard() {
            return fromSharedBoard;
        }

        public boolean isFromSharedMovie() {
            return fromSharedMovie;
        }

        public boolean isFromSharedGame() {
            return fromSharedGame;
        }

        public boolean isFromSharedSong() {
            return fromSharedSong;
        }

        public int getOverlapCount() {
            return overlapCount;
        }

    }
        /**
     * A single search result for the friend search bar on friends.html.
     * Pairs a user id / username with the viewer's current friend status
     * toward that user, so the template can render the right action button.
     */
    public static class FriendSearchResult {
        private final Long userId;
        private final String username;
        private final FriendStatus status;
        private final Long requestId;   // non-null only if there is a pending request

        public FriendSearchResult(Long userId, String username,
                                  FriendStatus status, Long requestId) {
            this.userId = userId;
            this.username = username;
            this.status = status;
            this.requestId = requestId;
        }

        public Long getUserId()     { return userId; }
        public String getUsername() { return username; }
        public FriendStatus getStatus() { return status; }
        public String getStatusName()   { return status.name(); }
        public Long getRequestId()  { return requestId; }
    }

    /**
     * Search for users by username and annotate each with the viewer's
     * current friend status. Used by the search bar on friends.html.
     *
     * Returns an empty list if the query is blank.
     */
    public List<FriendSearchResult> searchUsersByUsername(Long viewerId, String query) {
        if (query == null || query.isBlank()) return List.of();

        List<Profile> matches = profileRepo.searchByUsername(query.trim(), viewerId);

        List<FriendSearchResult> results = new ArrayList<>();
        for (Profile p : matches) {
            Long otherId = p.getUserId();
            FriendStatus status = getStatusBetween(viewerId, otherId);

            // If there's a pending request either direction, look up its id
            // so the template can POST to /accept or /cancel directly.
            Long requestId = null;
            if (status == FriendStatus.REQUEST_SENT || status == FriendStatus.REQUEST_RECEIVED) {
                requestId = friendshipRepo.findBetween(viewerId, otherId)
                        .map(f -> f.getId())
                        .orElse(null);
            }

            results.add(new FriendSearchResult(otherId, p.getUsername(), status, requestId));
        }
        return results;
    }
    /**
     * Returns null if the user has a username, or a redirect string if they don't.
     * Used as a guard at the top of all gated endpoints.
     */
    private String requireUsername(Long userId, RedirectAttributes redirectAttributes) {
        if (!profileService.hasUsername(userId)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "You need to set a username before using the friends page.");
            return "redirect:/profile";
        }
        return null;
    }
    // Requests

    public void sendFriendRequest(Long fromUserId, Long toUserId) {
        if (fromUserId.equals(toUserId)) {
            throw new IllegalStateException("You cannot send a friend request to yourself");
        }
        if (friendshipRepo.existsByUserIdAndFriendId(fromUserId, toUserId) ||
                friendshipRepo.existsByFriendIdAndUserId(toUserId, fromUserId)) {
            throw new IllegalStateException("Friend request already exists");
        }
        Friendship request = new Friendship(fromUserId, toUserId);
        friendshipRepo.save(request);
    }

    public void acceptFriendRequest(Long requestId) {
        Friendship request = friendshipRepo.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("Friend request not found"));
        request.setAccepted(true);
        friendshipRepo.save(request);
    }

    /**
     * Reject an incoming request OR cancel an outgoing request.
     * Both end up deleting the friendship row.
     */
    public void cancelFriendRequest(Long requestId) {
        Friendship request = friendshipRepo.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("Friend request not found"));
        if (request.isAccepted()) {
            throw new IllegalStateException("Cannot cancel an already-accepted friendship");
        }
        friendshipRepo.delete(request);
    }

    /** Unfriend — removes an accepted friendship in either direction. */
    public void removeFriend(Long userId, Long otherUserId) {
        Friendship f = friendshipRepo.findBetween(userId, otherUserId)
                .orElseThrow(() -> new IllegalStateException("Friendship not found"));
        friendshipRepo.delete(f);
    }

    // Queries

    public List<User> getFriends(Long userId) {
        List<Friendship> friendships = friendshipRepo.findAcceptedFriendshipsForUser(userId);

        return friendships.stream()
                .map(f -> {
                    Long otherId = f.getUserId().equals(userId) ? f.getFriendId() : f.getUserId();
                    return userRepo.findById(otherId).orElse(null);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<FriendRequestDTO> getIncomingRequests(Long userId) {
        return friendshipRepo.findIncomingRequests(userId);
    }

    public List<FriendRequestDTO> getOutgoingRequests(Long userId) {
        return friendshipRepo.findOutgoingRequests(userId);
    }
    /**
     * Suggest friends using two signals:
     *   1. Users who share at least one Board with the viewer.
     *   2. Users who have LIKED the same media as the viewer, broken
     *      down by media type (MOVIE / GAME / SONG).
     *
     * A single candidate can match multiple signals — the returned
     * SuggestedFriend carries a flag for each so the UI can filter.
     *
     * Excludes: the viewer themselves, existing friends, and anyone
     * with a pending request in either direction.
     */
    public List<SuggestedFriend> getSuggestedFriendsMedia(Long userId) {

        Map<Long, SuggestionAccumulator> candidates = new LinkedHashMap<>();

        // ─── Signal 1: shared boards ─────────────────────────────
        List<BoardRole> myRoles = boardRoleRepo.findActiveByUserId(userId);
        List<Long> myBoardIds = myRoles.stream()
                .map(BoardRole::getBoardId)
                .collect(Collectors.toList());

        for (Long boardId : myBoardIds) {
            List<BoardRole> rolesInBoard = boardRoleRepo.findMembersByBoardId(boardId);
            for (BoardRole r : rolesInBoard) {
                Long otherId = r.getUserId();
                if (otherId.equals(userId)) continue;
                candidates.computeIfAbsent(otherId, k -> new SuggestionAccumulator())
                        .sharedBoard = true;
            }
        }

        // ─── Signal 2: taste overlap per media type ──────────────
        mergeOverlap(candidates, userId, "MOVIE");
        mergeOverlap(candidates, userId, "GAME");
        mergeOverlap(candidates, userId, "SONG");

        // ─── Filter out self, existing friendships, pending reqs ─
        List<SuggestedFriend> results = new ArrayList<>();
        for (Map.Entry<Long, SuggestionAccumulator> entry : candidates.entrySet()) {
            Long candidateId = entry.getKey();
            SuggestionAccumulator acc = entry.getValue();

            boolean hasRelation =
                    friendshipRepo.existsByUserIdAndFriendId(userId, candidateId) ||
                            friendshipRepo.existsByFriendIdAndUserId(userId, candidateId) ||
                            friendshipRepo.existsByUserIdAndFriendId(candidateId, userId) ||
                            friendshipRepo.existsByFriendIdAndUserId(candidateId, userId);
            if (hasRelation) continue;
            // Skip anyone who hasn't set a username yet. They can't be meaningfully
            // displayed or interacted with until they complete their profile.
            if (!profileService.hasUsername(candidateId)) continue;

            userRepo.findById(candidateId).ifPresent(u ->
                    results.add(new SuggestedFriend(
                            u,
                            acc.sharedBoard,
                            acc.movieOverlap > 0,
                            acc.gameOverlap > 0,
                            acc.songOverlap > 0,
                            acc.movieOverlap + acc.gameOverlap + acc.songOverlap
                    )));
        }

        // Sort: overlap count first (strongest signal),
        // then alphabetical as a tiebreaker.
        results.sort((a, b) -> {
            int byOverlap = Integer.compare(b.getOverlapCount(), a.getOverlapCount());
            if (byOverlap != 0) return byOverlap;
            return Long.compare(a.getUserId(), b.getUserId());
        });

        return results;
    }

    /**
     * Query the interaction repo for users who liked the same media
     * of the given type as the viewer, then merge the overlap counts
     * into the candidate map.
     */
    private void mergeOverlap(Map<Long, SuggestionAccumulator> candidates,
                              Long userId,
                              String mediaType) {
        List<Object[]> rows =
                interactionRepo.findUsersWithLikeOverlapByMediaType(userId, mediaType);
        for (Object[] row : rows) {
            Long otherId = (Long) row[0];
            long overlap = ((Number) row[1]).longValue();
            SuggestionAccumulator acc =
                    candidates.computeIfAbsent(otherId, k -> new SuggestionAccumulator());
            switch (mediaType) {
                case "MOVIE" -> acc.movieOverlap = (int) overlap;
                case "GAME"  -> acc.gameOverlap  = (int) overlap;
                case "SONG"  -> acc.songOverlap  = (int) overlap;
            }
        }
    }

    /** Private mutable accumulator used while building the candidate map. */
    private static class SuggestionAccumulator {
        boolean sharedBoard = false;
        int movieOverlap = 0;
        int gameOverlap = 0;
        int songOverlap = 0;
    }

    /**
     * Suggest friends: users who share at least one Board with the current user,
     * excluding the user themselves, existing friends, and anyone with a pending
     * request in either direction.
     */
    public List<User> getSuggestedFriends(Long userId) {
        // 1. boards the user is actively in
        List<BoardRole> myRoles = boardRoleRepo.findActiveByUserId(userId);
        List<Long> myBoardIds = myRoles.stream()
                .map(BoardRole::getBoardId)
                .collect(Collectors.toList());

        if (myBoardIds.isEmpty()) {
            return List.of();
        }

        // 2. collect all userIds in those boards (de-duped), skipping self
        Set<Long> candidateIds = new HashSet<>();
        for (Long boardId : myBoardIds) {
            List<BoardRole> rolesInBoard = boardRoleRepo.findMembersByBoardId(boardId);
            for (BoardRole r : rolesInBoard) {
                if (!r.getUserId().equals(userId)) {
                    candidateIds.add(r.getUserId());
                }
            }
        }

        // 3. filter out anyone we already have a friendship/request with
        List<User> suggestions = new ArrayList<>();
        for (Long candidateId : candidateIds) {
            boolean hasRelation =
                    friendshipRepo.existsByUserIdAndFriendId(userId, candidateId) ||
                            friendshipRepo.existsByFriendIdAndUserId(userId, candidateId) ||
                            friendshipRepo.existsByUserIdAndFriendId(candidateId, userId) ||
                            friendshipRepo.existsByFriendIdAndUserId(candidateId, userId);
            if (hasRelation) continue;

            userRepo.findById(candidateId).ifPresent(suggestions::add);
        }

        return suggestions;
    }

    // Status helpers (for Add Friend button state)

    public enum FriendStatus { NONE, REQUEST_SENT, REQUEST_RECEIVED, FRIENDS, SELF }

    public FriendStatus getStatusBetween(Long viewerId, Long profileId) {
        if (viewerId.equals(profileId)) return FriendStatus.SELF;

        return friendshipRepo.findBetween(viewerId, profileId)
                .map(f -> {
                    if (f.isAccepted()) return FriendStatus.FRIENDS;
                    return f.getUserId().equals(viewerId)
                            ? FriendStatus.REQUEST_SENT
                            : FriendStatus.REQUEST_RECEIVED;
                })
                .orElse(FriendStatus.NONE);
    }
}