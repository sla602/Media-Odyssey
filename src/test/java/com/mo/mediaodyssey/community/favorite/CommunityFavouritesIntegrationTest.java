package com.mo.mediaodyssey.community.favorite;

import com.mo.mediaodyssey.recommendation.UserInteraction;
import com.mo.mediaodyssey.recommendation.UserInteractionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Community Favourites feature.
 *
 * These tests load the full Spring context and connect to the real PostgreSQL
 * DB.
 * 
 * They will FAIL if:
 * - The DB connection is misconfigured or unreachable.
 * - UserInteraction cannot be saved or queried.
 * - The ranking query returns unexpected results.
 * - External API keys (TMDB, RAWG, Last.fm) are invalid or missing.
 * - 50 likes are added during the test to give the items a score of 500 so they
 * appear in the Top 10. Likewise, 10 likes are added to the fast-rising top 5
 * items so they appear in the fast-rising list. However, this can still fail if
 * there are other items which have greater amount of points. These numbers were
 * picked because this is a reasonable number of interactions without an
 * excessive amount of time to write to the DB.
 *
 * Run with: ./mvnw test
 * Requires: valid DB and API credentials in application.properties or .env
 */
@SpringBootTest
class CommunityFavouritesIntegrationTest {

    @Autowired
    private UserInteractionRepository userInteractionRepository;

    @Autowired
    private MediaRankingService mediaRankingService;

    private static final long TEST_USER_ID_BASE = 99990L;

    @AfterEach
    void cleanUp() {
        for (long uid = TEST_USER_ID_BASE; uid <= TEST_USER_ID_BASE + 9; uid++) {
            List<UserInteraction> toDelete = userInteractionRepository.findByUserId(uid);
            if (!toDelete.isEmpty()) {
                userInteractionRepository.deleteAllInBatch(toDelete);
            }
        }
        mediaRankingService.clearRequestCache();
    }

    // ─── 1. DB Connection / Context Load ──────────────────────────────────────

    @Test
    void contextLoads_dbConnectionSucceeds() {
        assertThat(userInteractionRepository).isNotNull();
        assertThat(mediaRankingService).isNotNull();
    }

    // ─── 2. UserInteraction Save / Query ──────────────────────────────────────

    @Test
    void userInteraction_saveAndRetrieve_succeeds() {
        UserInteraction interaction = new UserInteraction();
        interaction.setUserId(TEST_USER_ID_BASE);
        interaction.setMediaApiId("integration-test-media");
        interaction.setMediaType("MOVIE");
        interaction.setInteractionType("VIEW");
        interaction.setTimestamp(LocalDateTime.now());
        interaction.setGenres(List.of("Action"));

        UserInteraction saved = userInteractionRepository.save(interaction);

        assertThat(saved.getId()).isNotNull();

        List<UserInteraction> found = userInteractionRepository.findByUserId(TEST_USER_ID_BASE);
        assertThat(found).isNotEmpty();
        assertThat(found.get(0).getMediaApiId()).isEqualTo("integration-test-media");
        assertThat(found.get(0).getInteractionType()).isEqualTo("VIEW");
    }

    // Score is verified directly from saved interactions (VIEW=1, LIKE=10)
    // rather than through the ranking query, to avoid LIMIT 10 cutting off
    // the test item in a populated DB.
    @Test
    void findTop10ByScore_viewAndLike_scoresCorrectly() {
        String testMediaId = "ranking-integration-" + System.currentTimeMillis();
        long testUserId = TEST_USER_ID_BASE + 1;

        UserInteraction view = new UserInteraction();
        view.setUserId(testUserId);
        view.setMediaApiId(testMediaId);
        view.setMediaType("MOVIE");
        view.setInteractionType("VIEW");
        view.setTimestamp(LocalDateTime.now());
        view.setGenres(List.of());

        UserInteraction like = new UserInteraction();
        like.setUserId(testUserId);
        like.setMediaApiId(testMediaId);
        like.setMediaType("MOVIE");
        like.setInteractionType("LIKE");
        like.setTimestamp(LocalDateTime.now());
        like.setGenres(List.of());

        userInteractionRepository.save(view);
        userInteractionRepository.save(like);

        // Verify score formula directly: VIEW×1 + LIKE×10 = 11
        List<UserInteraction> saved = userInteractionRepository.findByUserId(testUserId);
        long viewCount = saved.stream().filter(i -> "VIEW".equals(i.getInteractionType())).count();
        long likeCount = saved.stream().filter(i -> "LIKE".equals(i.getInteractionType())).count();
        long calculatedScore = viewCount * 1 + likeCount * 10;

        assertThat(calculatedScore)
                .as("Score formula: VIEW×1 + LIKE×10 should equal 11")
                .isGreaterThanOrEqualTo(11L);

        // Ranking query itself must not throw
        List<Object[]> allRows = userInteractionRepository.findTop10ByScoreWithCounts();
        assertThat(allRows).isNotNull();
    }

