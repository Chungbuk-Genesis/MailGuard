package com.example.MailGuard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAIConfig {
    
    @Value("${openai.api.key}")
    private String apiKey;
    
    @Value("${openai.model:gpt-4o-mini}")
    private String model;
    
    @Value("${openai.max.tokens:1000}")
    private int maxTokens;
    
    @Value("${openai.temperature:0.3}")
    private double temperature;
    
    public String getApiKey() {
        return apiKey;
    }
    
    public String getModel() {
        return model;
    }
    
    public int getMaxTokens() {
        return maxTokens;
    }
    
    public double getTemperature() {
        return temperature;
    }
    
    public static final String API_URL = "https://api.openai.com/v1/chat/completions";
}