package com.mo.mediaodyssey.socialFeature.repositories;

import com.mo.mediaodyssey.socialFeature.models.DTO.FriendRequestDTO;
import com.mo.mediaodyssey.socialFeature.models.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    List<Friendship> findByUserIdOrFriendIdAndAcceptedTrue(Long userId1, Long userId2);

//    Optional<Friendship> findByUserIdAndFriendId(Long userId, Long friendId);
//    List<Friendship> findByFriendIdAndAcceptedFalse(Long friendId);

    boolean existsByUserIdAndFriendId(Long userId, Long friendId);

    boolean existsByFriendIdAndUserId(Long userId, Long friendId);

    @Query("""
        SELECT new com.mo.mediaodyssey.socialFeature.models.DTO.FriendRequestDTO(
            f.id,
            u.id,
            u.email
        )
        FROM Friendship f
        JOIN User u ON f.userId = u.id
        WHERE f.friendId = :userId
          AND f.accepted = false
        """)
    List<FriendRequestDTO> findIncomingRequests(@Param("userId") Long userId);
}