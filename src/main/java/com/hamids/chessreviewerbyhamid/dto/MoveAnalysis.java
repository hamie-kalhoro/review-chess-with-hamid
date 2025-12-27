package com.hamids.chessreviewerbyhamid.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MoveAnalysis {
    private int moveNumber;
    private String move;
    private String moveClassification;
    private Double evaluationBefore;
    private Double evaluationAfter;
    private Double evaluationDrop;
    private String bestMove;
    private String fen;
    private String player;
}
