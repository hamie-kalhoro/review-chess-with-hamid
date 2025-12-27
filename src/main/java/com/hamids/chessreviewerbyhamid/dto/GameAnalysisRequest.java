package com.hamids.chessreviewerbyhamid.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GameAnalysisRequest {
    @NotBlank(message = "PGN is required")
    private String pgn;

    private Integer depth = 15;
}
