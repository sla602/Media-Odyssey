package com.mo.mediaodyssey.community.favorite;

import com.mo.mediaodyssey.recommendation.UserInteraction;
import com.mo.mediaodyssey.recommendation.UserInteractionRepository;
import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.shared.services.CurrentAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CommunityController.
 *
 * Uses pure Mockito (no Spring context) and verifies three behaviors:
 * - Page endpoint contract: communityPage() always returns the trending view
 * and does not perform server-side category filtering/population.
 * - Data endpoint contract: communityData() returns both "top10" and
 * "trending" payloads from MediaRankingService.
 * - Interaction persistence rules: likes/views are saved only for authenticated
 * users and duplicate interactions are deduplicated per session.
 */
@ExtendWith(MockitoExtension.class)
class CommunityControllerTest {

    @Mock
    private MediaRankingService mediaRankingService;

    @Mock
    private UserInteractionRepository userInteractionRepository;

    @Mock
    private CurrentAccountService currentAccountService;

    @InjectMocks
    private CommunityController communityController;

    private List<RankedMediaResponse> sampleList;

    @BeforeEach
    void setUp() {
        // Shared fixture: this list represents stable ranking data returned by
        // MediaRankingService for tests that exercise /community/data mapping.
        // Using a deterministic list keeps assertions focused on controller
        // behavior (shape + pass-through) rather than random values.
        sampleList = Arrays.asList(
                new RankedMediaResponse("11", "Inception", "", "MOVIE", "", 130L, 5L, 80L),
                new RankedMediaResponse("22", "The Witcher 3", "", "GAME", "", 95L, 3L, 65L),
                new RankedMediaResponse("33", "Bohemian Rhapsody", "Queen", "SONG", "", 50L, 1L, 40L));
    }

    // ─── communityPage() ──────────────────────────────────────────────────────

    // No category query parameter: the server returns only the page view.
    // Data is fetched separately from /community/data by client-side code.
    @Test
    void communityPage_noCategory_returnsTrendingView() {
        // Arrange: empty model and no category query parameter.
        org.springframework.ui.Model model = new org.springframework.ui.ExtendedModelMap();

        // Act: call the page endpoint method.
        String view = communityController.communityPage(model, null);

        // Assert:
        // 1) server returns the trending page template,
        // 2) server does not inject ranking data (client fetches it),
        // 3) ranking service is not invoked for page render.
        assertThat(view).isEqualTo("boardsLayout/features/trending");
        assertThat(model.asMap()).doesNotContainKeys("mediaList", "trending", "currentCat");
        verifyNoInteractions(mediaRankingService);
    }

    // Even with a category provided, category filtering is client-side.
    // The server still returns only the page view without ranking data.
    @Test
    void communityPage_withMovieCategory_returnsTrendingView() {
        // Arrange: category is present, but filtering responsibility is client-side.
        org.springframework.ui.Model model = new org.springframework.ui.ExtendedModelMap();

        // Act: invoke the same page endpoint with MOVIE category.
        String view = communityController.communityPage(model, "MOVIE");

        // Assert: identical contract as null category case (view-only endpoint).
        assertThat(view).isEqualTo("boardsLayout/features/trending");
        assertThat(model.asMap()).doesNotContainKeys("mediaList", "trending", "currentCat");
        verifyNoInteractions(mediaRankingService);
    }

    // Blank category is treated the same way: no server-side ranking work.
    @Test
    void communityPage_withBlankCategory_returnsTrendingView() {
        // Arrange: blank category string should be treated as no effective category.
        org.springframework.ui.Model model = new org.springframework.ui.ExtendedModelMap();

        // Act
        String view = communityController.communityPage(model, "");

        // Assert: still view-only, no server-side ranking work.
        assertThat(view).isEqualTo("boardsLayout/features/trending");
        assertThat(model.asMap()).doesNotContainKeys("mediaList", "trending", "currentCat");
        verifyNoInteractions(mediaRankingService);
    }

    // Null category does not create a populated category model value.
    @Test
    void communityPage_nullCategory_currentCatIsNull() {
        // Arrange
        org.springframework.ui.Model model = new org.springframework.ui.ExtendedModelMap();

        // Act
        communityController.communityPage(model, null);

        // Assert: currentCat is not populated when category is absent.
        // We also guard that no ranking lookup was triggered.
        assertThat(model.asMap().get("currentCat")).isNull();
        verifyNoInteractions(mediaRankingService);
    }

