package com.mo.mediaodyssey.community.favorite;

import com.mo.mediaodyssey.recommendation.UserInteractionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MediaRankingService.
 *
 * Covers:
 * - normalizeMediaType(): input normalisation (MOVIE / GAME / SONG)
 * - getTop10(): calls correct repository method and returns ranked list
 * - getTop10ByMediaType(): filters by category and normalises input
 * - enrichScoreRows(): fallback behaviour and display rating range
 */
@ExtendWith(MockitoExtension.class)
class MediaRankingServiceTest {

    @Mock
    private UserInteractionRepository userInteractionRepository;

    @InjectMocks
    private MediaRankingService rankingService;

    // ─── normalizeMediaType Tests ──────────────────────────────────────────────

    // "MOVIE", "movies", "Movies" should all map to "MOVIE"
    @Test
    void normalizeMediaType_movie_returnsNormalizedString() {
        assertThat(rankingService.normalizeMediaType("MOVIE")).isEqualTo("MOVIE");
        assertThat(rankingService.normalizeMediaType("movies")).isEqualTo("MOVIE");
        assertThat(rankingService.normalizeMediaType("Movies")).isEqualTo("MOVIE");
    }

    // "GAME" and "games" should both map to "GAME"
    @Test
    void normalizeMediaType_game_returnsNormalizedString() {
        assertThat(rankingService.normalizeMediaType("GAME")).isEqualTo("GAME");
        assertThat(rankingService.normalizeMediaType("games")).isEqualTo("GAME");
    }

    // "MUSIC", "song", "SONGS" should all map to "SONG" (internal canonical value)
    @Test
    void normalizeMediaType_music_returnsSong() {
        assertThat(rankingService.normalizeMediaType("MUSIC")).isEqualTo("SONG");
        assertThat(rankingService.normalizeMediaType("song")).isEqualTo("SONG");
        assertThat(rankingService.normalizeMediaType("SONGS")).isEqualTo("SONG");
    }

    // Null input should return empty string without throwing
    @Test
    void normalizeMediaType_null_returnsEmptyString() {
        assertThat(rankingService.normalizeMediaType(null)).isEqualTo("");
    }

    // Unknown input should be uppercased and returned as-is
    @Test
    void normalizeMediaType_unknown_returnsUppercase() {
        assertThat(rankingService.normalizeMediaType("podcast")).isEqualTo("PODCAST");
    }

    // ─── getTop10 Tests ────────────────────────────────────────────────────────

    // Empty DB → should return an empty list without errors
    @Test
    void getTop10_noInteractions_returnsEmptyList() {
        when(userInteractionRepository.findTop10ByScoreWithCounts())
                .thenReturn(Collections.emptyList());

        List<RankedMediaResponse> result = rankingService.getTop10();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    // getTop10() must call findTop10ByScoreWithCounts(), not the category-filtered version
    @Test
    void getTop10_callsCorrectRepositoryMethod() {
        when(userInteractionRepository.findTop10ByScoreWithCounts())
                .thenReturn(Collections.emptyList());

        rankingService.getTop10();

        verify(userInteractionRepository, times(1)).findTop10ByScoreWithCounts();
        verify(userInteractionRepository, never()).findTop10ByScoreWithCountsAndMediaType(any());
    }

    // Result count must equal the number of rows returned by the repository —
    // fails if enrichScoreRows silently drops or duplicates entries
    @Test
    void getTop10_resultSizeMatchesRepositoryRowCount() {
        // Row format: [mediaApiId, mediaType, totalScore, likeCount, viewCount]
        List<Object[]> fakeRows = Arrays.asList(
                new Object[]{"11", "UNKNOWN", 130L, 5L, 80L},
                new Object[]{"22", "UNKNOWN",  95L, 3L, 65L},
                new Object[]{"33", "UNKNOWN",  50L, 1L, 40L}
        );
        when(userInteractionRepository.findTop10ByScoreWithCounts()).thenReturn(fakeRows);

        List<RankedMediaResponse> result = rankingService.getTop10();

        // Must return exactly 3 — not 0, not 2, not more
        assertThat(result).hasSize(3);
    }

    // ─── getTop10ByMediaType Tests ─────────────────────────────────────────────

    // "movies" input should be normalised to "MOVIE" before hitting the repository
    @Test
    void getTop10ByMediaType_movie_callsRepositoryWithNormalizedType() {
        when(userInteractionRepository.findTop10ByScoreWithCountsAndMediaType("MOVIE"))
                .thenReturn(Collections.emptyList());

        rankingService.getTop10ByMediaType("movies");

        verify(userInteractionRepository, times(1))
                .findTop10ByScoreWithCountsAndMediaType("MOVIE");
    }

    // "MUSIC" should be normalised to "SONG" (the DB canonical value)
    @Test
    void getTop10ByMediaType_music_normalizesToSong() {
        when(userInteractionRepository.findTop10ByScoreWithCountsAndMediaType("SONG"))
                .thenReturn(Collections.emptyList());

        rankingService.getTop10ByMediaType("MUSIC");

        verify(userInteractionRepository, times(1))
                .findTop10ByScoreWithCountsAndMediaType("SONG");
    }

    // Unknown category → repository returns nothing → result should be empty
    @Test
    void getTop10ByMediaType_unknownCategory_returnsEmptyList() {
        when(userInteractionRepository.findTop10ByScoreWithCountsAndMediaType("PODCAST"))
                .thenReturn(Collections.emptyList());

        List<RankedMediaResponse> result = rankingService.getTop10ByMediaType("PODCAST");

        assertThat(result).isEmpty();
    }

    // ─── enrichScoreRows / RankedMediaResponse structure Tests ────────────────

    // Fallback entry must preserve the exact mediaApiId from the DB row —
    // fails if enrichScoreRows replaces or drops the id
    @Test
    void getTop10_fallbackEntry_preservesMediaApiId() {
        // Row format: [mediaApiId, mediaType, totalScore, likeCount, viewCount]
        Object[] row = new Object[]{"abc-123", "UNKNOWN", 20L, 1L, 10L};
        List<Object[]> fakeRows = Collections.singletonList(row);
        when(userInteractionRepository.findTop10ByScoreWithCounts()).thenReturn(fakeRows);

        List<RankedMediaResponse> result = rankingService.getTop10();

        assertThat(result).hasSize(1);
        // Must preserve the exact mediaApiId — fails if enrichScoreRows loses it
        assertThat(result.get(0).getMediaApiId()).isEqualTo("abc-123");
        // Must use "Unknown" as fallback title — fails if null or empty is returned
        assertThat(result.get(0).getTitle()).isEqualTo("Unknown");
    }

    // Likes and views counts must be correctly passed through to the DTO
    @Test
    void getTop10_fallbackEntry_preservesLikesAndViews() {
        // Row format: [mediaApiId, mediaType, totalScore, likeCount, viewCount]
        Object[] row = new Object[]{"xyz-456", "UNKNOWN", 35L, 3L, 5L};
        List<Object[]> fakeRows = Collections.singletonList(row);
        when(userInteractionRepository.findTop10ByScoreWithCounts()).thenReturn(fakeRows);

        List<RankedMediaResponse> result = rankingService.getTop10();

        assertThat(result).hasSize(1);
        // likes and views must be preserved from row[3] and row[4]
        assertThat(result.get(0).getLikes()).isEqualTo(3L);
        assertThat(result.get(0).getViews()).isEqualTo(5L);
    }
}