package com.mo.mediaodyssey.socialFeature.repositories;

import com.mo.mediaodyssey.socialFeature.enums.RoleType;
import com.mo.mediaodyssey.socialFeature.models.SocialSpace;
import com.mo.mediaodyssey.socialFeature.models.SocialSpaceRole;
import com.mo.mediaodyssey.socialFeature.models.DTO.SocialSpaceDTO;
import com.mo.mediaodyssey.socialFeature.models.DTO.SocialSpaceMemberDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SocialSpaceRoleRepository extends JpaRepository<SocialSpaceRole,Integer> {



    Optional<SocialSpaceRole> findByUserIdAndSocialSpaceId(Integer userId, Integer SocialSpaceId);

    boolean existsByUserIdAndSocialSpaceId(Integer userId, Integer socialSpaceId);

    List<SocialSpaceRole> findBySocialSpaceId(Integer socialSpaceId);


    List<SocialSpaceRole> findBySocialSpaceIdAndRoleType(Integer socialSpaceId, RoleType role);

    void deleteByUserIdAndSocialSpaceId(Integer userId, Integer socialSpaceId);



    @Query("""
SELECT ss
FROM SocialSpace ss
JOIN SocialSpaceRole ssr ON ss.id = ssr.socialSpaceId
WHERE ssr.userId = :userId
""")
    List<SocialSpace> findSocialSpacesByUserId(@Param("userId") Integer userId);

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
            @Param("search") String search
    );

    @Query("""
SELECT new com.mo.mediaodyssey.socialFeature.models.DTO.SocialSpaceDTO(
    ss.id,
    ss.name
)
FROM SocialSpaceRole ssr
JOIN SocialSpace ss ON ssr.socialSpaceId = ss.id
WHERE ssr.userId = :userId
AND ssr.roleType = com.mo.mediaodyssey.socialFeature.enums.RoleType.OWNER
""")
    List<SocialSpaceDTO> findSocialSpacesOwnedByUser(@Param("userId") Integer userId);



    //member count of socialSpace
    Integer countBySocialSpaceId(Integer socialSpaceId);


    List<SocialSpaceRole> findByUserId(Integer userId);
}
