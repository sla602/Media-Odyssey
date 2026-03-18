package com.mo.mediaodyssey.socialFeature.services;

import com.mo.mediaodyssey.socialFeature.enums.RoleType;
import com.mo.mediaodyssey.socialFeature.models.Comment;
import com.mo.mediaodyssey.socialFeature.models.Community;
import com.mo.mediaodyssey.socialFeature.models.CommunityRole;
import com.mo.mediaodyssey.socialFeature.models.DTO.CommunityDTO;
import com.mo.mediaodyssey.socialFeature.models.DTO.CommunityMemberDTO;
import com.mo.mediaodyssey.socialFeature.models.Post;
import com.mo.mediaodyssey.socialFeature.repositories.CommentRepository;
import com.mo.mediaodyssey.socialFeature.repositories.CommunityRepository;
import com.mo.mediaodyssey.socialFeature.repositories.CommunityRoleRepository;
import com.mo.mediaodyssey.socialFeature.repositories.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Transactional
public class CommuService {

    private final CommunityRepository communityRepository;
    private final CommunityRoleRepository roleRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PermissionService permissionService;
    private final CommentService commentService;

    public CommuService(CommunityRepository communityRepository,
                        CommunityRoleRepository roleRepository, PermissionService permissionService, PostRepository postRepository, CommentRepository commentRepository, CommentService commentService
    ) {
        this.communityRepository = communityRepository;
        this.roleRepository = roleRepository;
        this.permissionService = permissionService;

        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.commentService = commentService;
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

    public void leaveCommunity(Integer userId, Integer communityId) {
        CommunityRole role = roleRepository
                .findByUserIdAndCommunityId(userId, communityId)
                .orElseThrow(() -> new RuntimeException("User not a member"));

        if (role.getRoleType().isOwner()) {
            throw new RuntimeException("Owner cannot leave community. Transfer ownership first.");
        }

        roleRepository.deleteByUserIdAndCommunityId(userId, communityId);
    }

    public void createPost(Integer userId, Integer communityId, String title, String content) {
        // Check permission
        permissionService.canCreatePost(userId, communityId);

        // check if user is member of community
        roleRepository.findByUserIdAndCommunityId(userId, communityId)
                .orElseThrow(() -> new RuntimeException("User not a member of this community"));

        // Create post
        Post post = new Post(communityId,userId,title,content);


        postRepository.save(post);
    }

    public void editPost(Integer userId, Integer postId, String newTitle, String newContent) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        Integer communityId = post.getCommunityId();

        // Check permission to edit posts
        permissionService.canEditPost(userId, communityId);

        // Only the author can edit
        if (!post.getAuthorId().equals(userId)) {
            throw new RuntimeException("Members can only edit their own posts");
        }

        post.setTitle(newTitle);
        post.setContent(newContent);
        postRepository.save(post);
    }


    public void createComment(Integer userId, Integer postId, String content){

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        Integer communityId = post.getCommunityId();

        permissionService.canCreateComment(userId, communityId);

        commentService.createComment(userId, postId, content);
    }

