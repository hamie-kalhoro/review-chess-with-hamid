package com.hamids.chessreviewerbyhamid.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisResponse {
    private boolean success;
    private String bestMove;
    private Integer evaluation;
    private String continuation;
    private Integer mate;
    private String ponder;
    private String message;
}
