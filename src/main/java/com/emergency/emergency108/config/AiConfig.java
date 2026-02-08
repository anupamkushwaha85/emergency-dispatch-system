package com.emergency.emergency108.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Value("${app.ai.enabled:true}")
    private boolean isAiEnabled;

    @Value("${app.ai.provider:rule_based}")
    private String aiProvider; // rule_based, spring_ai, etc.

    public boolean isAiEnabled() {
        return isAiEnabled;
    }

    public void setAiEnabled(boolean aiEnabled) {
        isAiEnabled = aiEnabled;
    }

    public String getAiProvider() {
        return aiProvider;
    }
}
