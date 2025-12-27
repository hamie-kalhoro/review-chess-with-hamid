package com.hamids.chessreviewerbyhamid.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameSummary {
    private double whiteAccuracy;
    private double blackAccuracy;
    private int whiteBrilliant;
    private int whiteGreat;
    private int whiteGood;
    private int whiteInaccuracy;
    private int whiteMistake;
    private int whiteBlunder;
    private int blackBrilliant;
    private int blackGreat;
    private int blackGood;
    private int blackInaccuracy;
    private int blackMistake;
    private int blackBlunder;
    private int totalMoves;
}
