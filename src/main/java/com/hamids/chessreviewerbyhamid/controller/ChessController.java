package com.hamids.chessreviewerbyhamid.controller;

import com.hamids.chessreviewerbyhamid.dto.AnalysisRequest;
import com.hamids.chessreviewerbyhamid.dto.AnalysisResponse;
import com.hamids.chessreviewerbyhamid.service.ChessAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chess")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChessController {

    private final ChessAnalysisService chessAnalysisService;
    private final GameAnalysisService gameAnalysisService;

    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResponse> analyzePosition(@Valid @RequestBody AnalysisRequest request) {
        AnalysisResponse response = chessAnalysisService.analyzePosition(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/analyze-game")
    public ResponseEntity<GameAnalysisResponse> analyzeGame(@Valid @RequestBody GameAnalysisRequest request) {
        GameAnalysisResponse response = gameAnalysisService.analyzeGame(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Chess Reviewer API is running");
    }
}
