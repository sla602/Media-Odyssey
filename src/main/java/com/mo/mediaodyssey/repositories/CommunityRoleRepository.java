package com.mo.mediaodyssey.repositories;

import com.mo.mediaodyssey.enums.RoleType;
import com.mo.mediaodyssey.models.Community;
import com.mo.mediaodyssey.models.CommunityRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    //member count of community
    Integer countByCommunityId(Integer communityId);
}
