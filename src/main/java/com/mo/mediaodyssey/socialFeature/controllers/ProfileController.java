package com.mo.mediaodyssey.socialFeature.controllers;

import com.mo.mediaodyssey.layout.models.Profile;
import com.mo.mediaodyssey.layout.services.AvatarService;
import com.mo.mediaodyssey.layout.services.ProfileService;
import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.socialFeature.services.FriendshipService;
import com.mo.mediaodyssey.socialFeature.services.FriendshipService.FriendStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles the user profile page (username/description/pronouns, save button,
 * add-friend button) and the friends.html page with its four sections.
 */
@Controller
public class ProfileController {

    private final FriendshipService friendshipService;
    private final ProfileService profileService;

    public ProfileController(FriendshipService friendshipService, ProfileService profileService) {
        this.friendshipService = friendshipService;
        this.profileService = profileService;
    }


    // ─── Own profile page ────────────────────────────────────────────

    /**
     * Show the current user's own profile (editable, has Save button).
     */
    @GetMapping("/profile")
    public String viewOwnProfile(Authentication authentication, Model model) {
        User user = (User) authentication.getPrincipal();
        Profile profile = profileService.getOrCreateProfile(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("profile", profile);
        model.addAttribute("isOwnProfile", true);
        model.addAttribute("avatarUrl", AvatarService.avatarGenerate(user.getId()));
        model.addAttribute("recentActivity", profileService.buildRecentActivity(user.getId(), user.getId()));

        return "boardsLayout/userSide/userProfile";
    }

    /**
     * Show another user's profile (read-only, has Add Friend button).
     * userId is profileUserId
     */
    @GetMapping("/profile/{userId}")
    public String viewProfile(@PathVariable Long userId,
                              Authentication authentication,
                              Model model) {
        User viewer = (User) authentication.getPrincipal();
        Profile profile = profileService.getOrCreateProfile(userId);

        boolean isOwn = viewer.getId().equals(userId);
        FriendStatus status = friendshipService.getStatusBetween(viewer.getId(), userId);

        model.addAttribute("user", viewer);
        model.addAttribute("profile", profile);
        model.addAttribute("profileUserId", userId);
        model.addAttribute("isOwnProfile", isOwn);
        model.addAttribute("friendStatus", status.name());
        model.addAttribute("avatarUrl", AvatarService.avatarGenerate(userId));
        model.addAttribute("recentActivity", profileService.buildRecentActivity(userId, viewer.getId()));

        return "boardsLayout/userSide/userProfile";
    }

    /**
     * Save the current user's profile (Save button on userProfile.html).
     */
    @PostMapping("/profile/save")
    public String saveProfile(@RequestParam(required = false) String username,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false) String pronouns,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        User user = (User) authentication.getPrincipal();
        profileService.updateProfile(user.getId(), username, description, pronouns);

        redirectAttributes.addFlashAttribute("successMessage", "Profile saved.");
        return "redirect:/profile";
    }

    // ─── Friend request actions ──────────────────────────────────────

    /**
     * Send a friend request (the Add Friend button on userProfile.html).
     */
    @PostMapping("/profile/{targetUserId}/add-friend")
    public String addFriend(@PathVariable Long targetUserId,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        User viewer = (User) authentication.getPrincipal();
        try {
            friendshipService.sendFriendRequest(viewer.getId(), targetUserId);
            redirectAttributes.addFlashAttribute("successMessage", "Friend request sent.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/profile/" + targetUserId;
    }
}