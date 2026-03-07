package com.mo.mediaodyssey.services;

import com.mo.mediaodyssey.enums.RoleType;
import com.mo.mediaodyssey.models.Community;
import com.mo.mediaodyssey.models.CommunityRole;
import com.mo.mediaodyssey.repositories.CommunityRepository;
import com.mo.mediaodyssey.repositories.CommunityRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final CommunityRoleRepository roleRepository;

    public CommunityService(CommunityRepository communityRepository,
                            CommunityRoleRepository roleRepository
                            ) {
        this.communityRepository = communityRepository;
        this.roleRepository = roleRepository;

    }



    public void createCommunity(
            Integer creatorId,
            String name,
            String description) {

        Community community = new Community(name,description,creatorId);


        communityRepository.save(community);

        // Creator becomes OWNER
        CommunityRole role = new CommunityRole(creatorId,community.getId(), RoleType.OWNER);

        roleRepository.save(role);
    }


    public void joinCommunity(Integer userId, Integer communityId) {

        if (roleRepository
                .findByUserIdAndCommunityId(userId, communityId)
                .isPresent()) {
            throw new IllegalStateException("Already a member");
        }

        CommunityRole role = new CommunityRole(userId,communityId,RoleType.MEMBER);

        roleRepository.save(role);
    }


    public void promoteToModerator(Integer actingUserId, Integer targetUserId, Integer communityId) {

        CommunityRole actingRole = roleRepository
                .findByUserIdAndCommunityId(actingUserId, communityId)
                .orElseThrow(() -> new IllegalStateException("No role found"));

        if (actingRole.getRoleType() != RoleType.OWNER) {
            throw new SecurityException("Only owner can promote");
        }

        CommunityRole targetRole = roleRepository
                .findByUserIdAndCommunityId(targetUserId, communityId)
                .orElseThrow(() -> new IllegalStateException("User not member"));

        targetRole.setRoleType(RoleType.MODERATOR);

        roleRepository.save(targetRole);
    }


    public Integer getMemberCount(Integer communityId) {
        return roleRepository.countByCommunityId(communityId);
    }

    public Community getCommunityById(Integer communityId) {

        return communityRepository.findById(communityId)
                .orElseThrow(() -> new RuntimeException("Community not found"));
    }


    public List<Community> getAllCommunities() {
        return communityRepository.findAll();
    }

    public List<Community> getUserCommunities(Integer userId) {
        return roleRepository.findCommunitiesByUserId(userId);
    }

    public boolean isMember(Integer userId, Integer communityId) {
        return roleRepository.existsByUserIdAndCommunityId(userId, communityId);
    }
}