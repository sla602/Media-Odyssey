package com.mo.mediaodyssey.recommendation;

import com.mo.mediaodyssey.shared.services.CurrentAccountService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final CurrentAccountService currentAccountService;

    public RecommendationController(RecommendationService recommendationService,
            CurrentAccountService currentAccountService) {
        this.recommendationService = recommendationService;
        this.currentAccountService = currentAccountService;
    }

    // gets the logged-in user's ID
    private Long getUserIdFromSession(Authentication auth) {
        return currentAccountService.getCurrentAccount(auth).getId();

    }

    // POST /api/recommendations/interactions
    // body: { "mediaApiId": "...", "interactionType": "VIEW", "mediaType": "MOVIE",
    // "genres": ["Action"] }
    @PostMapping("/interactions")
    public ResponseEntity<Void> recordInteraction(@RequestBody InteractionRequest request,
            Authentication auth) {
        Long userId = getUserIdFromSession(auth);
        recommendationService.recordInteraction(userId, request);
        return ResponseEntity.ok().build();
    }

    // DELETE /api/recommendations/interactions/like?mediaApiId=123
    @DeleteMapping("/interactions/like")
    public ResponseEntity<Void> unlikeMedia(@RequestParam String mediaApiId,
            Authentication auth) {
        Long userId = getUserIdFromSession(auth);
        recommendationService.unlikeMedia(userId, mediaApiId);
        return ResponseEntity.ok().build();
    }

    // GET /api/recommendations?mediaType=MOVIE
    @GetMapping
    public ResponseEntity<List<RecommendationResponse>> getRecommendations(@RequestParam String mediaType,
            Authentication auth) {
        Long userId = getUserIdFromSession(auth);
        List<RecommendationResponse> recommendations = recommendationService.getRecommendations(userId, mediaType);
        return ResponseEntity.ok(recommendations);
    }

    // GET /api/recommendations/liked
    // Returns all media the logged-in user has liked, enriched with title/image
    // from external APIs
    @GetMapping("/liked")
    public ResponseEntity<List<RecommendationResponse>> getLikedMedia(Authentication auth) {
        Long userId = getUserIdFromSession(auth);
        List<RecommendationResponse> liked = recommendationService.getLikedMedia(userId);
        return ResponseEntity.ok(liked);
    }

    // POST /api/recommendations/admin/ban?mediaApiId=123&mediaType=MOVIE
    @PostMapping("/admin/ban")
    public ResponseEntity<Void> banMedia(@RequestParam String mediaApiId,
            @RequestParam String mediaType) {
        recommendationService.banMedia(mediaApiId, mediaType);
        return ResponseEntity.ok().build();
    }

    // DELETE /api/recommendations/admin/ban?mediaApiId=123
    @DeleteMapping("/admin/ban")
    public ResponseEntity<Void> unbanMedia(@RequestParam String mediaApiId) {
        recommendationService.unbanMedia(mediaApiId);
        return ResponseEntity.ok().build();
    }
}