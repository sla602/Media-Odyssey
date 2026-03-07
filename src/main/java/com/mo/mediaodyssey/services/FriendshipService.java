package com.mo.mediaodyssey.services;

import com.mo.mediaodyssey.models.Community;
import com.mo.mediaodyssey.models.Friendship;
import com.mo.mediaodyssey.models.User;
import com.mo.mediaodyssey.repositories.FriendshipRepository;
import com.mo.mediaodyssey.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class FriendshipService {

    private final FriendshipRepository friendshipRepo;
    private final CommunityService communityService;
    private final UserRepository userRepo;

    public FriendshipService(FriendshipRepository friendshipRepo,
                             CommunityService communityService,
                             UserRepository userRepo) {
        this.friendshipRepo = friendshipRepo;
        this.communityService = communityService;
        this.userRepo = userRepo;
    }

    /** Send a friend request */
    public Friendship sendFriendRequest(Integer fromUserId, Integer toUserId) {
        if(friendshipRepo.findByUserIdAndFriendId(fromUserId, toUserId).isPresent()) {
            throw new IllegalStateException("Friend request already exists");
        }
        Friendship request = new Friendship(fromUserId, toUserId);
        return friendshipRepo.save(request);
    }

    /** Accept a friend request */
    public void acceptFriendRequest(Integer requestId) {
        Friendship request = friendshipRepo.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("Friend request not found"));
        request.setAccepted(true);
        friendshipRepo.save(request);
    }

    /** Get friends as User objects */
    public List<User> getFriends(Integer userId) {
        List<Friendship> friendships = friendshipRepo.findByUserIdOrFriendIdAndAcceptedTrue(userId, userId);

        return friendships.stream()
                .map(f -> {
                    Integer otherId = f.getUserId().equals(userId) ? f.getFriendId() : f.getUserId();
                    return userRepo.findById(otherId).orElse(null);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    public List<User> getIncomingRequests(Integer userId) {
        return friendshipRepo.findByFriendIdAndAcceptedFalse(userId).stream()
                .map(f -> userRepo.findById(f.getUserId()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    public List<User> getSuggestedFriends(Integer userId) {
        List<Integer> communityIds = communityService.getUserCommunities(userId).stream()
                .map(Community::getId)
                .collect(Collectors.toList());

        return userRepo.findUsersInCommunitiesExcludingFriends(userId, communityIds);
    }
}