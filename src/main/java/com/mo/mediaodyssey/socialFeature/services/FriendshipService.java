package com.mo.mediaodyssey.socialFeature.services;

import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.auth.repository.UserRepository;
import com.mo.mediaodyssey.layout.models.BoardRole;
import com.mo.mediaodyssey.layout.repositories.BoardRoleRepository;
import com.mo.mediaodyssey.socialFeature.models.DTO.FriendRequestDTO;
import com.mo.mediaodyssey.socialFeature.models.Friendship;
import com.mo.mediaodyssey.socialFeature.repositories.FriendshipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages friendships between users.
 *
 * Migrated from SocialSpace -> Board:
 *  - Friend suggestions are now drawn from users who share at least one
 *    Board with the current user (via BoardRoleRepository).
 *  - SocialService dependency removed.
 */
@Service
@Transactional
public class FriendshipService {

    private final FriendshipRepository friendshipRepo;
    private final BoardRoleRepository boardRoleRepo;
    private final UserRepository userRepo;

    public FriendshipService(FriendshipRepository friendshipRepo,
                             BoardRoleRepository boardRoleRepo,
                             UserRepository userRepo) {
        this.friendshipRepo = friendshipRepo;
        this.boardRoleRepo = boardRoleRepo;
        this.userRepo = userRepo;
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