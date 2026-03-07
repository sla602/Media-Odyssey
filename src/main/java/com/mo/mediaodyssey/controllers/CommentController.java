package com.mo.mediaodyssey.controllers;

import com.mo.mediaodyssey.models.Comment;
import com.mo.mediaodyssey.services.CommentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService){
        this.commentService = commentService;
    }

    // Add a top-level comment
    @PostMapping("/post/{postId}")
    public String addComment(@PathVariable Integer postId,
                             @RequestParam String content,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {

        Integer userId = (Integer) session.getAttribute("userId");
        if(userId == null){
            redirectAttributes.addFlashAttribute("error","Please login to comment");
            return "redirect:/users/login";
        }

        commentService.createComment(userId, postId, content);

        // Redirect back to post page
        return "redirect:/posts/" + postId;
    }

    // Reply to an existing comment
    @PostMapping("/reply/{commentId}")
    public String replyComment(@PathVariable Integer commentId,
                               @RequestParam String content,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {

        Integer userId = (Integer) session.getAttribute("userId");
        if(userId == null){
            redirectAttributes.addFlashAttribute("error","Please login to reply");
            return "redirect:/users/login";
        }

        commentService.replyToComment(userId, commentId, content);

        // Find the parent post id
        Integer postId = commentService.getParentPostId(commentId);

        // Redirect back to the post view
        return "redirect:/posts/" + postId;
    }


//    @GetMapping("/post/{postId}")
//    public String getPostComments(@PathVariable Integer postId, Model model){
//        List<Comment> comments = commentService.getAllCommentsForPost(postId);
//        model.addAttribute("comments", comments);
//        return "posts/view-post :: comment-list"; // optional fragment
//    }
}