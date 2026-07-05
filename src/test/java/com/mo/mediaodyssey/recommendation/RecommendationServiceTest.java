package com.mo.mediaodyssey.recommendation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
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

    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(
                userInteractionRepository,
                bannedMediaRepository,
                restTemplate,
                directExecutor());
    }

    @Test
    void getRecommendations_movie_fetchesFreshResultsOnEachRefresh() throws Exception {
        UserInteraction favoriteInteraction = interaction(42L, "seed-movie", "MOVIE", "LIKE", "Action");

        when(userInteractionRepository.findByUserIdWithGenres(42L)).thenReturn(List.of(favoriteInteraction));
        when(userInteractionRepository.findLikedMediaApiIdsByUserId(42L)).thenReturn(List.of());
        when(bannedMediaRepository.findAll()).thenReturn(List.of());

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

    @Test
    void getRecommendations_game_fetchesFavoriteAndAlternateGenresSeparately() {
        UserInteraction favoriteInteraction = interaction(42L, "seed-game", "GAME", "LIKE", "Action");

        when(userInteractionRepository.findByUserIdWithGenres(42L)).thenReturn(List.of(favoriteInteraction));
        when(userInteractionRepository.findLikedMediaApiIdsByUserId(42L)).thenReturn(List.of());
        when(bannedMediaRepository.findAll()).thenReturn(List.of());

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenAnswer(invocation -> {
            String url = invocation.getArgument(0, String.class);
            if (url.contains("api.rawg.io") && url.contains("genres=action")) {
                return """
                        {"results":[{"id":101,"name":"Favorite genre game","background_image":"https://images.example/fav.jpg","metacritic":91}]}
                        """;
            }
            if (url.contains("api.rawg.io") && url.contains("page_size=10")) {
                return """
                        {"results":[{"id":201,"name":"Alternate genre game","background_image":"https://images.example/other.jpg","metacritic":84}]}
                        """;
            }
            throw new AssertionError("Unexpected URL: " + url);
        });

        List<RecommendationResponse> recommendations = recommendationService.getRecommendations(42L, "GAME");

        assertThat(recommendations).hasSize(2);
        assertThat(recommendations.get(0).getTitle()).isEqualTo("Favorite genre game");
        assertThat(recommendations.get(0).getGenre()).isEqualTo("Action");
        assertThat(recommendations.get(1).getTitle()).isEqualTo("Alternate genre game");
        assertThat(recommendations.get(1).getGenre()).isNotEqualToIgnoringCase("Action");
        verify(restTemplate, atLeastOnce()).getForObject(anyString(), eq(String.class));
    }

    @Test
    void getRecommendations_game_retriesPageOneAfterInvalidRawgPageAndLogsError() throws Exception {
        UserInteraction favoriteInteraction = interaction(42L, "seed-game", "GAME", "LIKE", "Action");
        setRecommendationRandomSeed(2L);

        when(userInteractionRepository.findByUserIdWithGenres(42L)).thenReturn(List.of(favoriteInteraction));
        when(userInteractionRepository.findLikedMediaApiIdsByUserId(42L)).thenReturn(List.of());
        when(bannedMediaRepository.findAll()).thenReturn(List.of());

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenAnswer(invocation -> {
            String url = invocation.getArgument(0, String.class);
            if (url.contains("api.rawg.io") && !url.contains("page=1")) {
                throw new RuntimeException("404 Not Found: {\"detail\":\"Invalid page.\"}");
            }
            if (url.contains("api.rawg.io") && url.contains("genres=action")) {
                return """
                        {"results":[{"id":101,"name":"Favorite genre game","background_image":"https://images.example/fav.jpg","metacritic":91}]}
                        """;
            }
            if (url.contains("api.rawg.io")) {
                return """
                        {"results":[{"id":201,"name":"Alternate genre game","background_image":"https://images.example/other.jpg","metacritic":84}]}
                        """;
            }
            throw new AssertionError("Unexpected URL: " + url);
        });

        PrintStream originalErr = System.err;
        ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
        System.setErr(new PrintStream(capturedErr, true, StandardCharsets.UTF_8));
        List<RecommendationResponse> recommendations;
        try {
            recommendations = recommendationService.getRecommendations(42L, "GAME");
        } finally {
            System.setErr(originalErr);
        }

        assertThat(recommendations).hasSize(2);
        assertThat(recommendations)
                .extracting(RecommendationResponse::getTitle)
                .containsExactly("Favorite genre game", "Alternate genre game");
        assertThat(capturedErr.toString(StandardCharsets.UTF_8)).contains("RAWG API error");
    }

    private UserInteraction interaction(Long userId, String mediaApiId, String mediaType,
            String interactionType, String genre) {
        UserInteraction interaction = new UserInteraction();
        interaction.setUserId(userId);
        interaction.setMediaApiId(mediaApiId);
        interaction.setMediaType(mediaType);
        interaction.setInteractionType(interactionType);
        interaction.setTimestamp(LocalDateTime.now());
        interaction.setGenres(List.of(genre));
        return interaction;
    }

    private void setRecommendationRandomSeed(long seed) throws Exception {
        Field randomField = RecommendationService.class.getDeclaredField("random");
        randomField.setAccessible(true);
        ((java.util.Random) randomField.get(recommendationService)).setSeed(seed);
    }

    private ExecutorService directExecutor() {
        return new AbstractExecutorService() {
            @Override
            public void shutdown() {
            }

            @Override
            public List<Runnable> shutdownNow() {
                return List.of();
            }

            @Override
            public boolean isShutdown() {
                return false;
            }

            @Override
            public boolean isTerminated() {
                return false;
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) {
                return true;
            }

            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
    }
}
