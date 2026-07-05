package com.mo.mediaodyssey.recommendation;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract tests for static/layout/js/homePage.js.
 *
 * These checks protect the client-side recommendation cache behavior:
 * - fresh cache entries can render instantly
 * - stale responses must not repaint the UI
 * - the tab left behind should be refreshed in the background
 */
class HomePageClientScriptTest {

    private String readHomePageScript() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/layout/js/homePage.js");
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    @Test
    void tabClick_keepsCachedResultsAndRefreshesPreviousTabInBackground() throws IOException {
        String script = readHomePageScript();

        assertThat(script)
                .contains("const previousMediaType = currentMediaType;")
                .contains("loadRecommendations(currentMediaType);")
                .contains("recCacheStale[previousMediaType] = true;")
                .contains("prefetchRecommendations(previousMediaType, true);")
                .doesNotContain("delete recCache[currentMediaType];")
                .doesNotContain("delete recCache[previousMediaType];");
    }

    @Test
    void prefetchKeepsAllFetchedMediaTypesCachedAndMarksThemFresh() throws IOException {
        String script = readHomePageScript();

        assertThat(script)
                .contains("const recCacheStale = {};")
                .contains("const recRefreshPromises = {};")
                .contains("if (recCache[mediaType] && !recCacheStale[mediaType]) {")
                .contains("recCache[mediaType] = data;")
                .contains("recCacheStale[mediaType] = false;")
                .contains("if (mediaType !== currentMediaType) {")
                .contains("async function prefetchRecommendations(mediaType, force = false) {")
                .contains("existingRefresh.catch(() => undefined).then(() => fetchAndCacheRecommendations(mediaType))")
                .contains("[\"GAME\", \"SONG\"].forEach(type => prefetchRecommendations(type));");
    }
}
