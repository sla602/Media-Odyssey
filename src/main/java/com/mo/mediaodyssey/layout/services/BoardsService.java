package com.mo.mediaodyssey.layout.services;

import com.mo.mediaodyssey.auth.model.User;
import com.mo.mediaodyssey.layout.models.Boards;
import com.mo.mediaodyssey.layout.DTO.BoardDTO;
import com.mo.mediaodyssey.layout.repositories.BoardsRepository;
import com.mo.mediaodyssey.socialFeature.enums.RoleType;
import com.mo.mediaodyssey.layout.models.BoardRole;
import com.mo.mediaodyssey.socialFeature.models.Comment;
import com.mo.mediaodyssey.socialFeature.models.Post;
import com.mo.mediaodyssey.layout.repositories.BoardRoleRepository;
import com.mo.mediaodyssey.socialFeature.repositories.CommentRepository;
import com.mo.mediaodyssey.socialFeature.repositories.PostRepository;
import com.mo.mediaodyssey.socialFeature.services.CommentService;
import com.mo.mediaodyssey.socialFeature.services.PermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Replaces both the old BoardsService and SocialService.
 * Boards is now the single "space" concept — all role management,
 * post/comment operations, and membership live here.
 */
@Service
@Transactional
public class BoardsService {

    private final BoardsRepository boardsRepo;
    private final BoardRoleRepository roleRepo;
    private final PostRepository postRepo;
    private final CommentRepository commentRepo;
    private final PermissionService permissionService;
    private final CommentService commentService;

    public BoardsService(BoardsRepository boardsRepo,
                        BoardRoleRepository roleRepo,
                        PostRepository postRepo,
                        CommentRepository commentRepo,
                        PermissionService permissionService,
                        CommentService commentService) {
        this.boardsRepo = boardsRepo;
        this.roleRepo = roleRepo;
        this.postRepo = postRepo;
        this.commentRepo = commentRepo;
        this.permissionService = permissionService;
        this.commentService = commentService;
    }

    // ─── Board CRUD ──────────────────────────────────────────────────

    public Boards createBoard(User creator, String name, String description, String boardType) {
        Boards board = new Boards();
        board.setBoard_name(name);
        board.setBoard_description(description);
        board.setBoard_type(boardType);
        board.setUser(creator);
        boardsRepo.save(board);

        // Creator becomes OWNER
        BoardRole ownerRole = new BoardRole(creator.getId(), board.getId(), RoleType.OWNER);
        roleRepo.save(ownerRole);

        return board;
    }

    public void editBoard(Long actingUserId, Long boardId, String newName, String newDescription, String newBoardType) {
        permissionService.canEditBoard(actingUserId, boardId);

        Boards board = boardsRepo.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found"));

        board.setBoard_name(newName);
        board.setBoard_description(newDescription);
        board.setBoard_type(newBoardType);
        boardsRepo.save(board);
    }

