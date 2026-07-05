package com.mo.mediaodyssey.community.favorite;

import com.mo.mediaodyssey.recommendation.UserInteractionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private MediaRankingService rankingService;

    private void stubMetadataResponses() {
        when(restTemplate.getForEntity(anyString(), eq(byte[].class))).thenAnswer(invocation -> {
            String url = invocation.getArgument(0, String.class);
            if (url.contains("api.themoviedb.org")) {
                return ResponseEntity.ok("{\"title\":\"Inception\",\"poster_path\":\"/poster.jpg\"}".getBytes());
            }
            if (url.contains("api.rawg.io")) {
                return ResponseEntity
                        .ok("{\"name\":\"Grand Theft Auto V\",\"background_image\":\"https://images.example/gta.jpg\"}"
                                .getBytes());
            }
            if (url.contains("ws.audioscrobbler.com")) {
                return ResponseEntity.ok(
                        "{\"track\":{\"name\":\"Bohemian Rhapsody\",\"artist\":{\"name\":\"Queen\"},\"album\":{\"image\":[{\"#text\":\"\"},{\"#text\":\"\"},{\"#text\":\"https://images.example/mid.jpg\"},{\"#text\":\"https://images.example/large.jpg\"}]}}}"
                                .getBytes());
            }
            throw new AssertionError("Unexpected metadata URL: " + url);
        });
    }

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

    // getTop10() must call findTop10ByScoreWithCounts(), not the category-filtered
    // version
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
        // Use real IDs so the test exercises actual metadata enrichment logic
        List<Object[]> fakeRows = Arrays.asList(
                new Object[] { "27205", "MOVIE", 130L, 5L, 80L },
                new Object[] { "3498", "GAME", 95L, 3L, 65L },
                new Object[] { "https://www.last.fm/music/Queen/_/Bohemian+Rhapsody", "SONG", 50L, 1L, 40L });
        when(userInteractionRepository.findTop10ByScoreWithCounts()).thenReturn(fakeRows);
        stubMetadataResponses();

        List<RankedMediaResponse> result = rankingService.getTop10();

        assertThat(result).hasSize(3);
        verify(restTemplate, times(3)).getForEntity(anyString(), eq(byte[].class));
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
        // Use a real movie ID so the metadata fetch succeeds
        Object[] row = new Object[] { "27205", "MOVIE", 20L, 1L, 10L };
        List<Object[]> fakeRows = Collections.singletonList(row);
        when(userInteractionRepository.findTop10ByScoreWithCounts()).thenReturn(fakeRows);
        stubMetadataResponses();

        List<RankedMediaResponse> result = rankingService.getTop10();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMediaApiId()).isEqualTo("27205");
        assertThat(result.get(0).getTitle()).isEqualTo("Inception");
    }

    // Likes and views counts must be correctly passed through to the DTO
    @Test
    void getTop10_fallbackEntry_preservesLikesAndViews() {
        // Row format: [mediaApiId, mediaType, totalScore, likeCount, viewCount]
        // Use a real game ID so the metadata fetch succeeds
        Object[] row = new Object[] { "3498", "GAME", 35L, 3L, 5L };
        List<Object[]> fakeRows = Collections.singletonList(row);
        when(userInteractionRepository.findTop10ByScoreWithCounts()).thenReturn(fakeRows);
        stubMetadataResponses();

        List<RankedMediaResponse> result = rankingService.getTop10();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLikes()).isEqualTo(3L);
        assertThat(result.get(0).getViews()).isEqualTo(5L);
    }

    @Test
    void getTop10_metadataApiUnavailable_keepsFallbackEntry() {
        Object[] row = new Object[] { "offline-movie", "MOVIE", 20L, 1L, 10L, "", "", "" };
        when(userInteractionRepository.findTop10ByScoreWithCounts()).thenReturn(Collections.singletonList(row));
        when(restTemplate.getForEntity(anyString(), eq(byte[].class)))
                .thenThrow(new RuntimeException("metadata service unavailable"));

        List<RankedMediaResponse> result = rankingService.getTop10();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMediaApiId()).isEqualTo("offline-movie");
        assertThat(result.get(0).getTitle()).isEqualTo("offline-movie");
        assertThat(result.get(0).getTotalScore()).isEqualTo(20L);
    }

    // Ensure fetchJsonBody correctly handles gzip-compressed responses even when
    // the
    // Content-Encoding header is missing (magic-byte detection).
    @Test
    void getTop10_handlesGzipCompressedResponses() throws Exception {
        List<Object[]> fakeRows = Collections.singletonList(
                new Object[] { "27205", "MOVIE", 20L, 1L, 10L });
        when(userInteractionRepository.findTop10ByScoreWithCounts()).thenReturn(fakeRows);

        // gzipped JSON body for TMDB
        byte[] gzipped = gzipBytes("{\"title\":\"Inception\",\"poster_path\":\"/poster.jpg\"}");
        when(restTemplate.getForEntity(anyString(), eq(byte[].class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok(gzipped));

        List<RankedMediaResponse> result = rankingService.getTop10();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Inception");
    }

    @Test
    void getFastRising5PerCategory_backfillsMissingImagesForAllMediaTypes() {
        when(userInteractionRepository.findTop5TrendingLikesSinceAndMediaType(any(), eq("MOVIE")))
                .thenReturn(Collections.singletonList(new Object[] {
                        "27205",
                        "MOVIE",
                        4L,
                        "Inception",
                        "",
                        ""
                }));
        when(userInteractionRepository.findTop5TrendingLikesSinceAndMediaType(any(), eq("GAME")))
                .thenReturn(Collections.singletonList(new Object[] {
                        "3498",
                        "GAME",
                        4L,
                        "Grand Theft Auto V",
                        "",
                        ""
                }));
        when(userInteractionRepository.findTop5TrendingLikesSinceAndMediaType(any(), eq("SONG")))
                .thenReturn(Collections.singletonList(new Object[] {
                        "https://www.last.fm/music/Queen/_/Bohemian+Rhapsody",
                        "SONG",
                        4L,
                        "Bohemian Rhapsody",
                        "Queen",
                        ""
                }));
        when(userInteractionRepository.updateImageUrlForMediaIfMissing(
                eq("27205"),
                eq("MOVIE"),
                eq("https://image.tmdb.org/t/p/w500/poster.jpg"))).thenReturn(1);
        when(userInteractionRepository.updateImageUrlForMediaIfMissing(
                eq("3498"),
                eq("GAME"),
                eq("https://images.example/gta.jpg"))).thenReturn(1);
        when(userInteractionRepository.updateImageUrlForMediaIfMissing(
                eq("https://www.last.fm/music/Queen/_/Bohemian+Rhapsody"),
                eq("SONG"),
                eq("https://images.example/large.jpg"))).thenReturn(1);

        stubMetadataResponses();

        Map<String, List<RankedMediaResponse>> result = rankingService.getFastRising5PerCategory();

        assertThat(result.get("MOVIE")).hasSize(1);
        assertThat(result.get("GAME")).hasSize(1);
        assertThat(result.get("SONG")).hasSize(1);
        assertThat(result.get("MOVIE").get(0).getImageUrl()).isEqualTo("https://image.tmdb.org/t/p/w500/poster.jpg");
        assertThat(result.get("GAME").get(0).getImageUrl()).isEqualTo("https://images.example/gta.jpg");
        assertThat(result.get("SONG").get(0).getImageUrl()).isEqualTo("https://images.example/large.jpg");
        verify(userInteractionRepository, times(1)).updateImageUrlForMediaIfMissing(
                "27205",
                "MOVIE",
                "https://image.tmdb.org/t/p/w500/poster.jpg");
        verify(userInteractionRepository, times(1)).updateImageUrlForMediaIfMissing(
                "3498",
                "GAME",
                "https://images.example/gta.jpg");
        verify(userInteractionRepository, times(1)).updateImageUrlForMediaIfMissing(
                "https://www.last.fm/music/Queen/_/Bohemian+Rhapsody",
                "SONG",
                "https://images.example/large.jpg");
    }

    @Test
    void communityRanking_reusesCachedResolvedMetadataAcrossSections() {
        Object[] top10Row = new Object[] { "27205", "MOVIE", 20L, 1L, 10L, "Inception", "", "" };
        Object[] trendingRow = new Object[] { "27205", "MOVIE", 4L, "Inception", "", "https://image.tmdb.org/t/p/w500/poster.jpg" };

        when(userInteractionRepository.findTop10ByScoreWithCounts()).thenReturn(Collections.singletonList(top10Row));
        when(userInteractionRepository.findTop10ByScoreWithCountsAndMediaType("MOVIE"))
                .thenReturn(Collections.singletonList(top10Row));
        when(userInteractionRepository.findTop10ByScoreWithCountsAndMediaType("GAME"))
                .thenReturn(Collections.emptyList());
        when(userInteractionRepository.findTop10ByScoreWithCountsAndMediaType("SONG"))
                .thenReturn(Collections.emptyList());
        when(userInteractionRepository.findTop5TrendingLikesSinceAndMediaType(any(), eq("MOVIE")))
                .thenReturn(Collections.singletonList(trendingRow));
        when(userInteractionRepository.findTop5TrendingLikesSinceAndMediaType(any(), eq("GAME")))
                .thenReturn(Collections.emptyList());
        when(userInteractionRepository.findTop5TrendingLikesSinceAndMediaType(any(), eq("SONG")))
                .thenReturn(Collections.emptyList());
        when(userInteractionRepository.updateImageUrlForMediaIfMissing(
                eq("27205"),
                eq("MOVIE"),
                eq("https://image.tmdb.org/t/p/w500/poster.jpg"))).thenReturn(1);

        stubMetadataResponses();

        rankingService.getTop10PerCategory();
        rankingService.getFastRising5PerCategory();

        verify(restTemplate, times(1)).getForEntity(anyString(), eq(byte[].class));
        verify(userInteractionRepository, times(1)).updateImageUrlForMediaIfMissing(
                "27205",
                "MOVIE",
                "https://image.tmdb.org/t/p/w500/poster.jpg");
    }

    @Test
    void communityRanking_usesStoredImageWhenCachedMetadataExists() {
        Object[] uncachedRow = new Object[] { "27205", "MOVIE", 20L, 1L, 10L, "Inception", "", "" };
        Object[] storedImageRow = new Object[] {
                "27205",
                "MOVIE",
                4L,
                "Inception",
                "",
                "https://db.example/poster.jpg"
        };

        when(userInteractionRepository.findTop10ByScoreWithCounts()).thenReturn(Collections.singletonList(uncachedRow));
        when(userInteractionRepository.findTop5TrendingLikesSinceAndMediaType(any(), eq("MOVIE")))
                .thenReturn(Collections.singletonList(storedImageRow));
        when(userInteractionRepository.findTop5TrendingLikesSinceAndMediaType(any(), eq("GAME")))
                .thenReturn(Collections.emptyList());
        when(userInteractionRepository.findTop5TrendingLikesSinceAndMediaType(any(), eq("SONG")))
                .thenReturn(Collections.emptyList());
        when(userInteractionRepository.updateImageUrlForMediaIfMissing(
                eq("27205"),
                eq("MOVIE"),
                eq("https://image.tmdb.org/t/p/w500/poster.jpg"))).thenReturn(1);

        stubMetadataResponses();

        rankingService.getTop10();
        Map<String, List<RankedMediaResponse>> result = rankingService.getFastRising5PerCategory();

        assertThat(result.get("MOVIE")).hasSize(1);
        assertThat(result.get("MOVIE").get(0).getImageUrl()).isEqualTo("https://db.example/poster.jpg");
    }

    @Test
    void communityRanking_reusesCachedMetadataWhenImageUnavailable() {
        Object[] top10Row = new Object[] { "posterless", "MOVIE", 20L, 1L, 10L, "Posterless", "", "" };
        Object[] trendingRow = new Object[] { "posterless", "MOVIE", 4L, "Posterless", "", "" };

        when(userInteractionRepository.findTop10ByScoreWithCounts()).thenReturn(Collections.singletonList(top10Row));
        when(userInteractionRepository.findTop5TrendingLikesSinceAndMediaType(any(), eq("MOVIE")))
                .thenReturn(Collections.singletonList(trendingRow));
        when(userInteractionRepository.findTop5TrendingLikesSinceAndMediaType(any(), eq("GAME")))
                .thenReturn(Collections.emptyList());
        when(userInteractionRepository.findTop5TrendingLikesSinceAndMediaType(any(), eq("SONG")))
                .thenReturn(Collections.emptyList());
        when(restTemplate.getForEntity(anyString(), eq(byte[].class)))
                .thenReturn(ResponseEntity.ok("{\"title\":\"Posterless\",\"poster_path\":\"\"}".getBytes()));

        List<RankedMediaResponse> top10 = rankingService.getTop10();
        Map<String, List<RankedMediaResponse>> trending = rankingService.getFastRising5PerCategory();

        assertThat(top10.get(0).getImageUrl()).isEmpty();
        assertThat(trending.get("MOVIE").get(0).getImageUrl()).isEmpty();
        verify(restTemplate, times(1)).getForEntity(anyString(), eq(byte[].class));
        verify(userInteractionRepository, never()).updateImageUrlForMediaIfMissing(anyString(), anyString(), anyString());
    }

    // helper to produce gzipped bytes
    private static byte[] gzipBytes(String input) throws java.io.IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream gos = new java.util.zip.GZIPOutputStream(bos)) {
            gos.write(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        return bos.toByteArray();
    }
}
