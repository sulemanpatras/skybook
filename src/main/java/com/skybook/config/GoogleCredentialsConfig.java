package com.skybook.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;

@Configuration
public class GoogleCredentialsConfig {

    @Value("${GOOGLE_CREDENTIALS_JSON:}")
    private String credentialsJson;

    @PostConstruct
    public void writeCredentialsFile() {
        // Only run if env var is set (production)
        if (credentialsJson == null || credentialsJson.isBlank()) {
            System.out.println("GOOGLE_CREDENTIALS_JSON not set — using classpath file (local dev)");
            return;
        }

        try {
            // Write to a temp file the app can read
            File tempFile = new File(System.getProperty("java.io.tmpdir"), "google-credentials.json");
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(credentialsJson);
            }
            System.out.println("Google credentials written to: " + tempFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Failed to write Google credentials file: " + e.getMessage());
        }
    }

    public static String getCredentialsFilePath() {
        File tempFile = new File(System.getProperty("java.io.tmpdir"), "google-credentials.json");
        if (tempFile.exists()) {
            return "file:" + tempFile.getAbsolutePath();
        }
        // Fallback to classpath for local dev
        return "classpath:google-credentials.json";
    }
}