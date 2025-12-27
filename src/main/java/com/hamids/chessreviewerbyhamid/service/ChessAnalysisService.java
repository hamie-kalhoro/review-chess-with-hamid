package com.hamids.chessreviewerbyhamid.service;

import com.hamids.chessreviewerbyhamid.config.StockfishConfig;
import com.hamids.chessreviewerbyhamid.dto.AnalysisRequest;
import com.hamids.chessreviewerbyhamid.dto.AnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChessAnalysisService {

    private final StockfishConfig stockfishConfig;
    private Process stockfishProcess;
    private BufferedReader reader;
    private BufferedWriter writer;

    @PostConstruct
    public void initStockfish() {
        try {
            ProcessBuilder pb = new ProcessBuilder(stockfishConfig.getEnginePath());
            pb.redirectErrorStream(true);
            stockfishProcess = pb.start();
            reader = new BufferedReader(new InputStreamReader(stockfishProcess.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(stockfishProcess.getOutputStream()));

            sendCommand("uci");
            waitForResponse("uciok");
            sendCommand("isready");
            waitForResponse("readyok");

            log.info("Stockfish initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize Stockfish: ", e);
        }
    }

    @PreDestroy
    public void closeStockfish() {
        try {
            if (writer != null) {
                sendCommand("quit");
                writer.close();
            }
            if (reader != null) reader.close();
            if (stockfishProcess != null) {
                stockfishProcess.waitFor(5, TimeUnit.SECONDS);
                stockfishProcess.destroyForcibly();
            }
            log.info("Stockfish closed successfully");
        } catch (Exception e) {
            log.error("Error closing Stockfish: ", e);
        }
    }

    public synchronized AnalysisResponse analyzePosition(AnalysisRequest request) {
        try {
            int depth = request.getDepth() != null ? request.getDepth() : 15;

            sendCommand("ucinewgame");
            sendCommand("position fen " + request.getFen());
            sendCommand("go depth " + depth);

            String bestMove = null;
            Integer evaluation = null;
            Integer mate = null;
            String ponder = null;

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("info depth")) {
                    if (line.contains("score cp ")) {
                        evaluation = extractScore(line);
                    } else if (line.contains("score mate ")) {
                        mate = extractMate(line);
                    }
                }
                if (line.startsWith("bestmove")) {
                    String[] parts = line.split(" ");
                    if (parts.length >= 2) {
                        bestMove = parts[1];
                    }
                    if (parts.length >= 4 && "ponder".equals(parts[2])) {
                        ponder = parts[3];
                    }
                    break;
                }
            }

            return AnalysisResponse.builder()
                    .success(true)
                    .bestMove(bestMove)
                    .evaluation(evaluation)
                    .mate(mate)
                    .ponder(ponder)
                    .message("Analysis successful")
                    .build();

        } catch (Exception e) {
            log.error("Error analyzing position: ", e);
            return AnalysisResponse.builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build();
        }
    }

    private void sendCommand(String command) throws IOException {
        writer.write(command + "\n");
        writer.flush();
    }

    private void waitForResponse(String expected) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains(expected)) break;
        }
    }

    private Integer extractScore(String line) {
        try {
            int idx = line.indexOf("score cp ") + 9;
            String scoreStr = line.substring(idx).split(" ")[0];
            return Integer.parseInt(scoreStr);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer extractMate(String line) {
        try {
            int idx = line.indexOf("score mate ") + 11;
            String mateStr = line.substring(idx).split(" ")[0];
            return Integer.parseInt(mateStr);
        } catch (Exception e) {
            return null;
        }
    }
}
