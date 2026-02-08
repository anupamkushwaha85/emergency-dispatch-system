package com.hackathon.emergency108.service;

import com.hackathon.emergency108.config.AiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AiAssistanceService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistanceService.class);

    private final AiConfig aiConfig;

    public AiAssistanceService(AiConfig aiConfig) {
        this.aiConfig = aiConfig;
    }

    /**
     * Fallback Rule-Based Logic (Source of Truth).
     * Returns deterministic advice based on triage inputs.
     */
    public String getRuleBasedAdvice(String type, boolean isConscious, boolean isBleeding) {
        if (!isConscious) {
            return "CRITICAL: Check breathing. If not breathing, start CPR immediately. Push hard and fast in the center of the chest.";
        }
        if (isBleeding) {
            return "Apply firm direct pressure to the wound with a clean cloth. Do not remove the cloth if it soaks through. Keep the patient warm.";
        }
        if ("ACCIDENT".equalsIgnoreCase(type)) {
            return "Do not move the patient unless there is immediate danger (fire/explosion). Keep them warm and talk to them to keep them calm.";
        }

        // Default conscious/stable advice
        return "Stay calm. Keep the patient warm and comfortable. Do not give them anything to eat or drink. Wait for the ambulance.";
    }

    /**
     * Main entry point for AI Assistance.
     * Currently routed to Rule-Based engine, but ready for Spring AI integration.
     */
    public String generateAssistance(String emergencyType, Map<String, Object> triageData) {
        if (!aiConfig.isAiEnabled()) {
            log.info("AI Disabled. Returning standard wait message.");
            return "Help is on the way. Please wait for the ambulance.";
        }

        // Triage Data Extraction
        boolean isConscious = triageData.containsKey("conscious") && Boolean.TRUE.equals(triageData.get("conscious"));
        boolean isBleeding = triageData.containsKey("bleeding") && Boolean.TRUE.equals(triageData.get("bleeding"));

        // Logical Gateway
        // Future Integration:
        // if ("spring_ai".equals(aiConfig.getAiProvider())) {
        // try {
        // return callSpringAi(emergencyType, triageData);
        // } catch (Exception e) {
        // log.error("AI Provider Failed, falling back to rules: {}", e.getMessage());
        // }
        // }

        log.info("Generating Rule-Based Advice for Type: {}, Conscious: {}, Bleeding: {}", emergencyType, isConscious,
                isBleeding);
        return getRuleBasedAdvice(emergencyType, isConscious, isBleeding);
    }

    /**
     * Placeholder for Spring AI / LLM Integration.
     * Use strict prompt engineering to ensure safety.
     * 
     * @param emergencyType The type of emergency
     * @param triageData    Structured triage inputs
     * @return Generated advice or throws exception to trigger fallback
     */
    private String callSpringAi(String emergencyType, Map<String, Object> triageData) {
        // Implementation would use ChatClient or similar Spring AI component
        // String prompt = "You are an emergency medical assistant. Provide short, safe
        // instructions for...";
        // return chatClient.call(prompt);
        throw new UnsupportedOperationException("Spring AI not yet configured");
    }
}
