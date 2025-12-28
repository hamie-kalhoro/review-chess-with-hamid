package com.hamids.chessreviewerbyhamid.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Configuration
public class StockfishConfig {

    @Value("${stockfish.api.url:https://stockfish.online/api/s/v2.php}")
    private String apiUrl;

    @Value("${stockfish.engine.enabled:true}")
    private boolean engineEnabled;

    @Value("${stockfish.engine.path:}")
    private String enginePath;

    @Value("${stockfish.api.fallback.enabled:true}")
    private boolean apiFallbackEnabled;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public String stockfishExecutablePath() throws IOException {
        if (!engineEnabled) {
            log.info("Local Stockfish engine is disabled");
            return null;
        }

        // Try custom path first
        if (enginePath != null && !enginePath.trim().isEmpty()) {
            Path customPath = Paths.get(enginePath);
            if (Files.exists(customPath) && Files.isExecutable(customPath)) {
                log.info("Using custom Stockfish path: {}", enginePath);
                return customPath.toAbsolutePath().toString();
            }
        }

        // Auto-detect OS and use bundled executable
        String os = System.getProperty("os.name").toLowerCase();
        String executableName;

        if (os.contains("win")) {
            executableName = "stockfish-windows-x86-64-avx2.exe";
        } else if (os.contains("mac")) {
            executableName = "stockfish-macos";
        } else if (os.contains("nix") || os.contains("nux")) {
            executableName = "stockfish-linux";
        } else {
            log.warn("Unsupported OS: {}, falling back to API", os);
            return null;
        }

        // Try to extract from resources
        try {
            Path tempDir = Files.createTempDirectory("stockfish");
            Path targetPath = tempDir.resolve(executableName);

            try (var inputStream = getClass().getClassLoader()
                    .getResourceAsStream("stockfish/" + executableName)) {
                if (inputStream != null) {
                    Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

                    // Make executable on Unix-like systems
                    if (!os.contains("win")) {
                        Process chmod = new ProcessBuilder("chmod", "+x", targetPath.toString()).start();
                        chmod.waitFor();
                    }

                    log.info("Extracted Stockfish to: {}", targetPath);
                    return targetPath.toAbsolutePath().toString();
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract Stockfish from resources: {}", e.getMessage());
        }

        // Try system PATH
        log.info("Falling back to system PATH for Stockfish");
        return "stockfish";
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getEnginePath() {
        return enginePath;
    }

    public boolean isEngineEnabled() {
        return engineEnabled;
    }

    public boolean isApiFallbackEnabled() {
        return apiFallbackEnabled;
    }
}