    public void replyToComment(Integer userId, Integer parentCommentId, String content){

        Integer postId = commentService.getParentPostId(parentCommentId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        Integer communityId = post.getCommunityId();

        permissionService.canCreateComment(userId, communityId);

        commentService.replyToComment(userId, parentCommentId, content);
    }

    public void editComment(Integer userId, Integer commentId, String newContent){

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        Post post = postRepository.findById(comment.getPostId())
                .orElseThrow(() -> new RuntimeException("Post not found"));

        permissionService.canEditComment(userId, post.getCommunityId());

        if(!comment.getAuthorId().equals(userId)){
            throw new RuntimeException("Members can only edit their own comments");
        }

        commentService.updateCommentContent(commentId, newContent);
    }

    public void deletePost(Integer actingUserId, Integer postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        Integer communityId = post.getCommunityId();

        // Check permission to delete posts
        permissionService.canDeletePost(actingUserId, communityId);

        // Ownership check
        CommunityRole actingRole = roleRepository
                .findByUserIdAndCommunityId(actingUserId, communityId)
                .orElseThrow(() -> new RuntimeException("User not in community"));

        if (actingRole.getRoleType().isMember() && !post.getAuthorId().equals(actingUserId)) {
            throw new RuntimeException("Members can only delete their own posts");
        }

        // Moderators and owners can delete any post
        postRepository.delete(post);
    }

    public void deleteComment(Integer actingUserId, Integer commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        Post post = postRepository.findById(comment.getPostId())
                .orElseThrow(() -> new RuntimeException("Post not found"));

        Integer communityId = post.getCommunityId();

        // Check permission
        permissionService.canDeleteComment(actingUserId, communityId);

        CommunityRole actingRole = roleRepository
                .findByUserIdAndCommunityId(actingUserId, communityId)
                .orElseThrow(() -> new RuntimeException("User not in community"));

        // Members can only delete their own comments
        if (actingRole.getRoleType().isMember() && !comment.getAuthorId().equals(actingUserId)) {
            throw new RuntimeException("Members can only delete their own comments");
        }

        // Moderators and owners can delete any comment
        commentRepository.delete(comment);
    }




    public void promoteMember(Integer actingUserId, Integer targetUserId, Integer communityId) {

        permissionService.canPromoteMember(actingUserId, communityId);


        CommunityRole targetRole = roleRepository
                .findByUserIdAndCommunityId(targetUserId, communityId)
                .orElseThrow(() -> new RuntimeException("Target user not in community"));

        targetRole.setRoleType(RoleType.MODERATOR);
        roleRepository.save(targetRole);
    }



    public void demoteModerator(Integer actingUserId,
                                Integer targetUserId,
                                Integer communityId) {

        permissionService.canDemoteModerator(actingUserId, communityId);

        CommunityRole targetRole = roleRepository
                .findByUserIdAndCommunityId(targetUserId, communityId)
                .orElseThrow(() -> new RuntimeException("Target user not in community"));

        if (!targetRole.getRoleType().isModerator()) {
            throw new RuntimeException("Target user is not a moderator");
        }

        targetRole.setRoleType(RoleType.MEMBER);
        roleRepository.save(targetRole);
    }

    public void transferOwnership(Integer actingUserId,
                                  Integer targetUserId,
                                  Integer communityId) {

        permissionService.canTransferOwnership(actingUserId, communityId);

        CommunityRole currentOwner = roleRepository
                .findByUserIdAndCommunityId(actingUserId, communityId)
                .orElseThrow(() -> new RuntimeException("You are not owner"));

        CommunityRole newOwner = roleRepository
                .findByUserIdAndCommunityId(targetUserId, communityId)
                .orElseThrow(() -> new RuntimeException("Target user not in community"));

        // Swap roles
        currentOwner.setRoleType(RoleType.MODERATOR);
        newOwner.setRoleType(RoleType.OWNER);

        roleRepository.save(currentOwner);
        roleRepository.save(newOwner);
    }



    public void kickMember(Integer actingUserId, Integer targetUserId, Integer communityId) {

        permissionService.canKickMember(actingUserId, communityId);

        CommunityRole targetRole = roleRepository
                .findByUserIdAndCommunityId(targetUserId, communityId)
                .orElseThrow(() -> new RuntimeException("Target user not in community"));

        if (targetRole.getRoleType().isOwner()) {
            throw new RuntimeException("Cannot kick the owner");
        }

        roleRepository.deleteByUserIdAndCommunityId(targetUserId, communityId);
    }


    public void editCommunity(Integer actingUserId, Integer communityId, String newName, String newDescription) {

        permissionService.canEditCommunity(actingUserId, communityId);

        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new RuntimeException("Community not found"));

        community.setName(newName);
        community.setDescription(newDescription);
        communityRepository.save(community);
    }

    public void deleteCommunity(Integer actingUserId, Integer communityId) {

        permissionService.canDeleteCommunity(actingUserId, communityId);

        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new RuntimeException("Community not found"));

        // Delete all roles first
        List<CommunityRole> roles = roleRepository.findByCommunityId(communityId);
        roleRepository.deleteAll(roles);

        communityRepository.delete(community);
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

    public List<CommunityMemberDTO> getCommunityMembers(Integer communityId) {
        return roleRepository.findCommunityMembers(communityId);
    }

    public List<CommunityDTO> getOwnedCommunities(Integer userId) {
        return roleRepository.findCommunitiesOwnedByUser(userId);
    }

    public List<CommunityMemberDTO> searchCommunityMembers(Integer userId,String search) {
        return roleRepository.searchCommunityMembers(userId,search);
    }

}