    // /community/data must include both keys expected by the client script.
    @Test
    void communityData_returnsTop10AndTrending() {
        // Arrange: mock per-category data with "ALL" key for cross-category top 10 and
        // top 5 trending
        java.util.Map<String, List<RankedMediaResponse>> top10PerCategory = new java.util.LinkedHashMap<>();
        top10PerCategory.put("ALL", sampleList); // ALL tab gets full top 10 overall
        top10PerCategory.put("MOVIE", sampleList.size() > 0 ? sampleList.subList(0, 1) : List.of());
        top10PerCategory.put("GAME",
                sampleList.size() > 1 ? sampleList.subList(1, Math.min(2, sampleList.size())) : List.of());
        top10PerCategory.put("SONG", sampleList.size() > 2 ? sampleList.subList(2, sampleList.size()) : List.of());

        List<RankedMediaResponse> trendingList = List.of(sampleList.get(0));
        java.util.Map<String, List<RankedMediaResponse>> trendingPerCategory = new java.util.LinkedHashMap<>();
        // ALL tab gets top 5 trending
        trendingPerCategory.put("ALL",
                trendingList.size() > 0 ? trendingList.subList(0, Math.min(5, trendingList.size())) : List.of());
        trendingPerCategory.put("MOVIE", trendingList);
        trendingPerCategory.put("GAME", List.of());
        trendingPerCategory.put("SONG", List.of());

        when(mediaRankingService.getTop10PerCategory()).thenReturn(top10PerCategory);
        when(mediaRankingService.getFastRising5PerCategory()).thenReturn(trendingPerCategory);

        // Act: call JSON endpoint backing the trending page (null auth =
        // unauthenticated).
        var response = communityController.communityData(null);

        // Assert:
        // 1) HTTP 200 response,
        // 2) payload contains both required keys as per-category maps with "ALL" for
        // cross-category,
        // 3) values are passed through from the service as-is,
        // 4) both service methods are invoked exactly once.
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("top10")).isEqualTo(top10PerCategory);
        assertThat(response.getBody().get("trending")).isEqualTo(trendingPerCategory);
        verify(mediaRankingService).getTop10PerCategory();
        verify(mediaRankingService).getFastRising5PerCategory();
    }

    // ─── saveInteraction (via likeMedia / incrementView) ──────────────────────

    // Without an authenticated principal, interactions are rejected for saving.
    @Test
    void saveInteraction_nullUserId_doesNotSaveToRepository() {
        // Arrange: normalization still happens, but no authenticated principal exists.
        when(mediaRankingService.normalizeMediaType("MOVIE")).thenReturn("MOVIE");

        // auth = null simulates unauthenticated call
        MockHttpSession session = new MockHttpSession();

        // Act: attempt to like media while unauthenticated.
        communityController.likeMedia("11", "MOVIE", session, null);

        // Assert: repository save must never happen for anonymous requests.
        verify(userInteractionRepository, never()).save(any(UserInteraction.class));
    }

    // For the same session + same media + LIKE:
    // first call persists, second call is deduplicated.
    @Test
    void likeMedia_duplicateInSameSession_savesOnceThenReturnsFalse() {
        // Arrange:
        // 1) media type normalization,
        // 2) authenticated user resolution,
        // 3) fresh HTTP session used for deduplication scope.
        when(mediaRankingService.normalizeMediaType("MOVIE")).thenReturn("MOVIE");
        Authentication auth = mock(Authentication.class);
        User currentUser = new User("like-user@example.invalid", "unused");
        currentUser.setId(42L);
        when(currentAccountService.getCurrentAccount(auth)).thenReturn(currentUser);

        MockHttpSession session = new MockHttpSession();

        // Act: same LIKE interaction issued twice in the same session.
        var first = communityController.likeMedia("11", "MOVIE", session, auth);
        var second = communityController.likeMedia("11", "MOVIE", session, auth);

        // Assert API-level contract: first accepted, second deduplicated.
        assertThat(first.getBody()).containsEntry("liked", true);
        assertThat(second.getBody()).containsEntry("liked", false);

        // Assert persistence contract: exactly one stored interaction with
        // normalized media metadata and LIKE type.
        ArgumentCaptor<UserInteraction> captor = ArgumentCaptor.forClass(UserInteraction.class);
        verify(userInteractionRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(42L);
        assertThat(captor.getValue().getMediaApiId()).isEqualTo("11");
        assertThat(captor.getValue().getMediaType()).isEqualTo("MOVIE");
        assertThat(captor.getValue().getInteractionType()).isEqualTo("LIKE");
    }

    // For the same session + same media + VIEW:
    // first call persists, second call is deduplicated.
    @Test
    void incrementView_duplicateInSameSession_savesOnceThenReturnsFalse() {
        // Arrange equivalent setup for VIEW interaction path.
        when(mediaRankingService.normalizeMediaType("GAME")).thenReturn("GAME");
        Authentication auth = mock(Authentication.class);
        User currentUser = new User("view-user@example.invalid", "unused");
        currentUser.setId(84L);
        when(currentAccountService.getCurrentAccount(auth)).thenReturn(currentUser);

        MockHttpSession session = new MockHttpSession();

        // Act: same VIEW interaction submitted twice in one session.
        var first = communityController.incrementView("22", "GAME", session, auth);
        var second = communityController.incrementView("22", "GAME", session, auth);

        // Assert API-level dedupe response.
        assertThat(first.getBody()).containsEntry("viewed", true);
        assertThat(second.getBody()).containsEntry("viewed", false);

        // Assert persistence-level dedupe and captured interaction fields.
        ArgumentCaptor<UserInteraction> captor = ArgumentCaptor.forClass(UserInteraction.class);
        verify(userInteractionRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(84L);
        assertThat(captor.getValue().getMediaApiId()).isEqualTo("22");
        assertThat(captor.getValue().getMediaType()).isEqualTo("GAME");
        assertThat(captor.getValue().getInteractionType()).isEqualTo("VIEW");
    }

    // Different media IDs are independent dedupe keys and both are persisted.
    @Test
    void likeMedia_differentMediaIds_bothReturnTrue() {
        // Arrange: same user/session, but different media IDs.
        when(mediaRankingService.normalizeMediaType("MOVIE")).thenReturn("MOVIE");
        Authentication auth = mock(Authentication.class);
        User currentUser = new User("multi-like-user@example.invalid", "unused");
        currentUser.setId(100L);
        when(currentAccountService.getCurrentAccount(auth)).thenReturn(currentUser);

        MockHttpSession session = new MockHttpSession();

        // Act: LIKE two distinct media items.
        var r1 = communityController.likeMedia("11", "MOVIE", session, auth);
        var r2 = communityController.likeMedia("22", "MOVIE", session, auth);

        // Assert: dedupe key includes media identifier, so both succeed and persist.
        assertThat(r1.getBody()).containsEntry("liked", true);
        assertThat(r2.getBody()).containsEntry("liked", true);
        verify(userInteractionRepository, times(2)).save(any(UserInteraction.class));
    }

    // LIKE and VIEW are separate interaction types for the same media item.
    @Test
    void likeAndView_sameItem_trackedSeparately() {
        // Arrange: same user/session/media, but different interaction types.
        when(mediaRankingService.normalizeMediaType("SONG")).thenReturn("SONG");
        Authentication auth = mock(Authentication.class);
        User currentUser = new User("like-view-user@example.invalid", "unused");
        currentUser.setId(101L);
        when(currentAccountService.getCurrentAccount(auth)).thenReturn(currentUser);

        MockHttpSession session = new MockHttpSession();

        // Act: submit LIKE then VIEW for the same item.
        var liked = communityController.likeMedia("33", "SONG", session, auth);
        var viewed = communityController.incrementView("33", "SONG", session, auth);

        // Assert: both succeed because interaction type is part of the dedupe key.
        assertThat(liked.getBody()).containsEntry("liked", true);
        assertThat(viewed.getBody()).containsEntry("viewed", true);
        verify(userInteractionRepository, times(2)).save(any(UserInteraction.class));
    }
}