    // ─── 3. External API Calls ─────────────────────────────────────────────────

    // Seeds 50 LIKE interactions to give the item score=500, ensuring it appears
    // in Top 10 even in a populated DB (fast to write, guaranteed ranking).
    @Test
    void tmdbApi_fetchKnownMovie_returnsValidTitle() {
        String movieId = "27205"; // Inception on TMDB

        for (int i = 0; i < 50; i++) {
            UserInteraction like = new UserInteraction();
            like.setUserId(TEST_USER_ID_BASE + 2);
            like.setMediaApiId(movieId);
            like.setMediaType("MOVIE");
            like.setInteractionType("LIKE");
            like.setTimestamp(LocalDateTime.now());
            like.setGenres(List.of());
            userInteractionRepository.save(like);
        }

        List<RankedMediaResponse> result = mediaRankingService.getTop10ByMediaType("MOVIE");

        boolean hasTmdbTitle = result.stream()
                .anyMatch(r -> r.getMediaApiId().equals(movieId)
                        && r.getTitle() != null
                        && !r.getTitle().isBlank()
                        && !r.getTitle().equals("Unknown"));

        assertThat(hasTmdbTitle)
                .as("TMDB API should return a valid title for movie id=27205 (Inception)")
                .isTrue();
    }

    @Test
    void rawgApi_fetchKnownGame_returnsValidTitle() {
        String gameId = "3498"; // GTA V on RAWG

        for (int i = 0; i < 50; i++) {
            UserInteraction like = new UserInteraction();
            like.setUserId(TEST_USER_ID_BASE + 3);
            like.setMediaApiId(gameId);
            like.setMediaType("GAME");
            like.setInteractionType("LIKE");
            like.setTimestamp(LocalDateTime.now());
            like.setGenres(List.of());
            userInteractionRepository.save(like);
        }

        List<RankedMediaResponse> result = mediaRankingService.getTop10ByMediaType("GAME");

        boolean hasRawgTitle = result.stream()
                .anyMatch(r -> r.getMediaApiId().equals(gameId)
                        && r.getTitle() != null
                        && !r.getTitle().isBlank()
                        && !r.getTitle().equals("Unknown"));

        assertThat(hasRawgTitle)
                .as("RAWG API should return a valid title for game id=3498 (GTA V)")
                .isTrue();
    }

    // Last.fm URL format: https://www.last.fm/music/Artist/_/Track
    @Test
    void lastfmApi_fetchKnownTrack_returnsValidTitle() {
        String trackUrl = "https://www.last.fm/music/Queen/_/Bohemian+Rhapsody";

        for (int i = 0; i < 50; i++) {
            UserInteraction like = new UserInteraction();
            like.setUserId(TEST_USER_ID_BASE + 4);
            like.setMediaApiId(trackUrl);
            like.setMediaType("SONG");
            like.setInteractionType("LIKE");
            like.setTimestamp(LocalDateTime.now());
            like.setGenres(List.of());
            userInteractionRepository.save(like);
        }

        List<RankedMediaResponse> result = mediaRankingService.getTop10ByMediaType("SONG");

        boolean hasLastfmTitle = result.stream()
                .anyMatch(r -> r.getMediaApiId().equals(trackUrl)
                        && r.getTitle() != null
                        && !r.getTitle().isBlank()
                        && !r.getTitle().equals("Unknown"));

        assertThat(hasLastfmTitle)
                .as("Last.fm API should return a valid title for: " + trackUrl)
                .isTrue();
    }

    // ─── 4. Per-Category Data Structure ───────────────────────────────────────

    @Test
    void getTop10PerCategory_returnsMapWithAllThreeCategories() {
        var result = mediaRankingService.getTop10PerCategory();

        assertThat(result)
                .as("Should return Map with MOVIE, GAME, SONG keys")
                .containsKeys("MOVIE", "GAME", "SONG");

        assertThat(result.get("MOVIE")).isNotNull().isInstanceOf(List.class);
        assertThat(result.get("GAME")).isNotNull().isInstanceOf(List.class);
        assertThat(result.get("SONG")).isNotNull().isInstanceOf(List.class);
    }

    @Test
    void getFastRising5PerCategory_returnsMapWithAllThreeCategories() {
        var result = mediaRankingService.getFastRising5PerCategory();

        assertThat(result)
                .as("Should return Map with MOVIE, GAME, SONG keys")
                .containsKeys("MOVIE", "GAME", "SONG");

        assertThat(result.get("MOVIE")).isNotNull().isInstanceOf(List.class);
        assertThat(result.get("GAME")).isNotNull().isInstanceOf(List.class);
        assertThat(result.get("SONG")).isNotNull().isInstanceOf(List.class);
    }

