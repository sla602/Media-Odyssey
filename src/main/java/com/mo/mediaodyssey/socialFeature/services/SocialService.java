package com.mo.mediaodyssey.socialFeature.services;

import com.mo.mediaodyssey.socialFeature.enums.RoleType;
import com.mo.mediaodyssey.socialFeature.models.Comment;
import com.mo.mediaodyssey.socialFeature.models.SocialSpace;
import com.mo.mediaodyssey.socialFeature.models.DTO.SocialSpaceDTO;
import com.mo.mediaodyssey.socialFeature.models.DTO.SocialSpaceMemberDTO;
import com.mo.mediaodyssey.socialFeature.models.SocialSpaceRole;
import com.mo.mediaodyssey.socialFeature.models.Post;
import com.mo.mediaodyssey.socialFeature.repositories.CommentRepository;
import com.mo.mediaodyssey.socialFeature.repositories.PostRepository;
import com.mo.mediaodyssey.socialFeature.repositories.SocialSpaceRepository;
import com.mo.mediaodyssey.socialFeature.repositories.SocialSpaceRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Transactional
public class SocialService {

    private final SocialSpaceRepository socialSpaceRepository;
    private final SocialSpaceRoleRepository roleRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PermissionService permissionService;
    private final CommentService commentService;

    public SocialService(SocialSpaceRepository socialSpaceRepository,
                        SocialSpaceRoleRepository roleRepository, PermissionService permissionService, PostRepository postRepository, CommentRepository commentRepository, CommentService commentService
    ) {
        this.socialSpaceRepository = socialSpaceRepository;
        this.roleRepository = roleRepository;
        this.permissionService = permissionService;

        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.commentService = commentService;
    }

    // TODO: collapsed by board
    public void createSocialSpace(
            Integer creatorId,
            String name,
            String description) {

        SocialSpace socialSpace = new SocialSpace(name,description,creatorId);


        socialSpaceRepository.save(socialSpace);

        // Creator becomes OWNER
        SocialSpaceRole role = new SocialSpaceRole(creatorId,socialSpace.getId(), RoleType.OWNER);

        roleRepository.save(role);
    }

    // TODO: collapsed by board
    public void joinSocialSpace(Integer userId, Integer socialSpaceId) {

        if (roleRepository
                .findByUserIdAndSocialSpaceId(userId, socialSpaceId)
                .isPresent()) {
            throw new IllegalStateException("Already a member");
        }

        SocialSpaceRole role = new SocialSpaceRole(userId,socialSpaceId,RoleType.MEMBER);

        roleRepository.save(role);
    }


    // TODO: collapsed by board
    public void leaveSocialSpace(Integer userId, Integer socialSpaceId) {
        SocialSpaceRole role = roleRepository
                .findByUserIdAndSocialSpaceId(userId, socialSpaceId)
                .orElseThrow(() -> new RuntimeException("User not a member"));

        if (role.getRoleType().isOwner()) {
            throw new RuntimeException("Owner cannot leave community. Transfer ownership first.");
        }

        roleRepository.deleteByUserIdAndSocialSpaceId(userId, socialSpaceId);
    }

    public void createPost(Integer userId, Integer socialSpaceId, String title, String content) {
        // Check permission
        permissionService.canCreatePost(userId, socialSpaceId);

        // check if user is member of community
        roleRepository.findByUserIdAndSocialSpaceId(userId, socialSpaceId)
                .orElseThrow(() -> new RuntimeException("User not a member of this community"));

        // Create post
        Post post = new Post(socialSpaceId,userId,title,content);


        postRepository.save(post);
    }

    public void editPost(Integer userId, Integer postId, String newTitle, String newContent) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        Integer socialSpaceId = post.getSocialSpaceId();

        // Check permission to edit posts
        permissionService.canEditPost(userId, socialSpaceId);

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

        Integer socialSpaceId = post.getSocialSpaceId();

        permissionService.canCreateComment(userId, socialSpaceId);

