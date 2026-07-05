package com.mo.mediaodyssey.socialFeature.services;

import com.mo.mediaodyssey.layout.models.BoardRole;
import com.mo.mediaodyssey.layout.repositories.BoardRoleRepository;
import com.mo.mediaodyssey.layout.repositories.BoardsRepository;
import com.mo.mediaodyssey.socialFeature.enums.RoleType;
import com.mo.mediaodyssey.socialFeature.models.Report;
import com.mo.mediaodyssey.socialFeature.repositories.CommentRepository;
import com.mo.mediaodyssey.socialFeature.repositories.PostRepository;
import com.mo.mediaodyssey.socialFeature.repositories.ReportRepository;
import com.mo.mediaodyssey.shared.util.SqlLikeEscaper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ModerationService {

    private final ReportRepository reportRepo;
    private final BoardRoleRepository roleRepo;
    private final PostRepository postRepo;
    private final CommentRepository commentRepo;
    private final BoardsRepository boardsRepo;

    public ModerationService(ReportRepository reportRepo,
                             BoardRoleRepository roleRepo,
                             PostRepository postRepo,
                             CommentRepository commentRepo,
                             BoardsRepository boardsRepo) {
        this.reportRepo = reportRepo;
        this.roleRepo = roleRepo;
        this.postRepo = postRepo;
        this.commentRepo = commentRepo;
        this.boardsRepo = boardsRepo;
    }

    // ─── Reports ─────────────────────────────────────────────────────

    public void reportPost(Long boardId, Long postId, Long reportedByUserId, Long contentAuthorId, String reason) {
        Report report = Report.forPost(boardId, postId, reportedByUserId, contentAuthorId, reason);
        reportRepo.save(report);
    }

    public void reportComment(Long boardId, Long commentId, Long reportedByUserId, Long contentAuthorId, String reason) {
        Report report = Report.forComment(boardId, commentId, reportedByUserId, contentAuthorId, reason);
        reportRepo.save(report);
    }

    public List<Report> getUnresolvedReports(Long boardId) {
        return reportRepo.findByBoardIdAndResolvedFalseOrderByCreatedAtDesc(boardId);
    }

    public long getUnresolvedReportCount(Long boardId) {
        return reportRepo.countByBoardIdAndResolvedFalse(boardId);
    }

    public void dismissReport(Long reportId) {
        Report report = reportRepo.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        report.setResolved(true);
        reportRepo.save(report);
    }

    public void deleteReportedContent(Long reportId) {
        Report report = reportRepo.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        switch (report.getTargetType()) {
            case COMMENT -> commentRepo.findById(report.getCommentId()).ifPresent(comment -> {
                comment.setDeleted(true);
                comment.setContent("[deleted]");
                commentRepo.save(comment);
            });
            case POST -> postRepo.findById(report.getPostId()).ifPresent(post -> {
                post.setDeleted(true);
                post.setContent("[deleted]");
                post.setTitle("[deleted]");
                postRepo.save(post);
            });
        }

        report.setResolved(true);
        reportRepo.save(report);
    }

    // ─── Ban ─────────────────────────────────────────────────────────

    public void banMember(Long targetUserId, Long boardId) {
        BoardRole role = roleRepo.findByUserIdAndBoardId(targetUserId, boardId)
                .orElseThrow(() -> new RuntimeException("User not in board"));

        role.setRoleType(RoleType.BANNED);
        roleRepo.save(role);
    }

    public void banFromReport(Long reportId) {
        Report report = reportRepo.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        banMember(report.getContentAuthorId(), report.getBoardId());
        deleteReportedContent(reportId);
    }

    public void unbanMember(Long targetUserId, Long boardId) {
        BoardRole role = roleRepo.findByUserIdAndBoardId(targetUserId, boardId)
                .orElseThrow(() -> new RuntimeException("User not in board"));


        role.setRoleType(RoleType.MEMBER);
        roleRepo.save(role);
    }

    // ─── Members ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BoardRole> getBoardMembers(Long boardId) {
        return roleRepo.findMembersByBoardId(boardId);
    }

    @Transactional(readOnly = true)
    public List<BoardRole> searchBoardMembers(Long boardId, String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return roleRepo.searchMembersByBoardId(boardId, SqlLikeEscaper.escape(query));
    }

    // ─── Role Management (Owner only) ────────────────────────────────

    public void promoteMember(Long targetUserId, Long boardId) {
        BoardRole role = roleRepo.findByUserIdAndBoardId(targetUserId, boardId)
                .orElseThrow(() -> new RuntimeException("User not in board"));

        if (!role.getRoleType().isMember()) {
            throw new RuntimeException("Can only promote members");
        }

        role.setRoleType(RoleType.MODERATOR);
        roleRepo.save(role);
    }

    public void demoteModerator(Long targetUserId, Long boardId) {
        BoardRole role = roleRepo.findByUserIdAndBoardId(targetUserId, boardId)
                .orElseThrow(() -> new RuntimeException("User not in board"));

        if (!role.getRoleType().isModerator()) {
            throw new RuntimeException("User is not a moderator");
        }

        role.setRoleType(RoleType.MEMBER);
        roleRepo.save(role);
    }

    // ─── Ownership ───────────────────────────────────────────────────

    public void transferOwnership(Long currentOwnerId, Long newOwnerId, Long boardId) {
        BoardRole ownerRole = roleRepo.findByUserIdAndBoardId(currentOwnerId, boardId)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        BoardRole targetRole = roleRepo.findByUserIdAndBoardId(newOwnerId, boardId)
                .orElseThrow(() -> new RuntimeException("Target user not in board"));

        ownerRole.setRoleType(RoleType.MODERATOR);
        targetRole.setRoleType(RoleType.OWNER);

        roleRepo.save(ownerRole);
        roleRepo.save(targetRole);
    }

    /**
     * Hard deletes the board and ALL its content permanently.
     * No soft delete thus everything is removed from the database.
     */
    public void hardDeleteBoard(Long boardId) {
        // Delete all comments for all posts in the board
        List<Long> postIds = postRepo.findPostIdsByBoardId(boardId);
        if (!postIds.isEmpty()) {
            commentRepo.deleteByPostIdIn(postIds);
        }

        // Delete all posts
        postRepo.deleteByBoardId(boardId);

        // Delete all reports
        reportRepo.deleteByBoardId(boardId);

        // Delete all roles
        roleRepo.deleteByBoardId(boardId);

        // Delete the board
        boardsRepo.deleteById(boardId);
    }
}