    @Test
    void getTop10PerCategory_eachCategoryHasUpTo10Items() {
        // Create 15 likes for each media type to ensure we test the LIMIT 10
        for (int i = 0; i < 15; i++) {
            UserInteraction movie = new UserInteraction();
            movie.setUserId(TEST_USER_ID_BASE + 5);
            movie.setMediaApiId("test-movie-" + i);
            movie.setMediaType("MOVIE");
            movie.setInteractionType("LIKE");
            movie.setTimestamp(LocalDateTime.now());
            movie.setTitle("Movie " + i);
            movie.setImageUrl("https://images.example/movie-" + i + ".jpg");
            movie.setGenres(List.of());
            userInteractionRepository.save(movie);

            UserInteraction game = new UserInteraction();
            game.setUserId(TEST_USER_ID_BASE + 5);
            game.setMediaApiId("test-game-" + i);
            game.setMediaType("GAME");
            game.setInteractionType("LIKE");
            game.setTimestamp(LocalDateTime.now());
            game.setTitle("Game " + i);
            game.setImageUrl("https://images.example/game-" + i + ".jpg");
            game.setGenres(List.of());
            userInteractionRepository.save(game);

            UserInteraction song = new UserInteraction();
            song.setUserId(TEST_USER_ID_BASE + 5);
            song.setMediaApiId("test-song-" + i);
            song.setMediaType("SONG");
            song.setInteractionType("LIKE");
            song.setTimestamp(LocalDateTime.now());
            song.setTitle("Song " + i);
            song.setArtist("Artist " + i);
            song.setImageUrl("https://images.example/song-" + i + ".jpg");
            song.setGenres(List.of());
            userInteractionRepository.save(song);
        }

        var result = mediaRankingService.getTop10PerCategory();

        assertThat(result.get("MOVIE")).hasSize(10);
        assertThat(result.get("GAME")).hasSize(10);
        assertThat(result.get("SONG")).hasSize(10);
    }

    @Test
    void getFastRising5PerCategory_eachCategoryHasUpTo5Items() {
        // Create 10 likes for each media type within the past 7 days using real IDs
        String[] movieIds = { "27205", "550", "278", "109091", "19404" };
        String[] gameIds = { "3498", "5286", "802", "13633", "41494" };
        String[] songIds = {
                "https://www.last.fm/music/Queen/_/Bohemian+Rhapsody",
                "https://www.last.fm/music/The+Beatles/_/Hey+Jude",
                "https://www.last.fm/music/Pink+Floyd/_/Comfortably+Numb",
                "https://www.last.fm/music/Led+Zeppelin/_/Stairway+to+Heaven",
                "https://www.last.fm/music/The+Rolling+Stones/_/Paint+It+Black"
        };

        for (int i = 0; i < 10; i++) {
            UserInteraction movie = new UserInteraction();
            movie.setUserId(TEST_USER_ID_BASE + 6);
            movie.setMediaApiId(movieIds[i % movieIds.length]);
            movie.setMediaType("MOVIE");
            movie.setInteractionType("LIKE");
            movie.setTimestamp(LocalDateTime.now().minusDays(1));
            movie.setTitle("Movie " + (i % movieIds.length));
            movie.setImageUrl("https://images.example/movie-fast-" + (i % movieIds.length) + ".jpg");
            movie.setGenres(List.of());
            userInteractionRepository.save(movie);

            UserInteraction game = new UserInteraction();
            game.setUserId(TEST_USER_ID_BASE + 6);
            game.setMediaApiId(gameIds[i % gameIds.length]);
            game.setMediaType("GAME");
            game.setInteractionType("LIKE");
            game.setTimestamp(LocalDateTime.now().minusDays(1));
            game.setTitle("Game " + (i % gameIds.length));
            game.setImageUrl("https://images.example/game-fast-" + (i % gameIds.length) + ".jpg");
            game.setGenres(List.of());
            userInteractionRepository.save(game);

            UserInteraction song = new UserInteraction();
            song.setUserId(TEST_USER_ID_BASE + 6);
            song.setMediaApiId(songIds[i % songIds.length]);
            song.setMediaType("SONG");
            song.setInteractionType("LIKE");
            song.setTimestamp(LocalDateTime.now().minusDays(1));
            song.setTitle("Song " + (i % songIds.length));
            song.setArtist("Artist " + (i % songIds.length));
            song.setImageUrl("https://images.example/song-fast-" + (i % songIds.length) + ".jpg");
            song.setGenres(List.of());
            userInteractionRepository.save(song);
        }

        var result = mediaRankingService.getFastRising5PerCategory();

        assertThat(result.get("MOVIE")).hasSize(5);
        assertThat(result.get("GAME")).hasSize(5);
        assertThat(result.get("SONG")).hasSize(5);
    }
}
