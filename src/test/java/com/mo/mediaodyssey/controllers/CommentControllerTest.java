package com.mo.mediaodyssey.controllers;

import com.mo.mediaodyssey.socialFeature.controllers.CommentController;
import com.mo.mediaodyssey.socialFeature.services.CommentService;
import com.mo.mediaodyssey.socialFeature.services.SocialService;
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

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock private CommentService commentService;
    @Mock private SocialService socialService;
    @Mock private Authentication authentication;
    @Mock private RedirectAttributes redirectAttributes;

    @InjectMocks
    private CommentController commentController;

    private User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(1L);

        when(authentication.getPrincipal()).thenReturn(user);
    }


    // CREATE COMMENT

    @Test
    void createComment_success() {
        String result = commentController.addComment(
                5, "Hello",
                null, redirectAttributes, authentication
        );

        verify(socialService).createComment(1, 5, "Hello");
        verify(redirectAttributes).addFlashAttribute("success", "Comment added!");

        assertThat(result).isEqualTo("redirect:/posts/5");
    }

    @Test
    void createComment_failure() {
        doThrow(new RuntimeException("Not allowed"))
                .when(socialService).createComment(1, 5, "Hello");

        String result = commentController.addComment(
                5, "Hello",
                null, redirectAttributes, authentication
        );

        verify(redirectAttributes).addFlashAttribute("error", "Not allowed");
    }



    // EDIT COMMENT
    @Test
    void editComment_success() {
        when(commentService.getParentPostId(2)).thenReturn(5);

        String result = commentController.editComment(
                2, "Updated",
                null, redirectAttributes, authentication
        );

        verify(socialService).editComment(1, 2, "Updated");
        verify(redirectAttributes).addFlashAttribute("success", "Comment edited successfully");

        assertThat(result).isEqualTo("redirect:/posts/5");
    }

    // DELETE COMMENT
    @Test
    void deleteComment_success() {
        when(commentService.getParentPostId(2)).thenReturn(5);

        String result = commentController.deleteComment(
                2, null, redirectAttributes, authentication
        );

        verify(socialService).deleteComment(1, 2);
        verify(redirectAttributes).addFlashAttribute("success", "Comment deleted successfully");

        assertThat(result).isEqualTo("redirect:/posts/5");
    }

    @Test
    void deleteComment_failure() {
        when(commentService.getParentPostId(2)).thenReturn(5);

        doThrow(new RuntimeException("Cannot delete"))
                .when(socialService).deleteComment(1, 2);

        String result = commentController.deleteComment(
                2, null, redirectAttributes, authentication
        );

        verify(redirectAttributes).addFlashAttribute("error", "Cannot delete");

        assertThat(result).isEqualTo("redirect:/posts/5");
    }
}