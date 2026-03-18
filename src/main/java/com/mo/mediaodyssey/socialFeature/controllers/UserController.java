package com.mo.mediaodyssey.socialFeature.controllers;

import com.mo.mediaodyssey.auth.model.User;
import com.mo.mediaodyssey.socialFeature.models.Community;
import com.mo.mediaodyssey.socialFeature.models.DTO.FriendRequestDTO;
import com.mo.mediaodyssey.socialFeature.services.CommuService;
import com.mo.mediaodyssey.socialFeature.services.FriendshipService;
import com.mo.mediaodyssey.socialFeature.services.PermissionService;
// import com.mo.mediaodyssey.services.UserService;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/users")
public class UserController {

    // private final UserService userService;
    private final CommuService commuService;
    private final FriendshipService friendshipService;
    private final PermissionService permissionService;

    public UserController(
            // UserService userService,
            CommuService commuService,
            FriendshipService friendshipService,
            PermissionService permissionService) {
        // this.userService = userService;
        this.commuService = commuService;
        this.friendshipService = friendshipService;
        this.permissionService = permissionService;
    }

    // @Deprecated
    // @GetMapping("/login")
    // public String showLoginPage() { return "users/login"; }

    // @Deprecated
    // @GetMapping("/register")
    // public String showRegisterPage() { return "users/register"; }

    // @Deprecated
    // @PostMapping("/register")
    // public String registerUser(@RequestParam String username,
    // @RequestParam String email,
    // @RequestParam String password,
    // RedirectAttributes redirectAttributes) {
    // try {
    // userService.registerUser(username, email, password);
    // redirectAttributes.addFlashAttribute("successMessage", "Account created
    // successfully");
    // return "redirect:/users/login";
    // } catch (Exception e) {
    // redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    // return "redirect:/users/register";
    // }
    // }

    // @Deprecated
    // @PostMapping("/login")
    // public String login(@RequestParam String username,
    // @RequestParam String password,
    // HttpSession session,
    // RedirectAttributes redirectAttributes) {
    // try {
    // User user = userService.loginUser(username, password);
    // session.setAttribute("userId", user.getId()); // stored as Long
    // return "redirect:/users/dashboard";
    // } catch (Exception e) {
    // redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    // return "redirect:/users/login";
    // }
    // }

//    @GetMapping("/dashboard")
//    public String dashboard(HttpSession session, Model model, Authentication authentication) {
//        // Long userId = (Long) session.getAttribute("userId");
//        // if (userId == null) return "redirect:/users/login";
//
//        // TODO: changed to use id from /auth. Please clean up in future.
//        User user = (User) authentication.getPrincipal();
//        Long userId = user.getId();
//
//        List<Community> communities = commuService.getUserCommunities(userId.intValue());
//        model.addAttribute("myCommunities", communities);
//
//        Map<Integer, Boolean> moderationMap = new HashMap<>();
//        for (Community community : communities) {
//            moderationMap.put(community.getId(),
//                    permissionService.canModerateCommunity(userId.intValue(), community.getId()));
//        }
//        model.addAttribute("moderationMap", moderationMap);
//
//        List<User> friends = friendshipService.getFriends(userId.intValue());
//        model.addAttribute("friends", friends);
//
//        List<FriendRequestDTO> requests = friendshipService.getIncomingRequests(userId.intValue());
//        model.addAttribute("requests", requests);
//
//        List<User> suggested = friendshipService.getSuggestedFriends(userId);
//        model.addAttribute("suggested", suggested);
//
//        return "users/dashboard";
//    }
//
//    @PostMapping("/friends/request")
//    public String sendFriendRequest(@RequestParam Integer friendId, HttpSession session,
//            RedirectAttributes redirectAttributes, Authentication authentication) {
//        // Long userId = (Long) session.getAttribute("userId");
//        // if (userId == null)
//        // return "redirect:/users/login";
//
//        // TODO: changed to use id from /auth. Please clean up in future.
//        User user = (User) authentication.getPrincipal();
//        Long userId = user.getId();
//
//        friendshipService.sendFriendRequest(userId.intValue(), friendId);
//        redirectAttributes.addFlashAttribute("success", "Friend request sent");
//        return "redirect:/users/dashboard";
//    }
//
//    @PostMapping("/friends/accept")
//    public String acceptFriendRequest(@RequestParam Integer requestId, HttpSession session,
//            Authentication authentication) {
//        // Long userId = (Long) session.getAttribute("userId");
//        // if (userId == null)
//        // return "redirect:/users/login";
//
//        friendshipService.acceptFriendRequest(requestId);
//        return "redirect:/users/dashboard";
//    }
}