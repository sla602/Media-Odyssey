package com.mo.mediaodyssey.layout.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class AppConfigTest {

    @Test
    void restTemplate_usesHttpComponentsClientHttpRequestFactory() {
        RestTemplate restTemplate = new AppConfig().restTemplate();

        assertThat(restTemplate.getRequestFactory())
                .isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
    }
}