package com.mo.mediaodyssey.socialFeature.repositories;

import com.mo.mediaodyssey.socialFeature.enums.RoleType;
import com.mo.mediaodyssey.socialFeature.models.DTO.SocialSpaceDTO;
import com.mo.mediaodyssey.socialFeature.models.DTO.SocialSpaceMemberDTO;
import com.mo.mediaodyssey.socialFeature.models.SocialSpaceRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SocialSpaceRoleRepository extends JpaRepository<SocialSpaceRole, Integer> {

    Optional<SocialSpaceRole> findByUserIdAndSocialSpaceId(Integer userId, Integer socialSpaceId);

    List<SocialSpaceRole> findBySocialSpaceId(Integer socialSpaceId);


    void deleteByUserIdAndSocialSpaceId(Integer userId, Integer socialSpaceId);


    /**
     * Returns SocialSpaceDTO for all communities the user has joined
     */
    @Query("""
    SELECT new com.mo.mediaodyssey.socialFeature.models.DTO.SocialSpaceDTO(
        ss.id,
        ss.name
    )
    FROM SocialSpaceRole ssr
    JOIN SocialSpace ss ON ssr.socialSpaceId = ss.id
    WHERE ssr.userId = :userId
    """)
    List<SocialSpaceDTO> findSocialSpacesInvolvedByUserId(@Param("userId") Integer userId);

    /**
     * Returns members of a social space as SocialSpaceMemberDTO
     */
    @Query("""
        SELECT new com.mo.mediaodyssey.socialFeature.models.DTO.SocialSpaceMemberDTO(
            u.id,
            u.username,
            ssr.roleType,
            ss.name
        )
        FROM SocialSpaceRole ssr
        JOIN User u ON ssr.userId = u.id
        JOIN SocialSpace ss ON ssr.socialSpaceId = ss.id
        WHERE ssr.socialSpaceId = :socialSpaceId
        ORDER BY ssr.roleType DESC, u.username ASC
        """)
    List<SocialSpaceMemberDTO> findSocialSpaceMembers(@Param("socialSpaceId") Integer socialSpaceId);

    /**
     * Search members by username
     */
    @Query("""
        SELECT new com.mo.mediaodyssey.socialFeature.models.DTO.SocialSpaceMemberDTO(
            u.id,
            u.username,
            ssr.roleType,
            ss.name
        )
        FROM SocialSpaceRole ssr
        JOIN User u ON ssr.userId = u.id
        JOIN SocialSpace ss ON ssr.socialSpaceId = ss.id
        WHERE ssr.socialSpaceId = :socialSpaceId
          AND LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
        ORDER BY ssr.roleType DESC, u.username ASC
        """)
    List<SocialSpaceMemberDTO> searchSocialSpaceMembers(
            @Param("socialSpaceId") Integer socialSpaceId,
            @Param("search") String search);

    /**
     * Returns only communities owned by the user as SocialSpaceDTO
     */
    @Query("""
    SELECT new com.mo.mediaodyssey.socialFeature.models.DTO.SocialSpaceDTO(
        ss.id,
        ss.name
    )
    FROM SocialSpaceRole ssr
    JOIN SocialSpace ss ON ssr.socialSpaceId = ss.id
    WHERE ssr.userId = :userId
    """)
    List<SocialSpaceDTO> findSocialSpacesOwnedByUser(@Param("userId") Integer userId);


    Integer countBySocialSpaceId(Integer socialSpaceId);

    boolean existsByUserIdAndSocialSpaceId(Integer userId, Integer socialSpaceId);
}