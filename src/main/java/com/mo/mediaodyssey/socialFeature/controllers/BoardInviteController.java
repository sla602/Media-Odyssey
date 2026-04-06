package com.mo.mediaodyssey.socialFeature.controllers;

import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.socialFeature.services.BoardInviteService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles private-board invites: sending (from the board page),
 * accepting / declining (from the friends page), and cancelling
 * (either side).
 */
@Controller
public class BoardInviteController {

    private final BoardInviteService inviteService;

    public BoardInviteController(BoardInviteService inviteService) {
        this.inviteService = inviteService;
    }

    /**
     * Send an invite from the board page. Posted by the invite-picker modal.
     */
    @PostMapping("/boards/display/{boardId}/invite")
    public String sendInvite(@PathVariable Long boardId,
                             @RequestParam Long inviteeUserId,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        User user = (User) authentication.getPrincipal();
        try {
            inviteService.sendInvite(user.getId(), inviteeUserId, boardId);
            redirectAttributes.addFlashAttribute("successMessage", "Invite sent.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/boards/display/" + boardId;
    }

    /**
     * Accept a board invite. Posted from the Invitations section on friends.html.
     */
    @PostMapping("/invites/{inviteId}/accept")
    public String acceptInvite(@PathVariable Long inviteId,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        User user = (User) authentication.getPrincipal();
        try {
            inviteService.acceptInvite(inviteId, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Invite accepted.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/friends";
    }

    /**
     * Decline a board invite (invitee side).
     */
    @PostMapping("/invites/{inviteId}/decline")
    public String declineInvite(@PathVariable Long inviteId,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        User user = (User) authentication.getPrincipal();
        try {
            inviteService.declineInvite(inviteId, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Invite declined.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/friends";
    }

    /**
     * Cancel an invite you sent (inviter side, or any board staff cleaning up).
     */
    @PostMapping("/boards/display/{boardId}/invite/{inviteId}/cancel")
    public String cancelInvite(@PathVariable Long boardId,
                               @PathVariable Long inviteId,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        User user = (User) authentication.getPrincipal();
        try {
            inviteService.cancelInvite(inviteId, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Invite cancelled.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/boards/display/" + boardId;
    }
}