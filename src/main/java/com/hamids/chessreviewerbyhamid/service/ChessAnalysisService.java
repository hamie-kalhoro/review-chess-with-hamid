package com.hamids.chessreviewerbyhamid.service;

import com.hamids.chessreviewer.config.StockfishConfig;
import com.hamids.chessreviewer.dto.AnalysisRequest;
import com.hamids.chessreviewer.dto.AnalysisResponse;
import com.hamids.chessreviewer.dto.StockfishApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChessAnalysisService {

    private final RestTemplate restTemplate;
    private final StockfishConfig stockfishConfig;

    public AnalysisResponse analyzePosition(AnalysisRequest request) {
        try {
            String url = buildApiUrl(request);
            log.info("Calling Stockfish API: {}", url);

            StockfishApiResponse apiResponse = restTemplate.getForObject(url, StockfishApiResponse.class);

            if (apiResponse != null && apiResponse.isSuccess()) {
                return mapToAnalysisResponse(apiResponse);
            } else {
                return new AnalysisResponse(false, null, null, null, null, null, "Failed to analyze position");
            }

        } catch (Exception e) {
            log.error("Error analyzing position: ", e);
            return new AnalysisResponse(false, null, null, null, null, null, "Error: " + e.getMessage());
        }
    }

    private String buildApiUrl(AnalysisRequest request) {
        return UriComponentsBuilder.fromHttpUrl(stockfishConfig.getApiUrl())
                .queryParam("fen", request.getFen())
                .queryParam("depth", request.getDepth())
                .queryParam("mode", request.getMode())
                .toUriString();
    }

    private AnalysisResponse mapToAnalysisResponse(StockfishApiResponse apiResponse) {
        // Parse bestmove string (format: "bestmove e2e4 ponder e7e5")
        String bestMove = null;
        String ponder = null;

        if (apiResponse.getBestmove() != null) {
            String[] parts = apiResponse.getBestmove().split(" ");
            if (parts.length >= 2) {
                bestMove = parts[1]; // The actual move after "bestmove"
            }
            if (parts.length >= 4 && "ponder".equals(parts[2])) {
                ponder = parts[3]; // The ponder move
            }
        }

        return new AnalysisResponse(
                true,
                bestMove,
                apiResponse.getEvaluation(),
                apiResponse.getContinuation(),
                apiResponse.getMate(),
                ponder,
                "Analysis successful"
        );
    }
}
