package com.mo.mediaodyssey.layout.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AppConfig {

        /**
         * RestTemplate bean with configurable timeouts and HttpClient-based transport
         * to ensure robust handling of compressed responses.
         * - Connect timeout: 5s
         * - Read/socket timeout: 10s
         */
        @Bean
        public RestTemplate restTemplate() {
                ConnectionConfig connectionConfig = ConnectionConfig.custom()
                                .setConnectTimeout(Timeout.ofSeconds(5))
                                .build();

                PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder
                                .create()
                                .setDefaultConnectionConfig(connectionConfig)
                                .build();

                // Increase pool sizes to allow higher parallelism for external API calls
                connectionManager.setMaxTotal(200);
                connectionManager.setDefaultMaxPerRoute(50);

                CloseableHttpClient httpClient = HttpClients.custom()
                                .setConnectionManager(connectionManager)
                                .build();

                HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
                factory.setConnectionRequestTimeout(Duration.ofSeconds(5));
                factory.setReadTimeout(Duration.ofSeconds(10));
                return new RestTemplate(factory);
        }

        /**
         * ExecutorService bean for handling asynchronous recommendation processing.
         * - Shutdown hook ensures proper cleanup of resources.
         */
        @Bean(destroyMethod = "shutdown")
        public ExecutorService recommendationExecutor() {
                // Larger pool to support parallel external API fetches for recommendations
                return Executors.newFixedThreadPool(30);
        }
}
