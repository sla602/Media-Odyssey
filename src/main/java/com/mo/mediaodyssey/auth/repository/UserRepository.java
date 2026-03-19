package com.mo.mediaodyssey.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mo.mediaodyssey.shared.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // used by auth
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // used by social features
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);

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
              SELECT ssr.userId
              FROM SocialSpaceRole ssr
              WHERE ssr.socialSpaceId IN :socialSpaceIds
          )
    """)
    List<User> findUsersInSocialSpacesExcludingFriends(
            @Param("userId") Long userId,
            @Param("communityIds") List<Integer> communityIds
    );
}