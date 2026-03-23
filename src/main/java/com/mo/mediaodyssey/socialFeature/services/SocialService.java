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
    public SocialSpace createSocialSpace(
            Integer creatorId,
            String name,
            String description) {

        SocialSpace socialSpace = new SocialSpace(name, description, creatorId);

        socialSpaceRepository.save(socialSpace);

        // Creator becomes OWNER
        SocialSpaceRole role = new SocialSpaceRole(
                creatorId,
                socialSpace.getId(),
                RoleType.OWNER
        );

        roleRepository.save(role);

        return socialSpace;
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
        Post post = new Post(socialSpaceId,userId,title,content,false);


        postRepository.save(post);
    }

    public void editPost(Integer userId, Integer postId, String newTitle, String newContent) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        // Only author can edit — moderators/owners cannot
        if (!post.getAuthorId().equals(userId)) {
            throw new RuntimeException("You can only edit your own posts");
        }

        // Optional membership check
        Integer socialSpaceId = post.getSocialSpaceId();
        roleRepository.findByUserIdAndSocialSpaceId(userId, socialSpaceId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this space"));

        post.setTitle(newTitle);
        post.setContent(newContent);
        postRepository.save(post);
    }

    // Create a top-level comment on a post
    public void createComment(Integer userId, Integer postId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        Integer socialSpaceId = post.getSocialSpaceId();

        permissionService.canCreateComment(userId, socialSpaceId);

        commentService.createComment(userId, postId, content);
    }

    // Create a reply to an existing comment
    public void replyToComment(Integer userId, Integer parentCommentId, String content) {
        Integer postId = commentService.getParentPostId(parentCommentId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found for comment id: " + parentCommentId));

        Integer socialSpaceId = post.getSocialSpaceId();

        permissionService.canCreateComment(userId, socialSpaceId);

        commentService.replyToComment(userId, parentCommentId, content);
    }

    // Edit an existing comment
    public void editComment(Integer userId, Integer commentId, String newContent) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));

        if (comment.isDeleted()) {
            throw new RuntimeException("Cannot edit a deleted comment");
        }

        // Only the original author is allowed to edit
        if (!comment.getAuthorId().equals(userId)) {
            throw new RuntimeException("You can only edit your own comments");
        }

        Post post = postRepository.findById(comment.getPostId())
                .orElseThrow(() -> new RuntimeException("Post not found for comment id: " + commentId));

        Integer socialSpaceId = post.getSocialSpaceId();

        roleRepository.findByUserIdAndSocialSpaceId(userId, socialSpaceId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this space"));

        // Perform the edit
        commentService.updateCommentContent(commentId, newContent);
    }

    // Soft-delete a comment
    public void deleteComment(Integer actingUserId, Integer commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));

        Post post = postRepository.findById(comment.getPostId())
                .orElseThrow(() -> new RuntimeException("Post not found for comment id: " + commentId));

        Integer socialSpaceId = post.getSocialSpaceId();

        // Only check DELETE_COMMENT permission if it's NOT the user's own comment
        if (!comment.getAuthorId().equals(actingUserId)) {
            permissionService.canDeleteComment(actingUserId, socialSpaceId);
        }

        // Membership check
        roleRepository.findByUserIdAndSocialSpaceId(actingUserId, socialSpaceId)
                .orElseThrow(() -> new RuntimeException("User is not a member of this social space"));

        commentService.softDeleteComment(commentId);
    }


//    public void deletePost(Integer actingUserId, Integer postId) {
//        Post post = postRepository.findById(postId)
//                .orElseThrow(() -> new RuntimeException("Post not found"));
//
//        Integer socialSpaceId = post.getSocialSpaceId();
//
//        boolean isOwnPost = post.getAuthorId().equals(actingUserId);
//
//        if (!isOwnPost) {
//            permissionService.canDeletePost(actingUserId, socialSpaceId);
//        }
//
//        roleRepository.findByUserIdAndSocialSpaceId(actingUserId, socialSpaceId)
//                .orElseThrow(() -> new RuntimeException("User is not a member of this social space"));
//
//        postRepository.delete(post);
//    }

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


    public List<SocialSpace> getAllSocialSpaces() {
        return socialSpaceRepository.findAll();
    }

    public List<SocialSpace> getUserSocialSpaces(Integer userId) {
        return roleRepository.findSocialSpacesByUserId(userId);
    }

    public List<SocialSpaceMemberDTO> getSocialSpaceMembers(Integer socialSpaceId) {
        return roleRepository.findSocialSpaceMembers(socialSpaceId);
    }

    public List<SocialSpaceDTO> getOwnedSocialSpaces(Integer userId) {
        return roleRepository.findSocialSpacesOwnedByUser(userId);
    }

    public List<SocialSpaceMemberDTO> searchSocialSpaceMembers(Integer userId,String search) {
        return roleRepository.searchSocialSpaceMembers(userId,search);
    }

}