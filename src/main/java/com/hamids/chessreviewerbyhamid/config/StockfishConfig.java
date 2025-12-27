package com.hamids.chessreviewerbyhamid.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Configuration
public class StockfishConfig {

//    @Value("${stockfish.api.key:}")
//    private String apiKey;

//    @Value("${stockfish.api.url}")
//    private String apiUrl;

    @Value("${stockfish.engine.enabled:false}")
    private boolean engineEnabled;

    @Value("${stockfish.engine.path:stockfish}")
    private String enginePath;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public String stockfishExecutablePath() throws IOException {
        if (!engineEnabled) {
            return null;
        }

        // If custom path provided, use it
        if (!enginePath.equals("stockfish")) {
            return enginePath;
        }

        // Auto-detect OS and set executable
        String os = System.getProperty("os.name").toLowerCase();
        String executableName;

        if (os.contains("win")) {
            executableName = "stockfish.exe";
        } else if (os.contains("mac") || os.contains("nix") || os.contains("nux")) {
            executableName = "stockfish";
        } else {
            throw new UnsupportedOperationException("OS not supported: " + os);
        }

        // Try to extract from resources
        Path targetPath = Paths.get("temp/stockfish/" + executableName);
        Files.createDirectories(targetPath.getParent());

        try (var inputStream = getClass().getClassLoader()
                .getResourceAsStream("stockfish/" + executableName)) {
            if (inputStream != null) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

                // Make executable on Unix-like systems
                if (!os.contains("win")) {
                    new ProcessBuilder("chmod", "+x", targetPath.toString()).start().waitFor();
                }

                return targetPath.toAbsolutePath().toString();
            }
        } catch (Exception e) {
            // Fallback to system stockfish
        }

        return "stockfish"; // Use system PATH
    }

//    public String getApiKey() {
//        return apiKey;
//    }
//
//    public String getApiUrl() {
//        return apiUrl;
//    }
public String getEnginePath() {
    return enginePath;
}

    public boolean isEngineEnabled() {
        return engineEnabled;
    }
}
