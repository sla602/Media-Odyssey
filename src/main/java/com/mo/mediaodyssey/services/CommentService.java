package com.mo.mediaodyssey.services;

import com.mo.mediaodyssey.models.Comment;
import com.mo.mediaodyssey.repositories.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepo;

    public CommentService(CommentRepository commentRepo){
        this.commentRepo = commentRepo;
    }

    public void createComment(Integer userId, Integer postId, String content){
        Comment comment = new Comment(postId, userId, null, content);
        commentRepo.save(comment);
    }

    public void replyToComment(Integer userId, Integer parentCommentId, String content){
        Comment parent = commentRepo.findById(parentCommentId)
                .orElseThrow(() -> new IllegalStateException("Parent comment not found"));

        Comment reply = new Comment(parent.getPostId(), userId, parentCommentId, content);
        commentRepo.save(reply);
    }

    public Integer getParentPostId(Integer commentId){
        return commentRepo.findById(commentId)
                .orElseThrow(() -> new IllegalStateException("Comment not found"))
                .getPostId();
    }


    public List<CommentWithDepth> getCommentsWithDepth(Integer postId){
        List<Comment> allComments = commentRepo.findByPostIdOrderByCreatedAtAsc(postId);
        Map<Integer, List<Comment>> repliesMap = allComments.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Comment::getParentId));

        List<CommentWithDepth> result = new ArrayList<>();


        for (Comment c : allComments){
            if (c.getParentId() == null){
                appendCommentWithReplies(c, 0, repliesMap, result);
            }
        }

        return result;
    }

    private void appendCommentWithReplies(Comment comment, int depth,
                                          Map<Integer, List<Comment>> repliesMap,
                                          List<CommentWithDepth> result){
        result.add(new CommentWithDepth(comment, depth));
        List<Comment> replies = repliesMap.getOrDefault(comment.getId(), Collections.emptyList());
        for (Comment reply : replies){
            appendCommentWithReplies(reply, depth + 1, repliesMap, result);
        }
    }

    public static class CommentWithDepth {
        private final Comment comment;
        private final int depth;

        public CommentWithDepth(Comment comment, int depth){
            this.comment = comment;
            this.depth = depth;
        }

        public Comment getComment() { return comment; }
        public int getDepth() { return depth; }
    }
}