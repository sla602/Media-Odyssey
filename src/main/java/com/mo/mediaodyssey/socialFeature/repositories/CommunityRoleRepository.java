package com.mo.mediaodyssey.socialFeature.repositories;

import com.mo.mediaodyssey.socialFeature.enums.RoleType;
import com.mo.mediaodyssey.socialFeature.models.Community;
import com.mo.mediaodyssey.socialFeature.models.CommunityRole;
import com.mo.mediaodyssey.socialFeature.models.DTO.CommunityDTO;
import com.mo.mediaodyssey.socialFeature.models.DTO.CommunityMemberDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommunityRoleRepository extends JpaRepository<CommunityRole,Integer> {



    Optional<CommunityRole> findByUserIdAndCommunityId(Integer userId, Integer communityId);

    boolean existsByUserIdAndCommunityId(Integer userId, Integer communityId);

    List<CommunityRole> findByCommunityId(Integer communityId);


    List<CommunityRole> findByCommunityIdAndRoleType(Integer communityId, RoleType role);

    void deleteByUserIdAndCommunityId(Integer userId, Integer communityId);



    @Query("""
SELECT c
FROM Community c
JOIN CommunityRole r ON c.id = r.communityId
WHERE r.userId = :userId
""")
    List<Community> findCommunitiesByUserId(@Param("userId") Integer userId);

    @Query("""
SELECT new com.mo.mediaodyssey.models.DTO.CommunityMemberDTO(
    u.id,
    u.username,
    cr.roleType,
    c.name
)
FROM CommunityRole cr
JOIN User u ON cr.userId = u.id
JOIN Community c ON cr.communityId = c.id
WHERE cr.communityId = :communityId
ORDER BY cr.roleType DESC, u.username ASC
""")
    List<CommunityMemberDTO> findCommunityMembers(@Param("communityId") Integer communityId);


    @Query("""
SELECT new com.mo.mediaodyssey.models.DTO.CommunityMemberDTO(
    u.id,
    u.username,
    cr.roleType,
    c.name
)
FROM CommunityRole cr
JOIN User u ON cr.userId = u.id
JOIN Community c ON cr.communityId = c.id
WHERE cr.communityId = :communityId
AND LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
ORDER BY cr.roleType DESC, u.username ASC
""")
    List<CommunityMemberDTO> searchCommunityMembers(
            @Param("communityId") Integer communityId,
            @Param("search") String search
    );

    @Query("""
SELECT new com.mo.mediaodyssey.models.DTO.CommunityDTO(
    c.id,
    c.name
)
FROM CommunityRole cr
JOIN Community c ON cr.communityId = c.id
WHERE cr.userId = :userId
AND cr.roleType = com.mo.mediaodyssey.enums.RoleType.OWNER
""")
    List<CommunityDTO> findCommunitiesOwnedByUser(@Param("userId") Integer userId);



    //member count of community
    Integer countByCommunityId(Integer communityId);


    List<CommunityRole> findByUserId(Integer userId);
}
