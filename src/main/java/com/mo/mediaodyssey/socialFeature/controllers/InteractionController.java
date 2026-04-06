package com.mo.mediaodyssey.socialFeature.controllers;

import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.socialFeature.models.DTO.PostDTO;
import com.mo.mediaodyssey.socialFeature.services.CommentService;
import com.mo.mediaodyssey.socialFeature.services.ModerationService;
import com.mo.mediaodyssey.socialFeature.services.PostService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Handles CRUD for posts and comments inside a board. Previously lived
 * in BoardsController; moved here to separate content interactions from
 * board display logic.
 *
 * URL shapes match the originals so templates don't need updating.
 *
 * Related controllers:
 *  - ModerationController: ban / promote / ownership (mod actions)
 *  - BoardsController: board display and creation
 */
@Controller
@RequestMapping("/boards/display/{boardId}")
public class InteractionController {

    private final PostService postService;
    private final CommentService commentService;
    private final ModerationService moderationService;

    public InteractionController(PostService postService,
                                 CommentService commentService, ModerationService moderationService) {
        this.postService = postService;
        this.commentService = commentService;
        this.moderationService = moderationService;
    }

    // ─── Posts ───────────────────────────────────────────────────────

    /**
     * Called by the frontend when the user clicks the POST button on boardDisplay.html.
     */
    @GetMapping("/posts")
    @ResponseBody
    public List<PostDTO> getBoardPosts(@PathVariable Long boardId) {
        return postService.getPostsByBoardId(boardId);
    }

    /**
     * Create a new post in the board, then redirect back to the board page.
     */
    @PostMapping("/posts")
    public String createPost(@PathVariable Long boardId,
                             @RequestParam String title,
                             @RequestParam String content,
                             Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        postService.createPost(user.getId(), boardId, title, content);
        return "redirect:/boards/display/" + boardId;
    }

    @PostMapping("/posts/{postId}/edit")
    public String editPost(@PathVariable Long boardId,
                           @PathVariable Long postId,
                           @RequestParam String title,
                           @RequestParam String content,
                           Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        postService.updatePost(postId, title, content);
        return "redirect:/boards/display/" + boardId;
    }

    @PostMapping("/posts/{postId}/delete")
    public String deletePost(@PathVariable Long boardId,
                             @PathVariable Long postId,
                             Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        postService.deletePost(user.getId(), postId);
        return "redirect:/boards/display/" + boardId;
    }

    // ─── Comments ────────────────────────────────────────────────────

    /** Create a top-level comment on a post (no parent). */
    @PostMapping("/posts/{postId}/comments")
    public String createComment(@PathVariable Long boardId,
                                @PathVariable Long postId,
                                @RequestParam String content,
                                Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        commentService.createComment(user.getId(), postId, content);
        return "redirect:/boards/display/" + boardId + "?view=post&postId=" + postId;
    }

    @PostMapping("/posts/{postId}/comments/{commentId}/edit")
    public String editComment(@PathVariable Long boardId,
                              @PathVariable Long postId,
                              @PathVariable Long commentId,
                              @RequestParam String content,
                              Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        commentService.updateCommentContent(commentId, content);
        return "redirect:/boards/display/" + boardId + "?view=post&postId=" + postId;
    }

    /** Reply to an existing comment. */
    @PostMapping("/posts/{postId}/comments/{commentId}/reply")
    public String replyToComment(@PathVariable Long boardId,
                                 @PathVariable Long postId,
                                 @PathVariable Long commentId,
                                 @RequestParam String content,
                                 Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        commentService.replyToComment(user.getId(), commentId, content);
        return "redirect:/boards/display/" + boardId + "?view=post&postId=" + postId;
    }

    @PostMapping("/posts/{postId}/comments/{commentId}/delete")
    public String deleteComment(@PathVariable Long boardId,
                                @PathVariable Long postId,
                                @PathVariable Long commentId) {
        commentService.softDeleteComment(commentId);
        return "redirect:/boards/display/" + boardId + "?view=post&postId=" + postId;
    }

    // ─── Reporting (any member can report) ───────────────────────────

    @PostMapping("/posts/{postId}/report")
    public String reportPost(@PathVariable Long boardId,
                             @PathVariable Long postId,
                             @RequestParam String reason,
                             @RequestParam Long contentAuthorId,
                             Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        moderationService.reportPost(boardId, postId, user.getId(), contentAuthorId, reason);
        return "redirect:/boards/display/" + boardId;
    }

    @PostMapping("/posts/{postId}/comments/{commentId}/report")
    public String reportComment(@PathVariable Long boardId,
                                @PathVariable Long postId,
                                @PathVariable Long commentId,
                                @RequestParam String reason,
                                @RequestParam Long contentAuthorId,
                                Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        moderationService.reportComment(boardId, commentId, user.getId(), contentAuthorId, reason);
        return "redirect:/boards/display/" + boardId + "?view=post&postId=" + postId;
    }
}
