package com.mo.mediaodyssey.socialFeature.controllers;

import com.mo.mediaodyssey.layout.repositories.BoardsRepository;
import com.mo.mediaodyssey.socialFeature.models.BoardInvite;
import com.mo.mediaodyssey.socialFeature.repositories.ProfileRepository;
import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.shared.services.CurrentAccountService;
import com.mo.mediaodyssey.socialFeature.models.DTO.FriendRequestDTO;
import com.mo.mediaodyssey.socialFeature.services.BoardInviteService;
import com.mo.mediaodyssey.socialFeature.services.FriendshipService;
import com.mo.mediaodyssey.socialFeature.services.ProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

/**
 * Handles the friends.html page and all friend-request actions:
 * - viewing the four sections (friends, incoming, suggested, pending)
 * - sending / accepting / rejecting / cancelling requests
 * - removing an existing friend
 */
@Controller
public class FriendshipController {

    private final FriendshipService friendshipService;
    private final ProfileRepository profileRepo;
    private final ProfileService profileService;
    private final BoardInviteService boardInviteService;
    private final BoardsRepository boardsRepo;
    private final CurrentAccountService currentAccountService;

    public FriendshipController(FriendshipService friendshipService,
            ProfileRepository profileRepo, ProfileService profileService, BoardInviteService boardInviteService,
            BoardsRepository boardsRepo, CurrentAccountService currentAccountService) {
        this.friendshipService = friendshipService;
        this.profileRepo = profileRepo;
        this.profileService = profileService;
        this.boardInviteService = boardInviteService;
        this.boardsRepo = boardsRepo;
        this.currentAccountService = currentAccountService;
    }

    // ─── Helpers ─────────────────────────────────────────────────────
    /**
     * Returns null if the user has a username, or a redirect string if they don't.
     * Used as a guard at the top of all gated endpoints.
     */
    private String requireUsername(Long userId, RedirectAttributes redirectAttributes) {
        if (!profileService.hasUsername(userId)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "You need to set a username before using the friends page.");
            return "redirect:/profile";
        }
        return null;
    }

    /**
     * Look up the display username for a user id.
     * Falls back to "User #<id>" if no profile / no username is set.
     */
    private String displayNameFor(Long userId) {
        if (userId == null)
            return "Unknown";
        return profileRepo.findByUserId(userId)
                .map(p -> p.getUsername())
                .filter(name -> name != null && !name.isBlank())
                .orElse("User #" + userId);
    }

    /**
     * Build a map of userId -> username for a collection of user ids,
     * so the template can render names without calling the repo per row.
     */
    private Map<Long, String> buildUsernameMap(Collection<Long> userIds) {
        Map<Long, String> map = new HashMap<>();
        if (userIds == null)
            return map;
        for (Long id : userIds) {
            if (id != null && !map.containsKey(id)) {
                map.put(id, displayNameFor(id));
            }
        }
        return map;
    }

    // ─── Friends page ────────────────────────────────────────────────

    /**
     * friends.html — four sections:
     * 1) Friends
     * 2) Incoming friend requests
     * 3) Suggested friends (people in the same boards as the user)
     * 4) Pending (outgoing) requests — user can cancel these
     */
    @GetMapping("/friendsTab")
    public String viewFriendsPage(@RequestParam(value = "search", required = false) String search,
            Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        User user = currentAccountService.getCurrentAccount(authentication);
        Long userId = user.getId();

        // Gate: username required to use the friends page

        if (!profileService.hasUsername(userId)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "You need to set a username before using the friends page.");
            return "redirect:/profile";
        }

        List<User> friends = friendshipService.getFriends(userId);
        List<FriendRequestDTO> incoming = friendshipService.getIncomingRequests(userId);
        List<User> suggested = friendshipService.getSuggestedFriends(userId);
        List<FriendRequestDTO> pending = friendshipService.getOutgoingRequests(userId);

        // Search results (empty list if no query)
        List<FriendshipService.FriendSearchResult> searchResults = friendshipService.searchUsersByUsername(userId,
                search);

        // Pending board invites addressed to this user
        List<BoardInvite> boardInvites = boardInviteService.getInvitesForUser(userId);

        // Collect every user id we'll display so we can batch-load usernames.
        Set<Long> idsToResolve = new HashSet<>();
        friends.forEach(f -> idsToResolve.add(f.getId()));
        suggested.forEach(s -> idsToResolve.add(s.getId()));
        incoming.forEach(r -> idsToResolve.add(r.getOtherUserId()));
        pending.forEach(r -> idsToResolve.add(r.getOtherUserId()));

        Map<Long, String> inviteBoardNames = new HashMap<>();
        for (BoardInvite inv : boardInvites) {
            boardsRepo.findById(inv.getBoardId())
                    .ifPresent(b -> inviteBoardNames.put(inv.getBoardId(), b.getBoard_name()));
            idsToResolve.add(inv.getInviterUserId());
        }

        // Single username map covering friends, suggested, incoming,
        // pending, and invite-inviters.
        Map<Long, String> usernames = buildUsernameMap(idsToResolve);

        model.addAttribute("user", user);
        model.addAttribute("currentUserId", userId);
        model.addAttribute("friends", friends);
        model.addAttribute("incomingRequests", incoming);
        model.addAttribute("suggestedFriends", suggested);
        model.addAttribute("pendingRequests", pending);
        model.addAttribute("usernames", usernames);
        model.addAttribute("searchQuery", search);
        model.addAttribute("searchResults", searchResults);
        model.addAttribute("boardInvites", boardInvites);
        model.addAttribute("inviteBoardNames", inviteBoardNames);

        return "friends-view/friends";
    }

    // ─── Friend request actions ──────────────────────────────────────

    @PostMapping("/friends/requests/{requestId}/accept")
    public String acceptRequest(@PathVariable Long requestId, Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User viewer = currentAccountService.getCurrentAccount(authentication);
        String guard = requireUsername(viewer.getId(), redirectAttributes);
        if (guard != null)
            return guard;
        try {
            friendshipService.acceptFriendRequest(requestId);
            redirectAttributes.addFlashAttribute("successMessage", "Friend request accepted.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/friends";
    }

    @PostMapping("/friends/requests/{requestId}/reject")
    public String rejectRequest(@PathVariable Long requestId, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        User user = currentAccountService.getCurrentAccount(authentication);
        Long userId = user.getId();

        if (!profileService.hasUsername(userId)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "You need to set a username before using the friends page.");
            return "redirect:/profile";
        }
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
    public String cancelRequest(@PathVariable Long requestId, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        User user = currentAccountService.getCurrentAccount(authentication);
        Long userId = user.getId();

        if (!profileService.hasUsername(userId)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "You need to set a username before using the friends page.");
            return "redirect:/profile";
        }
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
        User viewer = currentAccountService.getCurrentAccount(authentication);

        if (!profileService.hasUsername(viewer.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "You need to set a username before using the friends page.");
            return "redirect:/profile";
        }
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
        User viewer = currentAccountService.getCurrentAccount(authentication);
        if (!profileService.hasUsername(viewer.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "You need to set a username before using the friends page.");
            return "redirect:/profile";
        }
        try {
            friendshipService.removeFriend(viewer.getId(), friendUserId);
            redirectAttributes.addFlashAttribute("successMessage", "Friend removed.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/friends";
    }
}