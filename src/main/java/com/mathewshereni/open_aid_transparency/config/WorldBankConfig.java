package com.mathewshereni.open_aid_transparency.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Builds a RestClient pre-pointed at the World Bank API. Exposing it as a @Bean
 * means any service can inject a ready-to-use HTTP client without rebuilding it.
 */
@Configuration
public class WorldBankConfig {

    @Bean
    public RestClient worldBankRestClient(@Value("${worldbank.api.base-url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
