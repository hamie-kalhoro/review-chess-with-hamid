package com.hamids.chessreviewerbyhamid.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameAnalysisResponse {
    private boolean success;
    private String message;
    private List<MoveAnalysis> moves;
    private GameSummary summary;
}
