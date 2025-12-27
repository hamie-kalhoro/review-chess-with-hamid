package com.hamids.chessreviewerbyhamid.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisResponse {
    private boolean success;
    private String bestMove;
    private Integer evaluation;
    private String continuation;
    private String mate;
    private String message;
}
