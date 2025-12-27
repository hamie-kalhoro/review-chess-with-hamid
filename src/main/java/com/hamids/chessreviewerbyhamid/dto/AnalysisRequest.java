package com.hamids.chessreviewerbyhamid.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnalysisRequest {
    @NotBlank(message = "FEN position is required")
    private String fen;

    private Integer depth = 15;

    private String mode = "bestmove";
}
