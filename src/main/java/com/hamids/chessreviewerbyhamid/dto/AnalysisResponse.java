package com.hamids.chessreviewerbyhamid.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponse {
    private boolean success;
    private String message;
    private String fen;

    // Best move info
    private String bestMove;
    private String bestMoveSan;
    private String ponder;

    // Evaluation
    private Integer evaluation;      // in centipawns
    private Integer mate;            // mate in X moves (null if no mate)
    private Double winChance;        // percentage 0-100

    // Continuation line
    private String continuation;
    private List<String> principalVariation;

    // Alternative moves with evaluations
    private List<MoveOption> topMoves;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MoveOption {
        private String move;           // UCI notation (e2e4)
        private String san;            // Standard notation (e4)
        private Integer evaluation;    // centipawns
        private Integer mate;
        private Double winChance;
        private Double evalChange;     // how much this differs from best move
        private String classification; // Brilliant, Great, Best, Good, Inaccuracy, Mistake, Blunder
        private String continuation;
    }
}