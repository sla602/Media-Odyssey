package com.mo.mediaodyssey.socialFeature.controllers;

import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.auth.repository.UserRepository;
import com.mo.mediaodyssey.socialFeature.enums.RoleType;
import com.mo.mediaodyssey.socialFeature.models.SocialSpaceRole;
import com.mo.mediaodyssey.socialFeature.models.DTO.CommentDTO;
import com.mo.mediaodyssey.socialFeature.models.Post;
import com.mo.mediaodyssey.socialFeature.repositories.SocialSpaceRoleRepository;
import com.mo.mediaodyssey.socialFeature.services.CommentService;
import com.mo.mediaodyssey.socialFeature.services.PostService;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;
    private final CommentService commentService;
    private final SocialSpaceRoleRepository roleRepo;
    private final UserRepository userRepo;

    public PostController(PostService postService, CommentService commentService,
                          SocialSpaceRoleRepository socialSpaceRoleRepo, UserRepository userRepo) {
        this.postService = postService;
        this.commentService = commentService;
        this.roleRepo = socialSpaceRoleRepo;
        this.userRepo = userRepo;
    }

    @GetMapping("/create/{socialSpaceId}")
    public String showCreatePostForm(@PathVariable Integer socialSpaceId, Model model) {
        model.addAttribute("socialSpaceId", socialSpaceId);
        return "posts/create-post";
    }

    @PostMapping("/create/{socialSpaceId}")
    public String createPost(@PathVariable Integer socialSpaceId,
            @RequestParam String title,
            @RequestParam String content,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        // TODO: changed to use id from /auth. Please clean up in future.
        User user = (User) authentication.getPrincipal();
        Long userId = user.getId();

        postService.createPost(userId.intValue(), socialSpaceId, title, content);
        redirectAttributes.addFlashAttribute("success", "Post created successfully");
        return "redirect:/socialSpaces/" + socialSpaceId;
    }

    @PostMapping("/{postId}/edit")
    public String updatePost(
            @PathVariable Integer postId,
            @RequestParam String title,
            @RequestParam String content,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        Integer currentUserId = user.getId().intValue();

        Post post = postService.getPostById(postId);

        // Only author is allowed to edit
        if (!post.getAuthorId().equals(currentUserId)) {
            redirectAttributes.addFlashAttribute("error", "You can only edit your own posts");
            return "redirect:/posts/" + postId;
        }

        try {
            postService.updatePost(postId, title, content);
            redirectAttributes.addFlashAttribute("success", "Post updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update post: " + e.getMessage());
        }

        return "redirect:/posts/" + postId;
    }

    @PostMapping("/{postId}/delete")
    public String deletePost(@PathVariable Integer postId,
            RedirectAttributes redirectAttributes, Authentication authentication) {

        // TODO: changed to use id from /auth. Please clean up in future.
        User user = (User) authentication.getPrincipal();
        Long userId = user.getId();

        try {
            Post post = postService.getPostById(postId);
            postService.deletePost(userId.intValue(), postId);
            redirectAttributes.addFlashAttribute("success", "Post deleted successfully");
            return "redirect:/socialSpaces/" + post.getSocialSpaceId();
        } catch (RuntimeException e) {
            Post post = postService.getPostById(postId);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/socialSpaces/" + post.getSocialSpaceId();
        }
    }

    @GetMapping("/{postId}")
    public String viewPost(@PathVariable Integer postId, Model model, Authentication authentication) {

        Post post = postService.getPostById(postId);

        String username = userRepo.findById((long) post.getAuthorId())
                .map(User::getUsername)
                .orElse("Unknown");

        List<CommentDTO> comments = commentService.getCommentsWithDepth(postId);

        User currentUser = (User) authentication.getPrincipal();
        Integer currentUserId = currentUser.getId().intValue();

        RoleType currentUserRole = roleRepo.findByUserIdAndSocialSpaceId(currentUserId, post.getSocialSpaceId())
                .map(SocialSpaceRole::getRoleType)
                .orElse(null);

        model.addAttribute("post", post);
        model.addAttribute("postUsername", username);
        model.addAttribute("comments", comments);
        model.addAttribute("currentUserId", currentUserId);
        model.addAttribute("currentUserRole", currentUserRole);
        model.addAttribute("postId", postId);
        model.addAttribute("socialSpaceId", post.getSocialSpaceId());

        return "posts/view-post";
    }
}