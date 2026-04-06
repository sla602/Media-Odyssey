package com.mo.mediaodyssey.socialFeature.services;

import com.mo.mediaodyssey.layout.models.Boards;
import com.mo.mediaodyssey.layout.models.Profile;
import com.mo.mediaodyssey.layout.repositories.BoardsRepository;
import com.mo.mediaodyssey.socialFeature.repositories.ProfileRepository;
import com.mo.mediaodyssey.layout.services.BoardsService;
import com.mo.mediaodyssey.socialFeature.models.Comment;
import com.mo.mediaodyssey.socialFeature.models.Post;
import com.mo.mediaodyssey.socialFeature.repositories.CommentRepository;
import com.mo.mediaodyssey.socialFeature.repositories.PostRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for profile-related read operations that don't belong on the
 * controller — profile fetch/create, and building the recent-activity
 * feed (posts + comments) shown on userProfile.html.
 */
@Service
public class ProfileService {

    private static final int RECENT_ACTIVITY_LIMIT = 5;
    private static final int PREVIEW_MAX_LENGTH = 140;

    private final ProfileRepository profileRepo;
    private final PostRepository postRepo;
    private final CommentRepository commentRepo;
    private final BoardsRepository boardsRepo;
    private final BoardsService boardsService;


    public ProfileService(ProfileRepository profileRepo,
                          PostRepository postRepo,
                          CommentRepository commentRepo,
                          BoardsRepository boardsRepo, BoardsService boardsService) {
        this.profileRepo = profileRepo;
        this.postRepo = postRepo;
        this.commentRepo = commentRepo;
        this.boardsRepo = boardsRepo;
        this.boardsService = boardsService;
    }

    // ─── Profile fetch/create ────────────────────────────────────────

    /**
     * Return the user's profile, creating an empty one on first access.
     */
    public Profile getOrCreateProfile(Long userId) {
        return profileRepo.findByUserId(userId)
                .orElseGet(() -> profileRepo.save(new Profile(userId)));
    }

    /**
     * Returns true if the user has a non-blank username set on their profile.
     * Used as a gate for social actions (join board, friend requests, etc.).
     */
    public boolean hasUsername(Long userId) {
        return profileRepo.findByUserId(userId)
                .map(p -> p.getUsername() != null && !p.getUsername().isBlank())
                .orElse(false);
    }


    /**
     * Update and persist the editable profile fields for the given user.
     *
     * Rule: once a user has set a username, they cannot clear it back to
     *  null/blank. The description and pronouns stay freely editable.
     */
    public Profile updateProfile(Long userId, String username, String description, String pronouns) {
        Profile profile = getOrCreateProfile(userId);

        String currentUsername = profile.getUsername();
        boolean hasExistingUsername = currentUsername != null && !currentUsername.isBlank();
        boolean incomingIsBlank = username == null || username.isBlank();

        if (hasExistingUsername && incomingIsBlank) {
            throw new IllegalStateException("Username cannot be removed once set.");
        }

        // Only update username if a value was actually provided.
        if (!incomingIsBlank) {
            profile.setUsername(username.trim());
        }
        profile.setDescription(description);
        profile.setPronouns(pronouns);
        return profileRepo.save(profile);
    }

    // ─── Recent activity feed ────────────────────────────────────────

    /**
     * Build the recent-activity list for a user: their most recent posts and
     * comments (non-deleted), newest first, capped at RECENT_ACTIVITY_LIMIT.
     */
    public List<Map<String, Object>> buildRecentActivity(Long userId, Long viewerUserId) {
        List<Post> posts = postRepo.findByAuthorId(userId);
        List<Comment> comments = commentRepo.findRecentByAuthorId(userId);

        // Collect board IDs for all items so we can look up names in one pass.
        Set<Long> boardIds = new HashSet<>();
        for (Post p : posts) {
            if (!p.isDeleted()) boardIds.add(p.getBoardId());
        }

        // Comments don't carry boardId directly — look it up via their post.
        // Batch-load the posts referenced by these comments.
        Set<Long> commentPostIds = comments.stream()
                .map(Comment::getPostId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Post> postsByIdForComments = new HashMap<>();
        if (!commentPostIds.isEmpty()) {
            postRepo.findAllById(commentPostIds)
                    .forEach(p -> postsByIdForComments.put(p.getId(), p));
        }
        for (Post p : postsByIdForComments.values()) {
            boardIds.add(p.getBoardId());

        }

        Map<Long, String> boardNames = loadBoardNames(boardIds);

        List<Map<String, Object>> items = new ArrayList<>();

        // Add posts
        for (Post p : posts) {
            if (p.isDeleted()) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("type", "POST");
            item.put("boardId", p.getBoardId());
            item.put("boardName", boardNames.getOrDefault(p.getBoardId(), "Deleted board"));
            item.put("title", p.getTitle());
            item.put("contentPreview", preview(p.getContent(), PREVIEW_MAX_LENGTH));
            item.put("postId", p.getId());
            item.put("createdAt", p.getCreatedAt());
            item.put("banned", boardsService.isUserBanned(viewerUserId, p.getBoardId()));
            items.add(item);
        }

        // Add comments
        for (Comment c : comments) {
            Post parent = postsByIdForComments.get(c.getPostId());
            if (parent == null) continue; // parent post deleted — skip
            Map<String, Object> item = new HashMap<>();
            item.put("type", "COMMENT");
            item.put("boardId", parent.getBoardId());
            item.put("boardName", boardNames.getOrDefault(parent.getBoardId(), "Deleted board"));
            item.put("title", parent.getTitle()); // show "on <title>"
            item.put("contentPreview", preview(c.getContent(), PREVIEW_MAX_LENGTH));
            item.put("postId", c.getPostId());
            item.put("createdAt", c.getCreatedAt());
            item.put("banned", boardsService.isUserBanned(viewerUserId, parent.getBoardId()));
            items.add(item);
        }

        // Sort newest-first across both types, then cap.
        items.sort((a, b) -> {
            Object da = a.get("createdAt");
            Object db = b.get("createdAt");
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            @SuppressWarnings("unchecked")
            Comparable<Object> ca = (Comparable<Object>) da;
            return ca.compareTo(db) * -1;
        });

        if (items.size() > RECENT_ACTIVITY_LIMIT) {
            return new ArrayList<>(items.subList(0, RECENT_ACTIVITY_LIMIT));
        }
        return items;
    }

    // ─── Internal helpers ────────────────────────────────────────────

    /**
     * Batch-load board names for the given IDs, so we don't hit the DB
     * once per activity item. Falls back to "Deleted board" if missing.
     */
    private Map<Long, String> loadBoardNames(Collection<Long> boardIds) {
        Map<Long, String> names = new HashMap<>();
        if (boardIds == null || boardIds.isEmpty()) return names;

        Set<Long> distinct = new HashSet<>();
        for (Long id : boardIds) {
            if (id != null) distinct.add(id);
        }
        for (Long id : distinct) {
            Optional<Boards> b = boardsRepo.findById(id);
            names.put(id, b.map(Boards::getBoard_name).orElse("Deleted board"));
        }
        return names;
    }

    /**
     * Truncate a string to maxLen characters if needed.
     */
    private String preview(String text, int maxLen) {
        if (text == null) return "";
        String clean = text.trim();
        if (clean.length() <= maxLen) return clean;
        return clean.substring(0, maxLen).trim() + "…";
    }
}
