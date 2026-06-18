package com.mo.mediaodyssey.community.favorite;

import com.mo.mediaodyssey.recommendation.UserInteraction;
import com.mo.mediaodyssey.recommendation.UserInteractionRepository;
import com.mo.mediaodyssey.shared.services.CurrentAccountService;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.mo.mediaodyssey.shared.model.User;

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
 * - Fast-Rising section built from likes in the last 7 days
 * - category filter
 * - view / like interaction recording
 *
 * Deferred to later iteration:
 * - user-submitted star rating interaction
 */
@Controller
@RequestMapping("/community")
public class CommunityController {

    private final MediaRankingService mediaRankingService;
    private final UserInteractionRepository userInteractionRepository;
    private final CurrentAccountService currentAccountService;

    public CommunityController(MediaRankingService mediaRankingService,
            UserInteractionRepository userInteractionRepository, CurrentAccountService currentAccountService) {
        this.mediaRankingService = mediaRankingService;
        this.userInteractionRepository = userInteractionRepository;
        this.currentAccountService = currentAccountService;
    }

    /**
     * Resolves the logged-in user's database ID from Spring Security.
     */
    private Long getUserId(Authentication auth) {
        if (auth == null) {
            return null;
        }

        try {
            User account = currentAccountService.getCurrentAccount(auth);
            return account != null ? account.getId() : null;
        } catch (AuthenticationCredentialsNotFoundException ex) {
            return null;
        }
    }

    /**
     * Persists LIKE or VIEW interactions into UserInteraction table.
     */
    private void saveInteraction(Long userId, String mediaApiId,
            String normalizedMediaType, String interactionType) {
        if (userId == null)
            return;

        UserInteraction interaction = new UserInteraction();
        interaction.setUserId(userId);
        interaction.setMediaApiId(mediaApiId);
        interaction.setMediaType(normalizedMediaType);
        interaction.setInteractionType(interactionType);
        interaction.setTimestamp(LocalDateTime.now());
        interaction.setGenres(List.of());
        userInteractionRepository.save(interaction);
    }

    /**
     * Loads the Community Favourites page.
     * category param is kept for URL compatibility but is now handled client-side.
     *
     * Iteration 3: Fast-Rising section is populated via
     * getFastRising5PerCategory().
     */
    @GetMapping
    public String communityPage(Model model,
            @RequestParam(required = false) String category) {
        return "boardsLayout/features/trending";
    }

    /**
     * JSON API for the Community Favourites page.
     *
     * The response includes two ranking maps:
     * - top10: the overall Top 10 and the Top 10 for each category.
     * - trending: the Fast-Rising lists for each category plus the derived ALL
     * bucket.
     *
     * The ALL bucket in trending is not queried separately. It is built by the
     * service from the category-specific Fast-Rising results.
     *
     * Response shape:
     * {
     * "top10": {
     * "ALL": [ ...top 10 overall sorted by popularity... ], // 10 items
     * "MOVIE": [ ...10 items per category... ],
     * "GAME": [ ...10 items per category... ],
     * "SONG": [ ...10 items per category... ]
     * },
     * "trending": {
     * "ALL": [ ...top 5 overall sorted by trending score... ], // 5 items
     * "MOVIE": [ ...5 items per category... ],
     * "GAME": [ ...5 items per category... ],
     * "SONG": [ ...5 items per category... ]
     * },
     * "userLikedMedia": [...],
     * "userViewedMedia": [...]
     * }
     */
    @GetMapping("/data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> communityData(Authentication auth) {
        try {
            java.util.Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("top10", mediaRankingService.getTop10PerCategory());
            response.put("trending", mediaRankingService.getFastRising5PerCategory());

            // Include current user's interactions
            Long userId = getUserId(auth);
            List<String> userLikedMedia = new java.util.ArrayList<>();
            List<String> userViewedMedia = new java.util.ArrayList<>();

            if (userId != null) {
                List<UserInteraction> userInteractions = userInteractionRepository.findByUserIdWithGenres(userId);
                for (UserInteraction interaction : userInteractions) {
                    String key = interaction.getMediaType() + "|" + interaction.getMediaApiId();
                    if ("LIKE".equals(interaction.getInteractionType())) {
                        userLikedMedia.add(key);
                    } else if ("VIEW".equals(interaction.getInteractionType())) {
                        userViewedMedia.add(key);
                    }
                }
            }

            response.put("userLikedMedia", userLikedMedia);
            response.put("userViewedMedia", userViewedMedia);
            return ResponseEntity.ok(response);
        } finally {
            // Clear request-scoped cache after building response
            mediaRankingService.clearRequestCache();
        }
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
