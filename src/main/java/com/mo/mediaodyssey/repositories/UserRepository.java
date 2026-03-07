package com.mo.mediaodyssey.repositories;

import com.mo.mediaodyssey.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("""
SELECT u FROM User u
WHERE u.id IN (
    SELECT r.userId
    FROM CommunityRole r
    WHERE r.communityId IN (
        SELECT r2.communityId
        FROM CommunityRole r2
        WHERE r2.userId = :userId
    )
)
AND u.id <> :userId
AND u.id NOT IN (
    SELECT f.friendId
    FROM Friendship f
    WHERE f.userId = :userId
    AND f.accepted = true
)
""")
    List<User> findSuggestedFriends(@Param("userId") Integer userId);


    @Query("""
        SELECT u
        FROM User u
        WHERE u.id <> :userId
          AND u.id NOT IN (
              SELECT f.friendId 
              FROM Friendship f 
              WHERE f.userId = :userId AND f.accepted = true
              UNION
              SELECT f.userId 
              FROM Friendship f 
              WHERE f.friendId = :userId AND f.accepted = true
          )
          AND u.id IN (
              SELECT cr.userId
              FROM CommunityRole cr
              WHERE cr.communityId IN :communityIds
          )
    """)
    List<User> findUsersInCommunitiesExcludingFriends(
            @Param("userId") Integer userId,
            @Param("communityIds") List<Integer> communityIds
    );
}