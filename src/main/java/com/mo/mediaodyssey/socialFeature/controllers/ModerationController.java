package com.mo.mediaodyssey.socialFeature.controllers;

import com.mo.mediaodyssey.layout.models.BoardRole;
import com.mo.mediaodyssey.layout.repositories.BoardRoleRepository;
import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.shared.services.CurrentAccountService;
import com.mo.mediaodyssey.socialFeature.enums.RoleType;
import com.mo.mediaodyssey.socialFeature.services.ModerationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handles all moderation actions for a board — reports, member moderation
 * (ban / unban / promote / demote), and owner-level actions (transfer, delete).
 *
 * Previously these endpoints lived in BoardsController; moved here so that
 * board CRUD / display logic is separated from moderation concerns.
 *
 * URL shape is preserved exactly, so friends of templates and redirects
 * don't need to change.
 */
@Controller
@RequestMapping("/boards/display/{boardId}/moderation")
public class ModerationController {

    private final ModerationService moderationService;
    private final BoardRoleRepository boardRoleRepository;
    private final CurrentAccountService currentAccountService;

    public ModerationController(ModerationService moderationService,
            BoardRoleRepository boardRoleRepository, CurrentAccountService currentAccountService) {
        this.moderationService = moderationService;
        this.boardRoleRepository = boardRoleRepository;
        this.currentAccountService = currentAccountService;
    }

    // ─── Helper ──────────────────────────────────────────────────────

    /** Look up the given user's role in the given board, defaulting to NONE. */
    private RoleType getUserRoleType(Long userId, Long boardId) {
        return boardRoleRepository.findByUserIdAndBoardId(userId, boardId)
                .map(BoardRole::getRoleType)
                .orElse(RoleType.NONE);
    }

    // ─── Reports ─────────────────────────────────────────────────────

    @PostMapping("/reports/{reportId}/dismiss")
    public String dismissReport(@PathVariable Long boardId,
            @PathVariable Long reportId) {
        moderationService.dismissReport(reportId);
        return "redirect:/boards/display/" + boardId + "?view=moderation&modTab=reports";
    }

    @PostMapping("/reports/{reportId}/delete-content")
    public String deleteReportedContent(@PathVariable Long boardId,
            @PathVariable Long reportId) {
        moderationService.deleteReportedContent(reportId);
        return "redirect:/boards/display/" + boardId + "?view=moderation&modTab=reports";
    }

    @PostMapping("/reports/{reportId}/ban")
    public String banFromReport(@PathVariable Long boardId,
            @PathVariable Long reportId) {
        moderationService.banFromReport(reportId);
        return "redirect:/boards/display/" + boardId + "?view=moderation&modTab=reports";
    }

    // ─── Member moderation ───────────────────────────────────────────

    @PostMapping("/members/{userId}/ban")
    public String banMember(@PathVariable Long boardId,
            @PathVariable Long userId) {
        moderationService.banMember(userId, boardId);
        return "redirect:/boards/display/" + boardId + "?view=moderation&modTab=members";
    }

    @PostMapping("/members/{userId}/unban")
    public String unbanMember(@PathVariable Long boardId,
            @PathVariable Long userId) {
        moderationService.unbanMember(userId, boardId);
        return "redirect:/boards/display/" + boardId + "?view=moderation&modTab=members";
    }

    @PostMapping("/members/{userId}/promote")
    public String promoteMember(@PathVariable Long boardId,
            @PathVariable Long userId) {
        moderationService.promoteMember(userId, boardId);
        return "redirect:/boards/display/" + boardId + "?view=moderation&modTab=members";
    }

    @PostMapping("/members/{userId}/demote")
    public String demoteModerator(@PathVariable Long boardId,
            @PathVariable Long userId) {
        moderationService.demoteModerator(userId, boardId);
        return "redirect:/boards/display/" + boardId + "?view=moderation&modTab=members";
    }

    // ─── Ownership actions ───────────────────────────────────────────

    @PostMapping("/ownership/transfer")
    public String transferOwnership(@PathVariable Long boardId,
            @RequestParam Long newOwnerId,
            Authentication authentication) {
        User user = currentAccountService.getCurrentAccount(authentication);
        moderationService.transferOwnership(user.getId(), newOwnerId, boardId);
        return "redirect:/boards/display/" + boardId;
    }

    @PostMapping("/ownership/delete")
    public String hardDeleteBoard(@PathVariable Long boardId,
            Authentication authentication) {
        User user = currentAccountService.getCurrentAccount(authentication);
        RoleType role = getUserRoleType(user.getId(), boardId);
        if (!role.isOwner()) {
            throw new SecurityException("Only the owner can delete the board");
        }
        moderationService.hardDeleteBoard(boardId);
        return "redirect:/";
    }
}