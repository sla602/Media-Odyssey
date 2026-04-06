package com.mo.mediaodyssey.socialFeature.services;

import com.mo.mediaodyssey.socialFeature.enums.RoleType;
import com.mo.mediaodyssey.layout.models.BoardRole;
import com.mo.mediaodyssey.socialFeature.models.DTO.PostDTO;
import com.mo.mediaodyssey.socialFeature.models.Post;
import com.mo.mediaodyssey.layout.repositories.BoardRoleRepository;
import com.mo.mediaodyssey.socialFeature.repositories.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PostService {

    private final PostRepository postRepo;
    private final BoardRoleRepository roleRepo;

    public PostService(PostRepository postRepo, BoardRoleRepository roleRepo) {
        this.postRepo = postRepo;
        this.roleRepo = roleRepo;
    }

    public void createPost(Long userId, Long boardId, String title, String content) {
        Post post = new Post(boardId, userId, title, content, false);
        postRepo.save(post);
    }

    @Transactional
    public void deletePost(Long actingUserId, Long postId) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new IllegalStateException("Post not found"));


        // Moderator / Owner can soft-delete any post
        RoleType role = roleRepo.findByUserIdAndBoardId(actingUserId, post.getBoardId())
                .map(BoardRole::getRoleType)
                .orElse(null);

        if (role == RoleType.OWNER || role == RoleType.MODERATOR) {
            post.setDeleted(true);
            post.setContent("[deleted]");
            postRepo.save(post);
            return;
        }

        throw new SecurityException("Not allowed to delete this post");
    }

    public List<PostDTO> getPostsByBoardId(Long boardId) {
        return postRepo.findPostsWithUserByBoardId(boardId);
    }

    public void updatePost(Long postId, String newTitle, String newContent) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new IllegalStateException("Post not found"));

        post.setTitle(newTitle);
        post.setContent(newContent);
        postRepo.save(post);
    }

    public Post getPostById(Long postId) {
        return postRepo.findById(postId)
                .orElseThrow(() -> new IllegalStateException("Post not found"));
    }
}