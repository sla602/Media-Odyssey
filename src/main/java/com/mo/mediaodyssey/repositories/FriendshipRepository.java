package com.mo.mediaodyssey.repositories;

import com.mo.mediaodyssey.models.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Integer> {

    List<Friendship> findByUserIdOrFriendIdAndAcceptedTrue(Integer userId1, Integer userId2);

    Optional<Friendship> findByUserIdAndFriendId(Integer userId, Integer friendId);
    List<Friendship> findByFriendIdAndAcceptedFalse(Integer friendId);
}