    /**
     * Returns all boards the user has joined (any role except BANNED).
     * This replaces findBoardsByUser() for the homepage display.
     */
    public List<Boards> getJoinedBoards(Long userId) {
        List<BoardRole> roles = roleRepo.findByUserIdAndRoleTypeNot(userId, RoleType.BANNED);

        return roles.stream()
                .map(role -> boardsRepo.findById(role.getBoardId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
    }

    public void deleteBoard(Long actingUserId, Long boardId) {
        permissionService.canDeleteBoard(actingUserId, boardId);

        Boards board = boardsRepo.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found"));

        List<BoardRole> roles = roleRepo.findByBoardId(boardId);
        roleRepo.deleteAll(roles);

        boardsRepo.delete(board);
    }

    public Optional<Boards> findBoardById(Long id) {
        return boardsRepo.findById(id);
    }

    public List<Boards> findAllBoards() {
        return boardsRepo.findAll();
    }

    public List<Boards> findBoardsByUser(User user) {
        return boardsRepo.findByUser(user);
    }

    // ─── Membership ──────────────────────────────────────────────────

    public void joinBoard(Long userId, Long boardId) {
        Optional<BoardRole> existing = roleRepo.findByUserIdAndBoardId(userId, boardId);

        if (existing.isPresent()) {
            RoleType role = existing.get().getRoleType();
            if (role.isOwner()) {
                throw new IllegalStateException("You are already the owner of this board.");
            } else if (role.isModerator()) {
                throw new IllegalStateException("You are already a moderator in this board.");
            } else {
                throw new IllegalStateException("You are already a member of this board.");
            }
        }

        BoardRole newRole = new BoardRole(userId, boardId, RoleType.MEMBER);
        roleRepo.save(newRole);
    }

    public void leaveBoard(Long userId, Long boardId) {
        BoardRole role = roleRepo.findByUserIdAndBoardId(userId, boardId)
                .orElseThrow(() -> new RuntimeException("User not a member"));

        if (role.getRoleType().isOwner()) {
            throw new RuntimeException("Owner cannot leave board. Transfer ownership first.");
        }

        roleRepo.deleteByUserIdAndBoardId(userId, boardId);
    }

    public boolean isUserInBoard(Long userId, Long boardId) {
        return roleRepo.existsByUserIdAndBoardId(userId, boardId);
    }

    public Long getMemberCount(Long boardId) {
        return roleRepo.countByBoardId(boardId);
    }





//    // ─── Comment operations (delegated, permission-checked) ─────────
//
//    public void createComment(Long userId, Long postId, String content) {
//        Post post = postRepo.findById(postId)
//                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));
//
//        permissionService.canCreateComment(userId, post.getBoardId());
//        commentService.createComment(userId, postId, content);
//    }
//
//    public void replyToComment(Long userId, Long parentCommentId, String content) {
//        Long postId = commentService.getParentPostId(parentCommentId);
//        Post post = postRepo.findById(postId)
//                .orElseThrow(() -> new RuntimeException("Post not found for comment id: " + parentCommentId));
//
//        permissionService.canCreateComment(userId, post.getBoardId());
//        commentService.replyToComment(userId, parentCommentId, content);
//    }
//
//    public void editComment(Long userId, Long commentId, String newContent) {
//        Comment comment = commentRepo.findById(commentId)
//                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));
//
//        if (comment.isDeleted()) {
//            throw new RuntimeException("Cannot edit a deleted comment");
//        }
//
//        if (!comment.getAuthorId().equals(userId)) {
//            throw new RuntimeException("You can only edit your own comments");
//        }
//
//        Post post = postRepo.findById(comment.getPostId())
//                .orElseThrow(() -> new RuntimeException("Post not found for comment id: " + commentId));
//
//        roleRepo.findByUserIdAndBoardId(userId, post.getBoardId())
//                .orElseThrow(() -> new RuntimeException("You are not a member of this board"));
//
//        commentService.updateCommentContent(commentId, newContent);
//    }
//
//    public void deleteComment(Long actingUserId, Long commentId) {
//        Comment comment = commentRepo.findById(commentId)
//                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));
//
//        Post post = postRepo.findById(comment.getPostId())
//                .orElseThrow(() -> new RuntimeException("Post not found for comment id: " + commentId));
//
//        if (!comment.getAuthorId().equals(actingUserId)) {
//            permissionService.canDeleteComment(actingUserId, post.getBoardId());
//        }
//
//        roleRepo.findByUserIdAndBoardId(actingUserId, post.getBoardId())
//                .orElseThrow(() -> new RuntimeException("User is not a member of this board"));
//
//        commentService.softDeleteComment(commentId);
//    }
//
//    // ─── Role / Moderation ───────────────────────────────────────────
//
//    public void promoteMember(Long actingUserId, Long targetUserId, Long boardId) {
//        permissionService.canPromoteMember(actingUserId, boardId);
//
//        BoardRole targetRole = roleRepo.findByUserIdAndBoardId(targetUserId, boardId)
//                .orElseThrow(() -> new RuntimeException("Target user not in board"));
//
//        targetRole.setRoleType(RoleType.MODERATOR);
//        roleRepo.save(targetRole);
//    }
//
//    public void demoteModerator(Long actingUserId, Long targetUserId, Long boardId) {
//        permissionService.canDemoteModerator(actingUserId, boardId);
//
//        BoardRole targetRole = roleRepo.findByUserIdAndBoardId(targetUserId, boardId)
//                .orElseThrow(() -> new RuntimeException("Target user not in board"));
//
//        if (!targetRole.getRoleType().isModerator()) {
//            throw new RuntimeException("Target user is not a moderator");
//        }
//
//        targetRole.setRoleType(RoleType.MEMBER);
//        roleRepo.save(targetRole);
//    }
//
//    public void transferOwnership(Long actingUserId, Long targetUserId, Long boardId) {
//        permissionService.canTransferOwnership(actingUserId, boardId);
//
//        BoardRole currentOwner = roleRepo.findByUserIdAndBoardId(actingUserId, boardId)
//                .orElseThrow(() -> new RuntimeException("You are not owner"));
//
//        BoardRole newOwner = roleRepo.findByUserIdAndBoardId(targetUserId, boardId)
//                .orElseThrow(() -> new RuntimeException("Target user not in board"));
//
//        currentOwner.setRoleType(RoleType.MODERATOR);
//        newOwner.setRoleType(RoleType.OWNER);
//
//        roleRepo.save(currentOwner);
//        roleRepo.save(newOwner);
//    }
//
//    public void kickMember(Long actingUserId, Long targetUserId, Long boardId) {
//        permissionService.canKickMember(actingUserId, boardId);
//
//        BoardRole targetRole = roleRepo.findByUserIdAndBoardId(targetUserId, boardId)
//                .orElseThrow(() -> new RuntimeException("Target user not in board"));
//
//        if (targetRole.getRoleType().isOwner()) {
//            throw new RuntimeException("Cannot kick the owner");
//        }
//
//        roleRepo.deleteByUserIdAndBoardId(targetUserId, boardId);
//    }
}