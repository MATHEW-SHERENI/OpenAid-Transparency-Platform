package com.mathewshereni.open_aid_transparency.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * RestClient pre-pointed at the UN SDG API (https://unstats.un.org/sdgapi).
 * Same pattern as WorldBankConfig - one ready-to-use HTTP client as a bean.
 */
@Configuration
public class UnSdgConfig {

    @Bean
    public RestClient unSdgRestClient(@Value("${unsdg.api.base-url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
