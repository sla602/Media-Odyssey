package com.mo.mediaodyssey.socialFeature.controllers;

import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.auth.repository.UserRepository;
import com.mo.mediaodyssey.socialFeature.enums.RoleType;
import com.mo.mediaodyssey.socialFeature.models.CommunityRole;
import com.mo.mediaodyssey.socialFeature.models.DTO.CommentDTO;
import com.mo.mediaodyssey.socialFeature.models.Post;
import com.mo.mediaodyssey.socialFeature.repositories.CommunityRoleRepository;
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
    private final CommunityRoleRepository communityRoleRepo;
    private final UserRepository userRepo;

    public PostController(PostService postService, CommentService commentService,
            CommunityRoleRepository communityRoleRepo, UserRepository userRepo) {
        this.postService = postService;
        this.commentService = commentService;
        this.communityRoleRepo = communityRoleRepo;
        this.userRepo = userRepo;
    }

    @GetMapping("/create/{communityId}")
    public String showCreatePostForm(@PathVariable Integer communityId, Model model) {
        model.addAttribute("communityId", communityId);
        return "posts/create-post";
    }

    @PostMapping("/create/{communityId}")
    public String createPost(@PathVariable Integer communityId,
            @RequestParam String title,
            @RequestParam String content,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        // TODO: changed to use id from /auth. Please clean up in future.
        User user = (User) authentication.getPrincipal();
        Long userId = user.getId();

        postService.createPost(userId.intValue(), communityId, title, content);
        redirectAttributes.addFlashAttribute("success", "Post created successfully");
        return "redirect:/communities/" + communityId;
    }

    @GetMapping("/{postId}/edit")
    public String showEditPostForm(@PathVariable Integer postId, HttpSession session,
            Model model, RedirectAttributes redirectAttributes, Authentication authentication) {

        // TODO: changed to use id from /auth. Please clean up in future.
        User user = (User) authentication.getPrincipal();
        Long userId = user.getId();

        Post post = postService.getPostById(postId);
        if (!post.getAuthorId().equals(userId.intValue())) {
            redirectAttributes.addFlashAttribute("error", "You can only edit your own posts");
            return "redirect:/posts/" + postId;
        }
        model.addAttribute("post", post);
        return "posts/view-post";
    }

    @PostMapping("/{postId}/delete")
    public String deletePost(@PathVariable Integer postId, HttpSession session,
            RedirectAttributes redirectAttributes, Authentication authentication) {

        // TODO: changed to use id from /auth. Please clean up in future.
        User user = (User) authentication.getPrincipal();
        Long userId = user.getId();

        try {
            Post post = postService.getPostById(postId);
            postService.deletePost(userId.intValue(), postId);
            redirectAttributes.addFlashAttribute("success", "Post deleted successfully");
            return "redirect:/communities/" + post.getCommunityId();
        } catch (RuntimeException e) {
            Post post = postService.getPostById(postId);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/communities/" + post.getCommunityId();
        }
    }

    @GetMapping("/{postId}")
    public String viewPost(@PathVariable Integer postId, Model model, HttpSession session,
            Authentication authentication) {
        Post post = postService.getPostById(postId);
        String username = userRepo.findById((long) post.getAuthorId())
                .map(User::getUsername)
                .orElse("Unknown");

        List<CommentDTO> comments = commentService.getCommentsWithDepth(postId);

        // TODO: changed to use id from /auth. Please clean up in future.
        User user = (User) authentication.getPrincipal();
        Long currentUserId = user.getId();

        RoleType currentUserRole = null;

        if (currentUserId != null) {
            Optional<CommunityRole> roleOpt = communityRoleRepo
                    .findByUserIdAndCommunityId(currentUserId.intValue(), post.getCommunityId());
            if (roleOpt.isPresent()) {
                currentUserRole = roleOpt.get().getRoleType();
            }
        }

        model.addAttribute("post", post);
        model.addAttribute("postUsername", username);
        model.addAttribute("comments", comments);
        model.addAttribute("currentUserId", currentUserId.intValue());
        model.addAttribute("currentUserRole", currentUserRole);

        return "posts/view-post";
    }
}