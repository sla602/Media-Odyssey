package com.mo.mediaodyssey.socialFeature.repositories;

import com.mo.mediaodyssey.socialFeature.models.DTO.FriendRequestDTO;
import com.mo.mediaodyssey.socialFeature.models.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FriendshipRepository extends JpaRepository<Friendship, Integer> {

    List<Friendship> findByUserIdOrFriendIdAndAcceptedTrue(Integer userId1, Integer userId2);

//    Optional<Friendship> findByUserIdAndFriendId(Integer userId, Integer friendId);
//    List<Friendship> findByFriendIdAndAcceptedFalse(Integer friendId);

    boolean existsByUserIdAndFriendId(Integer userId, Integer friendId);

    boolean existsByFriendIdAndUserId(Integer userId, Integer friendId);

    @Query("""
SELECT new com.mo.mediaodyssey.socialFeature.models.DTO.FriendRequestDTO(
    f.id,
    u.id,
    u.username
)
FROM Friendship f
JOIN User u ON f.userId = u.id
WHERE f.friendId = :userId
AND f.accepted = false
""")
    List<FriendRequestDTO> findIncomingRequests(@Param("userId") Integer userId);
}