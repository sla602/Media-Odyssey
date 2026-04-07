package com.mo.mediaodyssey.community.favorite;

import com.mo.mediaodyssey.recommendation.UserInteraction;
import com.mo.mediaodyssey.recommendation.UserInteractionRepository;
import com.mo.mediaodyssey.shared.services.CurrentAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CommunityController.
 *
 * Uses pure Mockito (no Spring context) to verify:
 * - communityPage() returns the correct view and model attributes
 * - likeMedia() and incrementView() deduplicate requests within the same
 * session
 * - No interaction is saved when the user is not authenticated
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
        // Sample ranked media list used across multiple tests
        sampleList = Arrays.asList(
                new RankedMediaResponse("11", "Inception", "", "MOVIE", "", 130L, 5L, 80L),
                new RankedMediaResponse("22", "The Witcher 3", "", "GAME", "", 95L, 3L, 65L),
                new RankedMediaResponse("33", "Bohemian Rhapsody", "Queen", "SONG", "", 50L, 1L, 40L));
    }

    // ─── communityPage() ──────────────────────────────────────────────────────

    // No category param → should call getTop10() and return the full ranked list
    @Test
    void communityPage_noCategory_callsGetTop10() {
        when(mediaRankingService.getTop10()).thenReturn(sampleList);
        when(mediaRankingService.getFastRising5()).thenReturn(List.of());

        org.springframework.ui.Model model = new org.springframework.ui.ExtendedModelMap();
        String view = communityController.communityPage(model, null);

        assertThat(view).isEqualTo("boardsLayout/features/trending");
        assertThat(model.asMap().get("mediaList")).isEqualTo(sampleList);
        verify(mediaRankingService, times(1)).getTop10();
        verify(mediaRankingService, never()).getTop10ByMediaType(any());
    }

    // Category "MOVIE" → should call getTop10ByMediaType() and set currentCat in
    // model
    @Test
    void communityPage_withMovieCategory_callsGetTop10ByMediaType() {
        when(mediaRankingService.normalizeMediaType("MOVIE")).thenReturn("MOVIE");
        when(mediaRankingService.getTop10ByMediaType("MOVIE"))
                .thenReturn(Arrays.asList(sampleList.get(0)));
        when(mediaRankingService.getFastRising5()).thenReturn(List.of());

        org.springframework.ui.Model model = new org.springframework.ui.ExtendedModelMap();
        String view = communityController.communityPage(model, "MOVIE");

        assertThat(view).isEqualTo("boardsLayout/features/trending");
        assertThat(model.asMap().get("currentCat")).isEqualTo("MOVIE");
        verify(mediaRankingService, times(1)).getTop10ByMediaType("MOVIE");
        verify(mediaRankingService, never()).getTop10();
    }

    // Blank category string should be treated the same as no filter
    @Test
    void communityPage_withBlankCategory_treatsAsNoFilter() {
        when(mediaRankingService.getTop10()).thenReturn(sampleList);
        when(mediaRankingService.getFastRising5()).thenReturn(List.of());

        org.springframework.ui.Model model = new org.springframework.ui.ExtendedModelMap();
        communityController.communityPage(model, "");

        verify(mediaRankingService, times(1)).getTop10();
        verify(mediaRankingService, never()).getTop10ByMediaType(any());
    }

    // Null category → currentCat model attribute should remain null
    @Test
    void communityPage_nullCategory_currentCatIsNull() {
        when(mediaRankingService.getTop10()).thenReturn(sampleList);
        when(mediaRankingService.getFastRising5()).thenReturn(List.of());

        org.springframework.ui.Model model = new org.springframework.ui.ExtendedModelMap();
        communityController.communityPage(model, null);

        assertThat(model.asMap().get("currentCat")).isNull();
    }

    // trending model attribute must always be populated (Fast-Rising section)
    @Test
    void communityPage_alwaysPopulatesTrendingAttribute() {
        when(mediaRankingService.getTop10()).thenReturn(sampleList);
        when(mediaRankingService.getFastRising5()).thenReturn(List.of());

        org.springframework.ui.Model model = new org.springframework.ui.ExtendedModelMap();
        communityController.communityPage(model, null);

        assertThat(model.asMap()).containsKey("trending");
        verify(mediaRankingService, times(1)).getFastRising5();
    }

    // ─── saveInteraction (via likeMedia / incrementView) ──────────────────────

    // Unauthenticated user (auth = null) → no UserInteraction should be saved
    @Test
    void saveInteraction_nullUserId_doesNotSaveToRepository() {
        when(mediaRankingService.normalizeMediaType("MOVIE")).thenReturn("MOVIE");

        // auth = null simulates unauthenticated call
        MockHttpSession session = new MockHttpSession();

        communityController.likeMedia("11", "MOVIE", session, null);

        verify(userInteractionRepository, never()).save(any(UserInteraction.class));
    }

    // Same user liking the same item twice in one session → second like should be
    // rejected
    @Test
    void likeMedia_duplicateInSameSession_secondCallReturnsFalse() {
        when(mediaRankingService.normalizeMediaType("MOVIE")).thenReturn("MOVIE");

        MockHttpSession session = new MockHttpSession();

        // First like
        var first = communityController.likeMedia("11", "MOVIE", session, null);
        // Second like same session
        var second = communityController.likeMedia("11", "MOVIE", session, null);

        assertThat(first.getBody()).containsEntry("liked", true);
        assertThat(second.getBody()).containsEntry("liked", false);
    }

    // Same user viewing the same item twice in one session → second view should be
    // rejected
    @Test
    void incrementView_duplicateInSameSession_secondCallReturnsFalse() {
        when(mediaRankingService.normalizeMediaType("GAME")).thenReturn("GAME");

        MockHttpSession session = new MockHttpSession();

        var first = communityController.incrementView("22", "GAME", session, null);
        var second = communityController.incrementView("22", "GAME", session, null);

        assertThat(first.getBody()).containsEntry("viewed", true);
        assertThat(second.getBody()).containsEntry("viewed", false);
    }

    // Liking two different items in one session → both should succeed
    @Test
    void likeMedia_differentMediaIds_bothReturnTrue() {
        when(mediaRankingService.normalizeMediaType("MOVIE")).thenReturn("MOVIE");

        MockHttpSession session = new MockHttpSession();

        var r1 = communityController.likeMedia("11", "MOVIE", session, null);
        var r2 = communityController.likeMedia("22", "MOVIE", session, null);

        assertThat(r1.getBody()).containsEntry("liked", true);
        assertThat(r2.getBody()).containsEntry("liked", true);
    }

    // Like and View for the same item are tracked independently in the session
    @Test
    void likeAndView_sameItem_trackedSeparately() {
        when(mediaRankingService.normalizeMediaType("SONG")).thenReturn("SONG");

        MockHttpSession session = new MockHttpSession();

        var liked = communityController.likeMedia("33", "SONG", session, null);
        var viewed = communityController.incrementView("33", "SONG", session, null);

        assertThat(liked.getBody()).containsEntry("liked", true);
        assertThat(viewed.getBody()).containsEntry("viewed", true);
    }
}