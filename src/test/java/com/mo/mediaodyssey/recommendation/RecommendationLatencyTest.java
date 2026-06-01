package com.mo.mediaodyssey.recommendation;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RecommendationLatencyTest {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private UserInteractionRepository userInteractionRepository;

    @Test
    void timedGameRecommendations_withLiveApis() {
        Long userId = 987654321L;

        UserInteraction view = new UserInteraction();
        view.setUserId(userId);
        view.setMediaApiId("seed-game-1");
        view.setInteractionType("VIEW");
        view.setMediaType("GAME");
        view.setTimestamp(LocalDateTime.now());
        view.setGenres(List.of("Action"));
        userInteractionRepository.save(view);

        UserInteraction like = new UserInteraction();
        like.setUserId(userId);
        like.setMediaApiId("seed-game-2");
        like.setInteractionType("LIKE");
        like.setMediaType("GAME");
        like.setTimestamp(LocalDateTime.now());
        like.setGenres(List.of("Action"));
        userInteractionRepository.save(like);

        long start = System.nanoTime();
        List<RecommendationResponse> recommendations = recommendationService.getRecommendations(userId, "GAME");
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        System.out.println("RECOMMENDATION_LATENCY_MS=" + elapsedMs + " RESULTS=" + recommendations.size());
        assertFalse(recommendations.isEmpty());
    }
}
