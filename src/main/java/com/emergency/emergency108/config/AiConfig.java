package com.emergency.emergency108.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Ai settings.
 *
 * @author anupam kushwaha
 */
@Configuration
public class AiConfig {

    @Value("${app.ai.enabled:true}")
    private boolean isAiEnabled;

    @Value("${app.ai.provider:rule_based}")
    private String aiProvider; // rule_based, spring_ai, etc.

    /**
     * Is ai enabled operation.
     * @return the boolean
     */
    public boolean isAiEnabled() {
        return isAiEnabled;
    }

    /**
     * Set ai enabled operation.
     * @param aiEnabled the aiEnabled
     */
    public void setAiEnabled(boolean aiEnabled) {
        isAiEnabled = aiEnabled;
    }

    /**
     * Get ai provider operation.
     * @return the String
     */
    public String getAiProvider() {
        return aiProvider;
    }
}
