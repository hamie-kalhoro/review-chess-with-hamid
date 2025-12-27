package com.hamids.chessreviewerbyhamid.dto;

import lombok.Data;

@Data
public class StockfishApiResponse {
    private boolean success;
    private String evaluation;
    private String mate;
    private String bestmove;
    private String continuation;
}
