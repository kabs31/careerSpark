package com.kabilan.careerSpark.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConfigurationProperties(prefix = "app.ollama")
@Getter
@Setter
public class OllamaConfig {

    private String baseUrl = "http://localhost:11434";
    private String model = "llama3";

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}