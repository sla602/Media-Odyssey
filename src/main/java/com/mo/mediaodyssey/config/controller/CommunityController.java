package com.mo.mediaodyssey.config.controller;

import com.mo.mediaodyssey.entity.Media;
import com.mo.mediaodyssey.repository.MediaRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/community")
public class CommunityController {

    private final MediaRepository mediaRepository;

    @Autowired
    public CommunityController(MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    // ── 1. Community Favourite Main Page ──────────────────────────────────────

    /**
     * Loads the community-favourite page.
     * View count is incremented automatically on every page load.
     * sort param options:
     *   - null (default) → Top 10 by Point System (views*1 + likes*5)
     *   - "likes"        → all media sorted by likes desc
     *   - "views"        → all media sorted by views desc
     * category param filters the Top 10 by Game / Movie / Song tab.
     */
    @GetMapping
    public String communityPage(Model model,
                                @RequestParam(required = false) String sort,
                                @RequestParam(required = false) String category) {

        List<Media> mediaList;

        if ("likes".equals(sort)) {
            mediaList = mediaRepository.findAllByOrderByLikesDesc();
        } else if ("views".equals(sort)) {
            mediaList = mediaRepository.findAllByOrderByViewsDesc();
        } else if (category != null && !category.isBlank()) {
            mediaList = mediaRepository.findTop10ByScoreAndCategory(category);
        } else {
            mediaList = mediaRepository.findTop10ByScore();
        }

        // Fast-Rising Top 5 for the trending section
        List<Media> trending = mediaRepository.findTop5Trending();

        model.addAttribute("mediaList",   mediaList);
        model.addAttribute("trending",    trending);
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentCat",  category);

        return "community";
    }

    // ── 2. Add Media Form ─────────────────────────────────────────────────────

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("media", new Media());
        return "add-media";
    }

    // ── 3. Save New Media ─────────────────────────────────────────────────────

    @PostMapping("/add")
    public String saveMedia(@ModelAttribute Media media) {
        mediaRepository.save(media);
        return "redirect:/community";
    }

    // ── 4. Like Media (AJAX — session-based duplicate prevention) ─────────────

    /**
     * Increments the like count by 1, but only once per session per media item.
     * Uses HttpSession to store a set of already-liked media IDs ("likedIds").
     * This prevents the same browser session from liking the same item twice.
     * Returns:
     *   - liked: true if the like was accepted, false if already liked
     *   - likes: updated like count
     *   - totalScore: recalculated Point System score
     */
    @PostMapping("/like/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> likeMedia(@PathVariable Long id,
                                                          HttpSession session) {
        // Retrieve or create the set of liked media IDs for this session
        @SuppressWarnings("unchecked")
        Set<Long> likedIdsRaw = (Set<Long>) session.getAttribute("likedIds");
        if (likedIdsRaw == null) {
            likedIdsRaw = new HashSet<>();
            session.setAttribute("likedIds", likedIdsRaw);
        }
        // Assign to a final variable so it can be used inside the lambda below
        final Set<Long> likedIds = likedIdsRaw;

        // Reject if this session already liked this item
        if (likedIds.contains(id)) {
            return ResponseEntity.ok(Map.<String, Object>of(
                    "liked",      false,
                    "likes",      mediaRepository.findById(id).map(Media::getLikes).orElse(0),
                    "totalScore", mediaRepository.findById(id).map(Media::getTotalScore).orElse(0)
            ));
        }

        return mediaRepository.findById(id)
                .map(m -> {
                    m.setLikes(m.getLikes() + 1);
                    mediaRepository.save(m);

                    // Record this like in the session so it can't be repeated
                    likedIds.add(id);
                    session.setAttribute("likedIds", likedIds);

                    return ResponseEntity.ok(Map.<String, Object>of(
                            "liked",      true,
                            "likes",      m.getLikes(),
                            "totalScore", m.getTotalScore()
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ── 5. Increment View Count (auto on page load) ───────────────────────────

    /**
     * Increments the view count for a specific media item.
     * Called automatically by JavaScript when the community page loads,
     * once per media item per session to avoid inflating counts on refresh.
     * Returns updated views and totalScore.
     */
    @PostMapping("/view/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> incrementView(@PathVariable Long id,
                                                              HttpSession session) {
        // Retrieve or create the set of already-viewed media IDs for this session
        @SuppressWarnings("unchecked")
        Set<Long> viewedIdsRaw = (Set<Long>) session.getAttribute("viewedIds");
        if (viewedIdsRaw == null) {
            viewedIdsRaw = new HashSet<>();
            session.setAttribute("viewedIds", viewedIdsRaw);
        }
        // Assign to a final variable so it can be used inside the lambda below
        final Set<Long> viewedIds = viewedIdsRaw;

        // Skip if already counted for this session
        if (viewedIds.contains(id)) {
            return mediaRepository.findById(id)
                    .map(m -> ResponseEntity.ok(Map.<String, Object>of(
                            "views",      m.getViews(),
                            "totalScore", m.getTotalScore()
                    )))
                    .orElse(ResponseEntity.notFound().build());
        }

        return mediaRepository.findById(id)
                .map(m -> {
                    m.setViews(m.getViews() + 1);
                    mediaRepository.save(m);

                    // Record this view in the session
                    viewedIds.add(id);
                    session.setAttribute("viewedIds", viewedIds);

                    return ResponseEntity.ok(Map.<String, Object>of(
                            "views",      m.getViews(),
                            "totalScore", m.getTotalScore()
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}