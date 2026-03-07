package com.mo.mediaodyssey.controllers;

import com.mo.mediaodyssey.models.Comment;
import com.mo.mediaodyssey.models.Post;
import com.mo.mediaodyssey.services.CommentService;
import com.mo.mediaodyssey.services.PostService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;
    private final CommentService commentService;

    public PostController(PostService postService, CommentService commentService){
        this.postService = postService;
        this.commentService = commentService;
    }

    // Show the Create Post Form
    @GetMapping("/create/{communityId}")
    public String showCreatePostForm(@PathVariable Integer communityId, Model model){
        model.addAttribute("communityId", communityId);
        return "posts/create-post"; // <-- this points to templates/posts/create-post.html
    }

    // Handle Post Submission
    @PostMapping("/create/{communityId}")
    public String createPost(@PathVariable Integer communityId,
                             @RequestParam String title,
                             @RequestParam String content,
                             HttpSession session,
                             RedirectAttributes redirectAttributes){

        Integer userId = (Integer) session.getAttribute("userId");
        if(userId == null){
            redirectAttributes.addFlashAttribute("error","Please login to create a post");
            return "redirect:/users/login";
        }

        postService.createPost(userId, communityId, title, content);
        redirectAttributes.addFlashAttribute("success","Post created successfully");

        // Redirect back to the community page
        return "redirect:/communities/" + communityId;
    }

    // Delete a post
//    @PostMapping("/{postId}/delete")
//    public String deletePost(@PathVariable Integer postId,
//                             HttpSession session,
//                             RedirectAttributes redirectAttributes){
//
//        Integer userId = (Integer) session.getAttribute("userId");
//        if(userId == null){
//            redirectAttributes.addFlashAttribute("error","Please login to delete posts");
//            return "redirect:/users/login";
//        }
//
//        try {
//            Post post = postService.getPostById(postId);
//            postService.deletePost(userId, postId);
//            redirectAttributes.addFlashAttribute("success","Post deleted successfully");
//            return "redirect:/posts/community/" + post.getCommunityId();
//        } catch (SecurityException e){
//            redirectAttributes.addFlashAttribute("error", e.getMessage());
//            Post post = postService.getPostById(postId);
//            return "redirect:/posts/community/" + post.getCommunityId();
//        }
//    }

    //view post
    @GetMapping("/{postId}")
    public String viewPost(@PathVariable Integer postId, Model model){
        Post post = postService.getPostById(postId);
        List<CommentService.CommentWithDepth> comments = commentService.getCommentsWithDepth(postId);

        model.addAttribute("post", post);
        model.addAttribute("comments", comments); // list of CommentWithDepth

        return "posts/view-post";
    }

}