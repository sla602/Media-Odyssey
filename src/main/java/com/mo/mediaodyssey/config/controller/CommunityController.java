package com.mo.mediaodyssey.config.controller;

import com.mo.mediaodyssey.entity.Media;
import com.mo.mediaodyssey.repository.MediaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/community")
public class CommunityController {

    private final MediaRepository mediaRepository;

    public CommunityController(MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    // ── 1. Community Favourite Main Page ──────────────────────────────────────

    /**
     * Loads the community-favourite page.
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
            // Show all media sorted by likes — used for "Most Liked" view
            mediaList = mediaRepository.findAllByOrderByLikesDesc();
        } else if ("views".equals(sort)) {
            // Show all media sorted by views — used for "Most Viewed" view
            mediaList = mediaRepository.findAllByOrderByViewsDesc();
        } else if (category != null && !category.isBlank()) {
            // Top 10 by Point System filtered by category tab
            mediaList = mediaRepository.findTop10ByScoreAndCategory(category);
        } else {
            // Default: Top 10 by Point System across all categories
            mediaList = mediaRepository.findTop10ByScore();
        }

        // Fast-Rising Top 5 for the trending section
        List<Media> trending = mediaRepository.findTop5Trending();

        model.addAttribute("mediaList",  mediaList);
        model.addAttribute("trending",   trending);
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentCat", category);

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

    // ── 4. Like Media (AJAX — Optimistic UI) ──────────────────────────────────

    /**
     * Increments the like count by 1.
     * Returns updated likes and totalScore as JSON so the frontend
     * can confirm the optimistic update without a page reload.
     */
    @PostMapping("/like/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> likeMedia(@PathVariable Long id) {
        return mediaRepository.findById(id)
                .map(m -> {
                    m.setLikes(m.getLikes() + 1);
                    mediaRepository.save(m);
                    return ResponseEntity.ok(Map.<String, Object>of(
                            "likes",      m.getLikes(),
                            "totalScore", m.getTotalScore()
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ── 5. Increment View Count (AJAX) ────────────────────────────────────────

    /**
     * Increments the view count when a media item is clicked or visited.
     * Returns updated views and totalScore.
     */
    @PostMapping("/view/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> incrementView(@PathVariable Long id) {
        return mediaRepository.findById(id)
                .map(m -> {
                    m.setViews(m.getViews() + 1);
                    mediaRepository.save(m);
                    return ResponseEntity.ok(Map.<String, Object>of(
                            "views",      m.getViews(),
                            "totalScore", m.getTotalScore()
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}