package com.mo.mediaodyssey.recommendation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private UserInteractionRepository userInteractionRepository;

    @Mock
    private BannedMediaRepository bannedMediaRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ExecutorService recommendationExecutor;

    @InjectMocks
    private RecommendationService recommendationService;

    @Test
    void getRecommendations_movie_fetchesFreshResultsOnEachRefresh() throws Exception {
        UserInteraction favoriteInteraction = new UserInteraction();
        favoriteInteraction.setUserId(42L);
        favoriteInteraction.setMediaApiId("seed-movie");
        favoriteInteraction.setMediaType("MOVIE");
        favoriteInteraction.setInteractionType("LIKE");
        favoriteInteraction.setTimestamp(LocalDateTime.now());
        favoriteInteraction.setGenres(List.of("Action"));

        when(userInteractionRepository.findByUserIdWithGenres(42L)).thenReturn(List.of(favoriteInteraction));
        when(userInteractionRepository.findLikedMediaApiIdsByUserId(42L)).thenReturn(List.of());
        when(bannedMediaRepository.findAll()).thenReturn(List.of());
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0, Runnable.class);
            task.run();
            return null;
        }).when(recommendationExecutor).execute(any());

        AtomicInteger callNumber = new AtomicInteger();

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenAnswer(invocation -> {
            String url = invocation.getArgument(0, String.class);
            if (url.contains("api.themoviedb.org") && url.contains("with_genres=")) {
                int current = callNumber.incrementAndGet();
                if (current <= 2) {
                    return current == 1
                            ? "{\"results\":[{\"id\":101,\"title\":\"First refresh A\",\"poster_path\":\"/fav.jpg\",\"popularity\":10.0}]}"
                            : "{\"results\":[{\"id\":102,\"title\":\"First refresh B\",\"poster_path\":\"/other.jpg\",\"popularity\":9.0}]}";
                }
                return current == 3
                        ? "{\"results\":[{\"id\":201,\"title\":\"Second refresh A\",\"poster_path\":\"/fav2.jpg\",\"popularity\":8.0}]}"
                        : "{\"results\":[{\"id\":202,\"title\":\"Second refresh B\",\"poster_path\":\"/other2.jpg\",\"popularity\":7.0}]}";
            }
            throw new AssertionError("Unexpected URL: " + url);
        });

        List<RecommendationResponse> first = recommendationService.getRecommendations(42L, "MOVIE");
        List<RecommendationResponse> second = recommendationService.getRecommendations(42L, "MOVIE");

        assertThat(first).hasSize(2);
        assertThat(second).hasSize(2);
        assertThat(first.get(0).getTitle()).isEqualTo("First refresh A");
        assertThat(first.get(1).getTitle()).isEqualTo("First refresh B");
        assertThat(second.get(0).getTitle()).isEqualTo("Second refresh A");
        assertThat(second.get(1).getTitle()).isEqualTo("Second refresh B");
        assertThat(callNumber.get()).isEqualTo(4);
        verify(bannedMediaRepository, org.mockito.Mockito.times(2)).findAll();
    }
}