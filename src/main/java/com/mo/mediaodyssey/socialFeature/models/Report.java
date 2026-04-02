package com.mo.mediaodyssey.socialFeature.models;

import com.mo.mediaodyssey.socialFeature.enums.ReportTargetType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "reports",
        indexes = {
                @Index(name = "idx_report_board", columnList = "board_id"),
                @Index(name = "idx_report_post", columnList = "post_id"),
                @Index(name = "idx_report_comment", columnList = "comment_id"),
                @Index(name = "idx_report_reporter", columnList = "reported_by_user_id"),
                @Index(name = "idx_report_author", columnList = "content_author_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_report_unique",
                        columnNames = {"post_id", "comment_id", "reported_by_user_id"})
        }
)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private ReportTargetType targetType;

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "reported_by_user_id", nullable = false)
    private Long reportedByUserId;

    @Column(name = "content_author_id", nullable = false)
    private Long contentAuthorId;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(nullable = false)
    private boolean resolved = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // ===== CONSTRUCTOR WITH VALIDATION =====
    public Report(Long boardId,
                  ReportTargetType targetType,
                  Long postId,
                  Long commentId,
                  Long reportedByUserId,
                  Long contentAuthorId,
                  String reason) {

        if (targetType == ReportTargetType.POST) {
            if (postId == null || commentId != null) {
                throw new IllegalArgumentException("POST report must have postId only");
            }
        }

        if (targetType == ReportTargetType.COMMENT) {
            if (commentId == null || postId != null) {
                throw new IllegalArgumentException("COMMENT report must have commentId only");
            }
        }

        this.boardId = boardId;
        this.targetType = targetType;
        this.postId = postId;
        this.commentId = commentId;
        this.reportedByUserId = reportedByUserId;
        this.contentAuthorId = contentAuthorId;
        this.reason = reason;
    }

    public Report() {

    }

    // ===== FACTORY METHODS (CLEAN USAGE) =====

    public static Report forPost(Long boardId,
                                 Long postId,
                                 Long reportedByUserId,
                                 Long contentAuthorId,
                                 String reason) {

        return new Report(boardId,
                ReportTargetType.POST,
                postId,
                null,
                reportedByUserId,
                contentAuthorId,
                reason);
    }

    public static Report forComment(Long boardId,
                                    Long commentId,
                                    Long reportedByUserId,
                                    Long contentAuthorId,
                                    String reason) {

        return new Report(boardId,
                ReportTargetType.COMMENT,
                null,
                commentId,
                reportedByUserId,
                contentAuthorId,
                reason);
    }


    public Long getId() { return id; }
    public Long getBoardId() { return boardId; }
    public ReportTargetType getTargetType() { return targetType; }
    public Long getPostId() { return postId; }
    public Long getCommentId() { return commentId; }
    public Long getReportedByUserId() { return reportedByUserId; }
    public Long getContentAuthorId() { return contentAuthorId; }
    public String getReason() { return reason; }
    public boolean isResolved() { return resolved; }
    public Instant getCreatedAt() { return createdAt; }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }
}