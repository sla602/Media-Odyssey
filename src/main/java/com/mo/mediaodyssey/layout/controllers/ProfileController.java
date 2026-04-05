package com.mo.mediaodyssey.layout.controllers;

import com.mo.mediaodyssey.layout.models.Profile;
import com.mo.mediaodyssey.layout.repositories.ProfileRepository;
import com.mo.mediaodyssey.layout.services.AvatarService;
import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.socialFeature.models.DTO.FriendRequestDTO;
import com.mo.mediaodyssey.socialFeature.services.FriendshipService;
import com.mo.mediaodyssey.socialFeature.services.FriendshipService.FriendStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Handles the user profile page (username/description/pronouns, save button,
 * add-friend button) and the friends.html page with its four sections.
 */
@Controller
public class ProfileController {

    private final ProfileRepository profileRepo;
    private final FriendshipService friendshipService;

    public ProfileController(ProfileRepository profileRepo,
                             FriendshipService friendshipService) {
        this.profileRepo = profileRepo;
        this.friendshipService = friendshipService;
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private Profile getOrCreateProfile(Long userId) {
        return profileRepo.findByUserId(userId)
                .orElseGet(() -> profileRepo.save(new Profile(userId)));
    }

    // ─── Own profile page ────────────────────────────────────────────

    /**
     * Show the current user's own profile (editable, has Save button).
     */
    @GetMapping("/profile")
    public String viewOwnProfile(Authentication authentication, Model model) {
        User user = (User) authentication.getPrincipal();
        Profile profile = getOrCreateProfile(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("profile", profile);
        model.addAttribute("isOwnProfile", true);
        model.addAttribute("avatarUrl", AvatarService.avatarGenerate(user.getId()));

        return "boardsLayout/userSide/userProfile";
    }

    /**
     * Show another user's profile (read-only, has Add Friend button).
     */
    @GetMapping("/profile/{userId}")
    public String viewProfile(@PathVariable Long userId,
                              Authentication authentication,
                              Model model) {
        User viewer = (User) authentication.getPrincipal();
        Profile profile = getOrCreateProfile(userId);

        boolean isOwn = viewer.getId().equals(userId);
        FriendStatus status = friendshipService.getStatusBetween(viewer.getId(), userId);

        model.addAttribute("user", viewer);
        model.addAttribute("profile", profile);
        model.addAttribute("profileUserId", userId);
        model.addAttribute("isOwnProfile", isOwn);
        model.addAttribute("friendStatus", status.name());
        model.addAttribute("avatarUrl", AvatarService.avatarGenerate(userId));

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
        Profile profile = getOrCreateProfile(user.getId());

        profile.setUsername(username);
        profile.setDescription(description);
        profile.setPronouns(pronouns);
        profileRepo.save(profile);

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

    // ─── Friends page (4 sections) ───────────────────────────────────

    /**
     * friends.html — four sections:
     *   1) Friends
     *   2) Incoming friend requests
     *   3) Suggested friends (people in the same boards as the user)
     *   4) Pending (outgoing) requests — user can cancel these
     */
    @GetMapping("/friends")
    public String viewFriendsPage(Authentication authentication, Model model) {
        User user = (User) authentication.getPrincipal();
        Long userId = user.getId();

        List<User> friends = friendshipService.getFriends(userId);
        List<FriendRequestDTO> incoming = friendshipService.getIncomingRequests(userId);
        List<User> suggested = friendshipService.getSuggestedFriends(userId);
        List<FriendRequestDTO> pending = friendshipService.getOutgoingRequests(userId);

        model.addAttribute("user", user);
        model.addAttribute("currentUserId", userId);
        model.addAttribute("friends", friends);
        model.addAttribute("incomingRequests", incoming);
        model.addAttribute("suggestedFriends", suggested);
        model.addAttribute("pendingRequests", pending);

        return "friends-view/friends";
    }

    @PostMapping("/friends/requests/{requestId}/accept")
    public String acceptRequest(@PathVariable Long requestId,
                                RedirectAttributes redirectAttributes) {
        try {
            friendshipService.acceptFriendRequest(requestId);
            redirectAttributes.addFlashAttribute("successMessage", "Friend request accepted.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/friends";
    }

    @PostMapping("/friends/requests/{requestId}/reject")
    public String rejectRequest(@PathVariable Long requestId,
                                RedirectAttributes redirectAttributes) {
        try {
            friendshipService.cancelFriendRequest(requestId);
            redirectAttributes.addFlashAttribute("successMessage", "Friend request rejected.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/friends";
    }

    /** Cancel an outgoing pending request. */
    @PostMapping("/friends/requests/{requestId}/cancel")
    public String cancelRequest(@PathVariable Long requestId,
                                RedirectAttributes redirectAttributes) {
        try {
            friendshipService.cancelFriendRequest(requestId);
            redirectAttributes.addFlashAttribute("successMessage", "Friend request cancelled.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/friends";
    }

    /** Send a friend request from the suggested list on friends.html. */
    @PostMapping("/friends/add/{targetUserId}")
    public String addFriendFromList(@PathVariable Long targetUserId,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        User viewer = (User) authentication.getPrincipal();
        try {
            friendshipService.sendFriendRequest(viewer.getId(), targetUserId);
            redirectAttributes.addFlashAttribute("successMessage", "Friend request sent.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/friends";
    }

    @PostMapping("/friends/{friendUserId}/remove")
    public String removeFriend(@PathVariable Long friendUserId,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        User viewer = (User) authentication.getPrincipal();
        try {
            friendshipService.removeFriend(viewer.getId(), friendUserId);
            redirectAttributes.addFlashAttribute("successMessage", "Friend removed.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/friends";
    }
}