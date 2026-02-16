package com.emergency.emergency108.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @PostConstruct
    public void initializeFirebase() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                log.info("Firebase Admin SDK already initialized");
                return;
            }

            GoogleCredentials credentials;
            String base64Config = System.getenv("FIREBASE_SERVICE_ACCOUNT_BASE64");

            if (base64Config != null && !base64Config.isEmpty()) {
                // Production: Use Environment Variable
                log.info("Initializing Firebase from Environment Variable (Base64)");
                try (InputStream is = new java.io.ByteArrayInputStream(
                        java.util.Base64.getDecoder().decode(base64Config))) {
                    credentials = GoogleCredentials.fromStream(is);
                }
            } else {
                // Local: Use File
                log.info("Initializing Firebase from Classpath Resource");
                InputStream serviceAccount = new ClassPathResource("firebase-service-account.json").getInputStream();
                credentials = GoogleCredentials.fromStream(serviceAccount);
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK initialized successfully");

        } catch (IOException | IllegalArgumentException e) {
            log.error("Failed to initialize Firebase Admin SDK", e);
            throw new RuntimeException("Failed to initialize Firebase", e);
        }
    }
}
