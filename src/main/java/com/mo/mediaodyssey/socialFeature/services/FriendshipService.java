package com.mo.mediaodyssey.socialFeature.services;

import com.mo.mediaodyssey.auth.model.User;
import com.mo.mediaodyssey.auth.repository.UserRepository;
import com.mo.mediaodyssey.socialFeature.models.DTO.FriendRequestDTO;
import com.mo.mediaodyssey.socialFeature.models.Friendship;
import com.mo.mediaodyssey.socialFeature.models.SocialSpace;
import com.mo.mediaodyssey.socialFeature.repositories.FriendshipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class FriendshipService {

    private final FriendshipRepository friendshipRepo;
    private final SocialService socialService;
    private final UserRepository userRepo;

    public FriendshipService(FriendshipRepository friendshipRepo,
                             SocialService socialService,
                             UserRepository userRepo) {
        this.friendshipRepo = friendshipRepo;
        this.socialService = socialService;
        this.userRepo = userRepo;
    }

    public void sendFriendRequest(Integer fromUserId, Integer toUserId) {
        if (friendshipRepo.existsByUserIdAndFriendId(fromUserId, toUserId) ||
                friendshipRepo.existsByFriendIdAndUserId(fromUserId, toUserId)) {
            throw new IllegalStateException("Friend request already exists");
        }
        Friendship request = new Friendship(fromUserId, toUserId);
        friendshipRepo.save(request);
    }

    public void acceptFriendRequest(Integer requestId) {
        Friendship request = friendshipRepo.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("Friend request not found"));
        request.setAccepted(true);
        friendshipRepo.save(request);
    }

    public List<User> getFriends(Integer userId) {
        List<Friendship> friendships = friendshipRepo.findByUserIdOrFriendIdAndAcceptedTrue(userId, userId);

        return friendships.stream()
                .map(f -> {
                    Integer otherId = f.getUserId().equals(userId) ? f.getFriendId() : f.getUserId();
                    return userRepo.findById(otherId.longValue()).orElse(null);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<FriendRequestDTO> getIncomingRequests(Integer userId) {
        return friendshipRepo.findIncomingRequests(userId);
    }

    public List<User> getSuggestedFriends(Long userId) {
        List<Integer> socialSpaceIds = socialService.getUserCommunities(userId.intValue()).stream()
                .map(SocialSpace::getId)
                .collect(Collectors.toList());

        List<User> candidates = userRepo.findUsersInSocialSpacesExcludingFriends(userId, socialSpaceIds);

        return candidates.stream()
                .filter(u ->
                        !friendshipRepo.existsByUserIdAndFriendId(userId.intValue(), u.getId().intValue()) &&
                        !friendshipRepo.existsByFriendIdAndUserId(userId.intValue(), u.getId().intValue())
                )
                .toList();
    }
}