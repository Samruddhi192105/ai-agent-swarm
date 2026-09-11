package com.example.agentswarm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    // Shared HTTP client used by LLM providers to call out to Gemini / Groq / etc.
    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }
}
