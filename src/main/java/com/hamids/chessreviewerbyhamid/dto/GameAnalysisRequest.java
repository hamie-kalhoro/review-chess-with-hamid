package com.hamids.chessreviewerbyhamid.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameAnalysisRequest {
    private String pgn;
    private Integer depth;
}
