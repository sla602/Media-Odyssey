package com.mo.mediaodyssey.layout.services;

import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.layout.models.Boards;
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

    // -----------------helper-------------------
    /**
     * Returns true if the user is BANNED from this board.
     * Used to block navigation into posts/comments the user can't access.
     */
    public boolean isUserBanned(Long userId, Long boardId) {
        return roleRepo.findByUserIdAndBoardId(userId, boardId)
                .map(role -> role.getRoleType().isBanned())
                .orElse(false);
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
        List<BoardRole> roles = roleRepo.findActiveByUserId(userId);
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
            if (role.isLeft()) {
                existing.get().setRoleType(RoleType.MEMBER);
                roleRepo.save(existing.get());
                return;
            }
            if (role.isOwner()) throw new IllegalStateException("You are already the owner.");
            if (role.isModerator()) throw new IllegalStateException("You are already a moderator.");
            if (role.isBanned()) throw new IllegalStateException("You are banned from this board.");
            throw new IllegalStateException("You are already a member.");
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

        System.out.println("LEAVING: userId=" + userId + " boardId=" + boardId + " oldRole=" + role.getRoleType());
        role.setRoleType(RoleType.LEFT);
        roleRepo.save(role);
        System.out.println("LEFT: new role=" + role.getRoleType());
    }

    public boolean isUserInBoard(Long userId, Long boardId) {
        return roleRepo.existsByUserIdAndBoardId(userId, boardId);
    }

    public Long getMemberCount(Long boardId) {
        return roleRepo.countByBoardId(boardId);
    }


}