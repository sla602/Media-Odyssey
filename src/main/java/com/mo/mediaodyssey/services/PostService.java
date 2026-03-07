package com.mo.mediaodyssey.services;

import com.mo.mediaodyssey.enums.RoleType;
import com.mo.mediaodyssey.models.CommunityRole;
import com.mo.mediaodyssey.models.Post;
import com.mo.mediaodyssey.repositories.CommunityRoleRepository;
import com.mo.mediaodyssey.repositories.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PostService {

    private final PostRepository postRepo;
    private final CommunityRoleRepository roleRepo;

    public PostService(PostRepository postRepo, CommunityRoleRepository roleRepo){
        this.postRepo = postRepo;
        this.roleRepo = roleRepo;
    }

    //Create a new post in a community by a user.

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
        RoleType role = roleRepo.findByUserIdAndCommunityId(actingUserId, post.getCommunityId())
                .map(CommunityRole::getRoleType)
                .orElse(null);

        if(role == RoleType.OWNER || role == RoleType.MODERATOR){
            postRepo.delete(post);
            return;
        }

        throw new SecurityException("Not allowed to delete this post");
    }


    public List<Post> getPostsByCommunityId(Integer communityId){
        return postRepo.findByCommunityId(communityId);
    }


    public Post getPostById(Integer postId){
        return postRepo.findById(postId)
                .orElseThrow(() -> new IllegalStateException("Post not found"));
    }


    public List<Post> getPostsByAuthorId(Integer authorId){
        return postRepo.findByAuthorId(authorId);
    }


    public boolean existsByIdAndAuthorId(Integer postId, Integer authorId){
        return postRepo.existsByIdAndAuthorId(postId, authorId);
    }
}