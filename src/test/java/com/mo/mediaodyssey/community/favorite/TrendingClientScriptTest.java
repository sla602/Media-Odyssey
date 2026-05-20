package com.mo.mediaodyssey.community.favorite;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract tests for static/layout/js/trending.js.
 *
 * These tests intentionally verify source-level behavior markers rather than
 * executing browser DOM code. This keeps the suite inside JUnit/Maven while
 * still guarding the client-side responsibilities introduced by the server ->
 * client filtering shift:
 * - category filtering must apply to both Top 10 and Fast-Rising sections
 * - the page must fetch a single JSON payload from /community/data
 * - status/empty-state text must remain meaningful for users
 */
class TrendingClientScriptTest {

    private String readTrendingScript() throws IOException {
        // Read the shipped static JS exactly as served by Spring Boot resources.
        ClassPathResource resource = new ClassPathResource("static/layout/js/trending.js");
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    @Test
    void renderFiltered_filtersTop10AndTrendingBySelectedCategory() throws IOException {
        // Arrange: load current script content.
        String script = readTrendingScript();

        // Act/Assert:
        // verify category-specific filtering from per-category data structures,
        // and confirm the regression fix remains in place (trending renders filtered
        // items).
        assertThat(script)
                .contains("top10ByCategory")
                .contains("trendingByCategory")
                .contains("renderFiltered()")
                .contains("renderTop10(top10Items);")
                .contains("renderTrending(trendingItems);")
                .doesNotContain("renderTrending(allTrending);");
    }

    @Test
    void loadCommunityData_fetchesSingleEndpointAndRecordsViews() throws IOException {
        // Arrange
        String script = readTrendingScript();

        // Act/Assert:
        // the bootstrapping flow must fetch one endpoint, record initial views,
        // and trigger rendering from the fetched payload.
        assertThat(script)
                .contains("const res = await fetch(\"/community/data\");")
                .contains("renderFiltered();");
    }

    @Test
    void renderedItems_recordViewsOnlyOncePerMedia() throws IOException {
        String script = readTrendingScript();

        assertThat(script)
                .contains("const viewedMediaKeys = new Set();")
                .contains("recordVisibleMediaViews(items);")
                .contains("if (viewedMediaKeys.has(viewKey)) return;")
                .doesNotContain("allTop10.forEach(m => recordView(m.mediaApiId, m.mediaType));");
    }

    @Test
    void emptyStateMessages_remainMeaningfulForCategoryAndNoDataCases() throws IOException {
        // Arrange
        String script = readTrendingScript();

        // Act/Assert:
        // both sections should explain empty-category vs no-data states clearly.
        assertThat(script)
                .contains("No rankings yet — start interacting with media on the home page!")
                .contains("No ranked media in this category yet.")
                .contains("No trending data yet — start interacting with media on the home page!")
                .contains("No fast-rising media in this category yet.");
    }

    @Test
    void tabClick_updatesCurrentFilterAndRerendersClientSide() throws IOException {
        // Arrange
        String script = readTrendingScript();

        // Act/Assert:
        // clicking a category tab should update currentFilter and rerender
        // without requesting server-side category-specific endpoints.
        assertThat(script)
                .contains("currentFilter = normalizeMediaType(tab.dataset.type);")
                .contains("renderFiltered();");
    }

    @Test
    void loadCommunityData_handlesNonOkAndErrorPathsWithUserStatus() throws IOException {
        // Arrange
        String script = readTrendingScript();

        // Act/Assert:
        // both non-OK responses and catch blocks should provide user-visible
        // failure status text for Top 10 and trending panels.
        assertThat(script)
                .contains("if (!res.ok) {")
                .containsPattern("setStatus\\(\"top10Status\",\\s+\"Could not load ranking data\\.\"\\);")
                .containsPattern("setStatus\\(\"trendingStatus\",\\s+\"Could not load trending data\\.\"\\);")
                .contains("} catch (err) {");
    }
}
