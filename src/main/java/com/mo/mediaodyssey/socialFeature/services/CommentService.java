package com.mo.mediaodyssey.socialFeature.services;

import com.mo.mediaodyssey.socialFeature.models.Comment;
import com.mo.mediaodyssey.socialFeature.models.DTO.CommentDTO;
import com.mo.mediaodyssey.socialFeature.repositories.CommentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepo;

    @PersistenceContext
    private EntityManager entityManager;

    public CommentService(CommentRepository commentRepo) {
        this.commentRepo = commentRepo;
    }

    // CREATE
    @Transactional
    public void createComment(Long userId, Long postId, String content) {
        Comment comment = new Comment(postId, userId, null, content, false);
        commentRepo.save(comment);
        entityManager.flush();
    }

    @Transactional
    public void replyToComment(Long userId, Long parentCommentId, String content) {
        Comment parent = commentRepo.findById(parentCommentId)
                .orElseThrow(() -> new IllegalStateException("Parent comment not found"));

        Comment reply = new Comment(parent.getPostId(), userId, parentCommentId, content, false);
        commentRepo.save(reply);
        entityManager.flush();
    }

    // EDIT
    @Transactional
    public void updateCommentContent(Long commentId, String newContent) {
        Comment comment = commentRepo.findById(commentId)
                .orElseThrow(() -> new IllegalStateException("Comment not found"));

        if (comment.isDeleted()) {
            throw new RuntimeException("Deleted comments cannot be edited");
        }

        comment.setContent(newContent);
        commentRepo.save(comment);
        entityManager.flush();
    }

    // DELETE (soft)
    @Transactional
    public void softDeleteComment(Long commentId) {
        Comment comment = commentRepo.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (comment.isDeleted()) {
            throw new RuntimeException("Comment already deleted");
        }

        comment.setDeleted(true);
        comment.setContent("[deleted]");

        commentRepo.save(comment);
        entityManager.flush();
    }

    // READ
    public Long getParentPostId(Long commentId) {
        return commentRepo.findById(commentId)
                .orElseThrow(() -> new IllegalStateException("Comment not found"))
                .getPostId();
    }

    @Transactional(readOnly = true)
    public List<CommentDTO> getCommentsWithDepth(Long postId) {
        List<CommentDTO> comments = commentRepo.findCommentsWithUser(postId);

        Map<Long, List<CommentDTO>> repliesMap = comments.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(CommentDTO::getParentId));

        List<CommentDTO> result = new ArrayList<>();

        for (CommentDTO c : comments) {
            if (c.getParentId() == null) {
                appendReplies(c, 0, repliesMap, result);
            }
        }
        return result;
    }

    private void appendReplies(CommentDTO comment, int depth,
                               Map<Long, List<CommentDTO>> repliesMap,
                               List<CommentDTO> result) {
        comment.setDepth(depth);
        result.add(comment);

        List<CommentDTO> replies = repliesMap.getOrDefault(comment.getId(), Collections.emptyList());
        for (CommentDTO reply : replies) {
            appendReplies(reply, depth + 1, repliesMap, result);
        }
    }
}