package com.mo.mediaodyssey.controllers;

import com.mo.mediaodyssey.socialFeature.controllers.PostController;
import com.mo.mediaodyssey.socialFeature.models.Post;
import com.mo.mediaodyssey.socialFeature.services.PostService;
import com.mo.mediaodyssey.socialFeature.services.CommentService;
import com.mo.mediaodyssey.socialFeature.repositories.SocialSpaceRoleRepository;
import com.mo.mediaodyssey.auth.repository.UserRepository;
import com.mo.mediaodyssey.auth.model.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    @Mock private PostService postService;
    @Mock private CommentService commentService;
    @Mock private SocialSpaceRoleRepository roleRepo;
    @Mock private UserRepository userRepo;
    @Mock private Authentication authentication;
    @Mock private RedirectAttributes redirectAttributes;

    @InjectMocks
    private PostController postController;

    private User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(1L);

        when(authentication.getPrincipal()).thenReturn(user);
    }


    // CREATE POST
    @Test
    void createPost_success() {
        String result = postController.createPost(
                10, "Title", "Content",
                null, redirectAttributes, authentication
        );

        verify(postService).createPost(1, 10, "Title", "Content");
        verify(redirectAttributes).addFlashAttribute("success", "Post created successfully");

        assertThat(result).isEqualTo("redirect:/socialSpaces/10");
    }


    // EDIT POST
    @Test
    void editPost_success() {
        Post post = new Post(10, 1, "Old", "Old", false);

        when(postService.getPostById(5)).thenReturn(post);

        String result = postController.updatePost(
                5, "New", "Updated",
                redirectAttributes, authentication
        );

        verify(postService).updatePost(5, "New", "Updated");
        verify(redirectAttributes).addFlashAttribute("success", "Post updated successfully");

        assertThat(result).isEqualTo("redirect:/posts/5");
    }

    @Test
    void editPost_notAuthor() {
        Post post = new Post(10, 99, "Old", "Old", false);

        when(postService.getPostById(5)).thenReturn(post);

        String result = postController.updatePost(
                5, "New", "Updated",
                redirectAttributes, authentication
        );

        verify(postService, never()).updatePost(any(), any(), any());
        verify(redirectAttributes).addFlashAttribute("error", "You can only edit your own posts");

        assertThat(result).isEqualTo("redirect:/posts/5");
    }


    // DELETE POST (soft delete)

    @Test
    void deletePost_success() {
        Post post = new Post(10, 1, "Title", "Content", false);

        when(postService.getPostById(5)).thenReturn(post);

        String result = postController.deletePost(
                5, redirectAttributes, authentication
        );

        verify(postService).deletePost(1, 5);
        verify(redirectAttributes).addFlashAttribute("success", "Post deleted successfully");

        assertThat(result).isEqualTo("redirect:/socialSpaces/10");
    }

    @Test
    void deletePost_failure() {
        Post post = new Post(10, 1, "Title", "Content", false);

        when(postService.getPostById(5)).thenReturn(post);
        doThrow(new RuntimeException("Not allowed"))
                .when(postService).deletePost(1, 5);

        String result = postController.deletePost(
                5, redirectAttributes, authentication
        );

        verify(redirectAttributes).addFlashAttribute("error", "Not allowed");

        assertThat(result).isEqualTo("redirect:/socialSpaces/10");
    }
}