        commentService.createComment(userId, postId, content);
    }

    public void replyToComment(Integer userId, Integer parentCommentId, String content){

        Integer postId = commentService.getParentPostId(parentCommentId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        Integer socialSpaceId = post.getSocialSpaceId();

        permissionService.canCreateComment(userId, socialSpaceId);

        commentService.replyToComment(userId, parentCommentId, content);
    }

    public void editComment(Integer userId, Integer commentId, String newContent){

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        Post post = postRepository.findById(comment.getPostId())
                .orElseThrow(() -> new RuntimeException("Post not found"));

        permissionService.canEditComment(userId, post.getSocialSpaceId());

        if(!comment.getAuthorId().equals(userId)){
            throw new RuntimeException("Members can only edit their own comments");
        }

        commentService.updateCommentContent(commentId, newContent);
    }

    public void deletePost(Integer actingUserId, Integer postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        Integer socialSpaceId = post.getSocialSpaceId();

        // Check permission to delete posts
        permissionService.canDeletePost(actingUserId, socialSpaceId);

        // Ownership check
        SocialSpaceRole actingRole = roleRepository
                .findByUserIdAndSocialSpaceId(actingUserId, socialSpaceId)
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

        Integer socialSpaceId = post.getSocialSpaceId();

        // Check permission
        permissionService.canDeleteComment(actingUserId, socialSpaceId);

        SocialSpaceRole actingRole = roleRepository
                .findByUserIdAndSocialSpaceId(actingUserId, socialSpaceId)
                .orElseThrow(() -> new RuntimeException("User not in community"));

        // Members can only delete their own comments
        if (actingRole.getRoleType().isMember() && !comment.getAuthorId().equals(actingUserId)) {
            throw new RuntimeException("Members can only delete their own comments");
        }

        // Moderators and owners can delete any comment
        commentRepository.delete(comment);
    }




    public void promoteMember(Integer actingUserId, Integer targetUserId, Integer socialSpaceId) {

        permissionService.canPromoteMember(actingUserId, socialSpaceId);


        SocialSpaceRole targetRole = roleRepository
                .findByUserIdAndSocialSpaceId(targetUserId, socialSpaceId)
                .orElseThrow(() -> new RuntimeException("Target user not in community"));

        targetRole.setRoleType(RoleType.MODERATOR);
        roleRepository.save(targetRole);
    }



    public void demoteModerator(Integer actingUserId,
                                Integer targetUserId,
                                Integer socialSpaceId) {

        permissionService.canDemoteModerator(actingUserId, socialSpaceId);

        SocialSpaceRole targetRole = roleRepository
                .findByUserIdAndSocialSpaceId(targetUserId, socialSpaceId)
                .orElseThrow(() -> new RuntimeException("Target user not in community"));

        if (!targetRole.getRoleType().isModerator()) {
            throw new RuntimeException("Target user is not a moderator");
        }

        targetRole.setRoleType(RoleType.MEMBER);
        roleRepository.save(targetRole);
    }

    public void transferOwnership(Integer actingUserId,
                                  Integer targetUserId,
                                  Integer socialSpaceId) {

        permissionService.canTransferOwnership(actingUserId, socialSpaceId);

        SocialSpaceRole currentOwner = roleRepository
                .findByUserIdAndSocialSpaceId(actingUserId, socialSpaceId)
                .orElseThrow(() -> new RuntimeException("You are not owner"));

        SocialSpaceRole newOwner = roleRepository
                .findByUserIdAndSocialSpaceId(targetUserId, socialSpaceId)
                .orElseThrow(() -> new RuntimeException("Target user not in community"));

        // Swap roles
        currentOwner.setRoleType(RoleType.MODERATOR);
        newOwner.setRoleType(RoleType.OWNER);

        roleRepository.save(currentOwner);
        roleRepository.save(newOwner);
    }



    public void kickMember(Integer actingUserId, Integer targetUserId, Integer socialSpaceId) {

        permissionService.canKickMember(actingUserId, socialSpaceId);

        SocialSpaceRole targetRole = roleRepository
                .findByUserIdAndSocialSpaceId(targetUserId, socialSpaceId)
                .orElseThrow(() -> new RuntimeException("Target user not in community"));

        if (targetRole.getRoleType().isOwner()) {
            throw new RuntimeException("Cannot kick the owner");
        }

        roleRepository.deleteByUserIdAndSocialSpaceId(targetUserId, socialSpaceId);
    }


    public void editSocialSpace(Integer actingUserId, Integer socialSpaceId, String newName, String newDescription) {

        permissionService.canEditSocialSpace(actingUserId, socialSpaceId);

        SocialSpace socialSpace = socialSpaceRepository.findById(socialSpaceId)
                .orElseThrow(() -> new RuntimeException("SocialSpace not found"));

        socialSpace.setName(newName);
        socialSpace.setDescription(newDescription);
        socialSpaceRepository.save(socialSpace);
    }

    public void deleteSocialSpace(Integer actingUserId, Integer socialSpaceId) {

        permissionService.canDeleteSocialSpace(actingUserId, socialSpaceId);

        SocialSpace community = socialSpaceRepository.findById(socialSpaceId)
                .orElseThrow(() -> new RuntimeException("SocialSpace not found"));

        // Delete all roles first
        List<SocialSpaceRole> roles = roleRepository.findBySocialSpaceId(socialSpaceId);
        roleRepository.deleteAll(roles);

        socialSpaceRepository.delete(community);
    }


    public Integer getMemberCount(Integer socialSpaceId) {
        return roleRepository.countBySocialSpaceId(socialSpaceId);
    }

    public SocialSpace getSocialSpaceById(Integer socialSpaceId) {

        return socialSpaceRepository.findById(socialSpaceId)
                .orElseThrow(() -> new RuntimeException("SocialSpace not found"));
    }


    public List<SocialSpace> getAllCommunities() {
        return socialSpaceRepository.findAll();
    }

    public List<SocialSpace> getUserCommunities(Integer userId) {
        return roleRepository.findSocialSpacesByUserId(userId);
    }

    public List<SocialSpaceMemberDTO> getSocialSpaceMembers(Integer socialSpaceId) {
        return roleRepository.findSocialSpaceMembers(socialSpaceId);
    }

    public List<SocialSpaceDTO> getOwnedCommunities(Integer userId) {
        return roleRepository.findSocialSpacesOwnedByUser(userId);
    }

    public List<SocialSpaceMemberDTO> searchSocialSpaceMembers(Integer userId,String search) {
        return roleRepository.searchSocialSpaceMembers(userId,search);
    }

}