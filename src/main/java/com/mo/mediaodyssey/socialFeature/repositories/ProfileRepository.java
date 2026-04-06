package com.mo.mediaodyssey.socialFeature.repositories;

import com.mo.mediaodyssey.layout.models.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    /**
     * Case-insensitive partial match on username, excluding the searcher.
     * Only returns profiles that actually have a non-blank username.
     */
    @Query("SELECT p FROM Profile p " +
            "WHERE LOWER(p.username) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "AND p.username IS NOT NULL " +
            "AND p.username <> '' " +
            "AND p.userId <> :excludeUserId")
    List<Profile> searchByUsername(@Param("query") String query,
                                   @Param("excludeUserId") Long excludeUserId);
}