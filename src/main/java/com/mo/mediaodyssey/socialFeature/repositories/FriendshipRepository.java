package com.mo.mediaodyssey.socialFeature.repositories;

import com.mo.mediaodyssey.socialFeature.models.DTO.FriendRequestDTO;
import com.mo.mediaodyssey.socialFeature.models.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    // ─── Accepted friendships ────────────────────────────────────────

    /**
     * All accepted friendships where the given user is on either side.
     * Replaces the old misnamed findByUserIdOrFriendIdAndAcceptedTrue
     * which Spring Data was parsing as (userId) OR (friendId AND accepted=true).
     */
    @Query("SELECT f FROM Friendship f " +
            "WHERE (f.userId = :userId OR f.friendId = :userId) " +
            "AND f.accepted = true")
    List<Friendship> findAcceptedFriendshipsForUser(@Param("userId") Long userId);

    // ─── Existence checks ────────────────────────────────────────────

    boolean existsByUserIdAndFriendId(Long userId, Long friendId);

    boolean existsByFriendIdAndUserId(Long userId, Long friendId);

    /**
     * Find a friendship record in either direction (for cancelling/removing).
     */
    @Query("SELECT f FROM Friendship f " +
            "WHERE (f.userId = :userA AND f.friendId = :userB) " +
            "OR (f.userId = :userB AND f.friendId = :userA)")
    Optional<Friendship> findBetween(@Param("userA") Long userA,
                                     @Param("userB") Long userB);

    // ─── Incoming requests (others -> me, not yet accepted) ──────────

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

    // ─── Outgoing requests (me -> others, not yet accepted) ──────────

    @Query("""
        SELECT new com.mo.mediaodyssey.socialFeature.models.DTO.FriendRequestDTO(
            f.id,
            u.id,
            u.email
        )
        FROM Friendship f
        JOIN User u ON f.friendId = u.id
        WHERE f.userId = :userId
          AND f.accepted = false
        """)
    List<FriendRequestDTO> findOutgoingRequests(@Param("userId") Long userId);
}