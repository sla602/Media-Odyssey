package com.mo.mediaodyssey.socialFeature.services;

import com.mo.mediaodyssey.socialFeature.enums.RoleType;
import com.mo.mediaodyssey.socialFeature.models.SocialSpaceRole;
import com.mo.mediaodyssey.socialFeature.models.DTO.PostDTO;
import com.mo.mediaodyssey.socialFeature.models.Post;
import com.mo.mediaodyssey.socialFeature.repositories.PostRepository;
import com.mo.mediaodyssey.socialFeature.repositories.SocialSpaceRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PostService {

    private final PostRepository postRepo;
    private final SocialSpaceRoleRepository roleRepo;

    public PostService(PostRepository postRepo, SocialSpaceRoleRepository roleRepo){
        this.postRepo = postRepo;
        this.roleRepo = roleRepo;
    }

    //Create a new post in a socialSpace by a user.

    public void createPost(Integer userId, Integer communityId, String title, String content){
        Post post = new Post(communityId, userId, title, content);
        postRepo.save(post);
    }

    // Delete a post. Only the author, a community OWNER, or MODERATOR can delete it.
    @Transactional
    public void deletePost(Integer actingUserId, Integer postId) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new IllegalStateException("Post not found"));

        // Author can delete
        if(post.getAuthorId().equals(actingUserId)){
            postRepo.delete(post);
            return;
        }

        // Check if acting user is a moderator or owner in the community
        RoleType role = roleRepo.findByUserIdAndSocialSpaceId(actingUserId, post.getSocialSpaceId())
                .map(SocialSpaceRole::getRoleType)
                .orElse(null);

        if(role == RoleType.OWNER || role == RoleType.MODERATOR){
            postRepo.delete(post);
            return;
        }

        throw new SecurityException("Not allowed to delete this post");
    }


    public List<PostDTO> getPostsBySocialSpaceId(Integer communityId){
        return postRepo.findPostsWithUserByCommunityId(communityId);
    }


    public void updatePost(Integer postId, String newTitle, String newContent) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new IllegalStateException("Post not found"));

        post.setTitle(newTitle);
        post.setContent(newContent);

        postRepo.save(post);
    }

    public Post getPostById(Integer postId){
        return postRepo.findById(postId)
                .orElseThrow(() -> new IllegalStateException("Post not found"));
    }


//    public List<Post> getPostsByAuthorId(Integer authorId){
//        return postRepo.findByAuthorId(authorId);
//    }
//
//
//    public boolean existsByIdAndAuthorId(Integer postId, Integer authorId){
//        return postRepo.existsByIdAndAuthorId(postId, authorId);
//    }
}