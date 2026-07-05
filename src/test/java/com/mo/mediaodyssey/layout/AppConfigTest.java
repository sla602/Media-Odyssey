package com.mo.mediaodyssey.layout;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.mo.mediaodyssey.layout.config.AppConfig;

import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;

class AppConfigTest {

    @Test
    void restTemplate_usesHttpComponentsClientHttpRequestFactory() {
        RestTemplate restTemplate = new AppConfig().restTemplate();

        assertThat(restTemplate.getRequestFactory())
                .isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
    }

    @Test
    void recommendationExecutor_createsExecutorService() {
        ExecutorService executor = new AppConfig().recommendationExecutor();

        try {
            assertThat(executor).isNotNull();
            assertThat(executor.isShutdown()).isFalse();
        } finally {
            executor.shutdownNow();
        }
    }
}
