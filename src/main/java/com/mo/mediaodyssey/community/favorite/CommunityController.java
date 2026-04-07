package com.mo.mediaodyssey.community.favorite;

import com.mo.mediaodyssey.auth.repository.UserRepository;
import com.mo.mediaodyssey.recommendation.UserInteraction;
import com.mo.mediaodyssey.recommendation.UserInteractionRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controller for the Community Favourites page (/community).
 *
 * Current iteration scope:
 * - Top 10 ranking page
 * - category filter
 * - view / like interaction recording
 *
 * Deferred to later iteration:
 * - Fast-Rising section
 * - user-submitted star rating interaction
 */
@Controller
@RequestMapping("/community")
public class CommunityController {

    private final MediaRankingService mediaRankingService;
    private final UserInteractionRepository userInteractionRepository;
    private final UserRepository userRepository;

    public CommunityController(MediaRankingService mediaRankingService,
            UserInteractionRepository userInteractionRepository,
            UserRepository userRepository) {
        this.mediaRankingService = mediaRankingService;
        this.userInteractionRepository = userInteractionRepository;
        this.userRepository = userRepository;
    }

    /**
     * Resolves the logged-in user's database ID from Spring Security.
     */
    private Long getUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated())
            return null;
        return userRepository.findByEmail(auth.getName())
                .map(u -> u.getId())
                .orElse(null);
    }

    /**
     * Persists LIKE or VIEW interactions into UserInteraction table.
     */
    private void saveInteraction(Long userId, String mediaApiId,
            String mediaType, String interactionType) {
        if (userId == null)
            return;

        UserInteraction interaction = new UserInteraction();
        interaction.setUserId(userId);
        interaction.setMediaApiId(mediaApiId);
        interaction.setMediaType(mediaRankingService.normalizeMediaType(mediaType));
        interaction.setInteractionType(interactionType);
        interaction.setTimestamp(LocalDateTime.now());
        interaction.setGenres(List.of());
        userInteractionRepository.save(interaction);
    }

    /**
     * Loads the Community Favourites page.
     * category param is kept for URL compatibility but is now handled client-side.
     *
     * Iteration 3: Fast-Rising section now populated via getFastRising5().
     */
    @GetMapping
    public String communityPage(Model model,
            @RequestParam(required = false) String category) {
        return "boardsLayout/features/trending";
    }

    /**
     * JSON API: returns ALL Top 10 + Fast-Rising data in one shot so the
     * front-end can filter client-side (same pattern as likedMedia.js).
     *
     * Response shape:
     * {
     *   "top10":    [ RankedMediaResponse, ... ],   // all categories, sorted by score
     *   "trending": [ RankedMediaResponse, ... ]    // fast-rising top 5 (past 7 days)
     * }
     */
    @GetMapping("/data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> communityData() {
        List<RankedMediaResponse> top10    = mediaRankingService.getTop10();
        List<RankedMediaResponse> trending = mediaRankingService.getFastRising5();
        return ResponseEntity.ok(Map.of(
                "top10",    top10,
                "trending", trending));
    }

    /**
     * Records a LIKE interaction for the specified media item.
     * Session-based duplicate prevention.
     *
     * Note:
     * The current UI does not show a like button, but this endpoint is kept
     * because the ranking system still uses LIKE as part of the internal score.
     */
    @PostMapping("/like")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> likeMedia(@RequestParam String mediaApiId,
            @RequestParam String mediaType,
            HttpSession session,
            Authentication auth) {
        String normalizedMediaType = mediaRankingService.normalizeMediaType(mediaType);
        String sessionKey = normalizedMediaType + ":" + mediaApiId;

        @SuppressWarnings("unchecked")
        Set<String> likedIdsRaw = (Set<String>) session.getAttribute("likedIds");
        if (likedIdsRaw == null) {
            likedIdsRaw = new HashSet<>();
            session.setAttribute("likedIds", likedIdsRaw);
        }
        final Set<String> likedIds = likedIdsRaw;

        if (likedIds.contains(sessionKey)) {
            return ResponseEntity.ok(Map.of(
                    "liked", false,
                    "mediaApiId", mediaApiId));
        }

        saveInteraction(getUserId(auth), mediaApiId, normalizedMediaType, "LIKE");

        likedIds.add(sessionKey);
        session.setAttribute("likedIds", likedIds);

        return ResponseEntity.ok(Map.of(
                "liked", true,
                "mediaApiId", mediaApiId));
    }

    /**
     * Records a VIEW interaction for the specified media item.
     * Called automatically by JavaScript when the page loads.
     */
    @PostMapping("/view")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> incrementView(@RequestParam String mediaApiId,
            @RequestParam String mediaType,
            HttpSession session,
            Authentication auth) {
        String normalizedMediaType = mediaRankingService.normalizeMediaType(mediaType);
        String sessionKey = normalizedMediaType + ":" + mediaApiId;

        @SuppressWarnings("unchecked")
        Set<String> viewedIdsRaw = (Set<String>) session.getAttribute("viewedIds");
        if (viewedIdsRaw == null) {
            viewedIdsRaw = new HashSet<>();
            session.setAttribute("viewedIds", viewedIdsRaw);
        }
        final Set<String> viewedIds = viewedIdsRaw;

        if (viewedIds.contains(sessionKey)) {
            return ResponseEntity.ok(Map.of(
                    "viewed", false,
                    "mediaApiId", mediaApiId));
        }

        saveInteraction(getUserId(auth), mediaApiId, normalizedMediaType, "VIEW");

        viewedIds.add(sessionKey);
        session.setAttribute("viewedIds", viewedIds);

        return ResponseEntity.ok(Map.of(
                "viewed", true,
                "mediaApiId", mediaApiId));
    }
}