package com.mo.mediaodyssey.controllers;

import com.mo.mediaodyssey.models.Community;
import com.mo.mediaodyssey.models.Post;
import com.mo.mediaodyssey.services.CommunityService;

import com.mo.mediaodyssey.services.PostService;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/communities")
public class CommunityController {

    private final CommunityService communityService;
    private final PostService postService;

    public CommunityController(
            CommunityService communityService,
            PostService postService) {
        this.communityService = communityService;
        this.postService = postService;
    }


    // Show create community page
    @GetMapping("/create")
    public String showCreateCommunityPage() {
        return "communities/create-community";
    }

    // Create community
    @PostMapping("/create")
    public String createCommunity(
            @RequestParam String name,
            @RequestParam String description,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/users/login";
        }

        communityService.createCommunity(userId, name, description);
        redirectAttributes.addFlashAttribute("success", "Community Created Successfully");
        return "redirect:/communities";
    }

    // Show all communities
    @GetMapping
    public String listCommunities(Model model) {
        List<Community> communities = communityService.getAllCommunities();
        model.addAttribute("communities", communities);
        return "communities/join-community";
    }


    // View a community
    @GetMapping("/{id}")
    public String viewCommunity(@PathVariable Integer id, Model model){

        Community community = communityService.getCommunityById(id);
        Integer memberCount = communityService.getMemberCount(id);
        List<Post> posts = postService.getPostsByCommunityId(id);


        model.addAttribute("community",community);
        model.addAttribute("memberCount",memberCount);
        model.addAttribute("posts",posts);

        return "communities/view-community";
    }




    @PostMapping("/{communityId:\\d+}/join")
    public String joinCommunity(
            @PathVariable Integer communityId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/users/login";
        }

        try {
            communityService.joinCommunity(userId, communityId);
            redirectAttributes.addFlashAttribute("success", "You joined the community!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/communities/" + communityId;
    }




    // Promote member to moderator
    @PostMapping("/{communityId:\\d+}/promote")
    public String promote(
            @PathVariable Integer communityId,
            @RequestParam Integer actingUserId,
            @RequestParam Integer targetUserId,
            RedirectAttributes redirectAttributes) {

        communityService.promoteToModerator(
                actingUserId,
                targetUserId,
                communityId
        );

        redirectAttributes.addFlashAttribute(
                "success",
                "Member Promoted Successfully"
        );

        return "redirect:/communities/" + communityId;
    }
}

