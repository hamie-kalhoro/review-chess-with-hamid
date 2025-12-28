package com.hamids.chessreviewerbyhamid.service;

import com.hamids.chessreviewerbyhamid.config.StockfishConfig;
import com.hamids.chessreviewerbyhamid.dto.AnalysisRequest;
import com.hamids.chessreviewerbyhamid.dto.AnalysisResponse;
import com.hamids.chessreviewerbyhamid.dto.AnalysisResponse.MoveOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChessAnalysisService {

    private final StockfishConfig stockfishConfig;
    private final RestTemplate restTemplate;

    private Process stockfishProcess;
    private BufferedReader reader;
    private BufferedWriter writer;
    private boolean usingLocalEngine = false;

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final Map<String, String> SAN_CACHE = new ConcurrentHashMap<>();

    // Opening book positions
    private static final Set<String> BOOK_POSITIONS = Set.of(
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR",
            "rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR",
            "rnbqkbnr/pppppppp/8/8/2P5/8/PP1PPPPP/RNBQKBNR",
            "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR",
            "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R",
            "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR",
            "rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR"
    );

    @PostConstruct
    public void initStockfish() {
        try {
            String enginePath = stockfishConfig.stockfishExecutablePath();

            if (enginePath != null) {
                log.info("Attempting to start local Stockfish from: {}", enginePath);

                ProcessBuilder pb = new ProcessBuilder(enginePath);
                pb.redirectErrorStream(true);
                stockfishProcess = pb.start();
                reader = new BufferedReader(new InputStreamReader(stockfishProcess.getInputStream()));
                writer = new BufferedWriter(new OutputStreamWriter(stockfishProcess.getOutputStream()));

                // Initialize Stockfish
                sendCommand("uci");
                String response = waitForResponse("uciok", 5000);
                if (response.contains("uciok")) {
                    sendCommand("setoption name Threads value 4");
                    sendCommand("setoption name Hash value 1024");
                    sendCommand("setoption name Skill Level value 20");
                    sendCommand("isready");
                    waitForResponse("readyok", 2000);
                    usingLocalEngine = true;
                    log.info("Local Stockfish initialized successfully");
                } else {
                    log.warn("Local Stockfish initialization failed");
                    closeLocalEngine();
                }
            }

            if (!usingLocalEngine && stockfishConfig.isApiFallbackEnabled()) {
                log.info("Using Stockfish API fallback");
            }

        } catch (Exception e) {
            log.error("Failed to initialize Stockfish: {}", e.getMessage());
            closeLocalEngine();
        }
    }

    public synchronized AnalysisResponse analyzePosition(AnalysisRequest request) {
        try {
            if (usingLocalEngine) {
                return analyzeWithLocalEngine(request);
            } else if (stockfishConfig.isApiFallbackEnabled()) {
                return analyzeWithAPI(request);
            } else {
                return AnalysisResponse.builder()
                        .success(false)
                        .message("No Stockfish engine available")
                        .build();
            }
        } catch (Exception e) {
            log.error("Error analyzing position: {}", e.getMessage());

            // Try API as fallback if local engine fails
            if (usingLocalEngine && stockfishConfig.isApiFallbackEnabled()) {
                log.info("Falling back to API after local engine error");
                return analyzeWithAPI(request);
            }

            return AnalysisResponse.builder()
                    .success(false)
                    .message("Analysis failed: " + e.getMessage())
                    .build();
        }
    }

    private AnalysisResponse analyzeWithLocalEngine(AnalysisRequest request) throws IOException {
        int depth = request.getDepth() != null ? Math.min(request.getDepth(), 30) : 15;
        int multiPv = 5;
        String fen = request.getFen();

        // Clean FEN
        fen = fen.trim();
        if (!isValidFEN(fen)) {
            throw new IllegalArgumentException("Invalid FEN format");
        }

        // Check if position is in opening book
        boolean isBookPosition = isInOpeningBook(fen.split(" ")[0]);

        sendCommand("ucinewgame");
        sendCommand("setoption name MultiPV value " + multiPv);
        sendCommand("position fen " + fen);
        sendCommand("isready");
        waitForResponse("readyok", 1000);
        sendCommand("go depth " + depth + " movetime 30000");

        Map<Integer, LineAnalysis> analyses = new HashMap<>();
        String bestMoveUci = null;
        String ponder = null;

        String line;
        long startTime = System.currentTimeMillis();
        long timeout = 35000; // 35 seconds timeout

        while ((line = readLineWithTimeout(timeout - (System.currentTimeMillis() - startTime))) != null) {
            if (line.startsWith("info") && line.contains("multipv")) {
                int pvNum = extractMultiPv(line);
                LineAnalysis analysis = new LineAnalysis();
                analysis.evaluation = extractScore(line);
                analysis.mate = extractMate(line);
                analysis.pv = extractPv(line);
                if (!analysis.pv.isEmpty()) {
                    analysis.move = analysis.pv.get(0);
                }
                analyses.put(pvNum, analysis);
            }

            if (line.startsWith("bestmove")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    bestMoveUci = parts[1];
                }
                if (parts.length >= 4 && "ponder".equals(parts[2])) {
                    ponder = parts[3];
                }
                break;
            }

            // Check timeout
            if (System.currentTimeMillis() - startTime > timeout) {
                sendCommand("stop");
                break;
            }
        }

        if (analyses.isEmpty()) {
            throw new RuntimeException("No analysis received from engine");
        }

        LineAnalysis bestLine = analyses.get(1);
        Integer bestEval = bestLine != null ? bestLine.evaluation : null;

        List<MoveOption> topMoves = new ArrayList<>();
        for (int i = 1; i <= multiPv; i++) {
            LineAnalysis analysis = analyses.get(i);
            if (analysis == null || analysis.move == null) continue;

            double evalChange = 0;
            if (bestEval != null && analysis.evaluation != null) {
                evalChange = (bestEval - analysis.evaluation) / 100.0;
            }

            String san = convertUciToSan(analysis.move, fen);
            String classification = classifyMove(
                    evalChange,
                    i == 1,
                    isBookPosition,
                    analysis.evaluation,
                    analysis.mate
            );

            String continuation = analysis.pv.size() > 1
                    ? String.join(" ", analysis.pv.subList(1, Math.min(5, analysis.pv.size())))
                    : null;

            topMoves.add(MoveOption.builder()
                    .move(analysis.move)
                    .san(san)
                    .evaluation(analysis.evaluation)
                    .mate(analysis.mate)
                    .winChance(calculateWinChance(analysis.evaluation, analysis.mate))
                    .evalChange(evalChange)
                    .classification(classification)
                    .continuation(continuation)
                    .build());
        }

        String bestSan = topMoves.isEmpty() ? bestMoveUci : topMoves.get(0).getSan();

        return AnalysisResponse.builder()
                .success(true)
                .fen(fen)
                .bestMove(bestMoveUci)
                .bestMoveSan(bestSan)
                .ponder(ponder)
                .evaluation(bestLine != null ? bestLine.evaluation : null)
                .mate(bestLine != null ? bestLine.mate : null)
                .winChance(calculateWinChance(
                        bestLine != null ? bestLine.evaluation : null,
                        bestLine != null ? bestLine.mate : null))
                .topMoves(topMoves)
                .message("Analysis successful")
                .build();
    }

    private AnalysisResponse analyzeWithAPI(AnalysisRequest request) {
        try {
            int depth = Math.min(request.getDepth() != null ? request.getDepth() : 15, 20);
            String fen = request.getFen();

            Map<String, Object> apiRequest = new HashMap<>();
            apiRequest.put("fen", fen);
            apiRequest.put("depth", depth);

            // Using a public Stockfish API
            String apiUrl = "https://stockfish.online/api/s/v2.php";

            Map<String, String> params = new HashMap<>();
            params.put("fen", fen);
            params.put("depth", String.valueOf(depth));

            String response = restTemplate.getForObject(apiUrl + "?fen={fen}&depth={depth}",
                    String.class, params);

            // Parse API response
            return parseApiResponse(response, fen);

        } catch (Exception e) {
            log.error("API analysis failed: {}", e.getMessage());
            return AnalysisResponse.builder()
                    .success(false)
                    .message("API analysis failed: " + e.getMessage())
                    .build();
        }
    }

    private AnalysisResponse parseApiResponse(String response, String fen) {
        try {
            // Simple parsing for demo - adjust based on actual API response
            if (response == null || !response.contains("\"success\":true")) {
                throw new RuntimeException("API returned error");
            }

            // Parse JSON-like response (adjust based on actual API)
            String bestMove = extractFromResponse(response, "bestmove", "\"");
            String evalStr = extractFromResponse(response, "evaluation", "\"");

            // Mock response for demonstration
            return AnalysisResponse.builder()
                    .success(true)
                    .fen(fen)
                    .bestMove(bestMove != null ? bestMove : "e2e4")
                    .bestMoveSan(bestMove != null ? convertUciToSan(bestMove, fen) : "e4")
                    .evaluation(evalStr != null ? Integer.parseInt(evalStr) : 25)
                    .winChance(calculateWinChance(evalStr != null ? Integer.parseInt(evalStr) : 25, null))
                    .message("Analyzed using Stockfish API")
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse API response: " + e.getMessage());
        }
    }

    private String extractFromResponse(String response, String key, String delimiter) {
        try {
            int idx = response.indexOf("\"" + key + "\":");
            if (idx == -1) return null;

            int start = response.indexOf(delimiter, idx + key.length() + 3);
            int end = response.indexOf(delimiter, start + 1);

            if (start != -1 && end != -1) {
                return response.substring(start + 1, end);
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private String classifyMove(double evalLoss, boolean isBest, boolean isBook,
                                Integer currentEval, Integer mate) {
        if (isBook && isBest) {
            return "Book";
        }

        // Found forced mate
        if (mate != null && mate > 0 && isBest) {
            return "Brilliant";
        }

        if (isBest) {
            if (currentEval != null && currentEval > 200) {
                return "Great";
            }
            return "Best";
        }

        if (evalLoss <= 0.05) {
            return "Excellent";
        } else if (evalLoss <= 0.15) {
            return "Great";
        } else if (evalLoss <= 0.30) {
            return "Good";
        } else if (evalLoss <= 0.50) {
            return "Inaccuracy";
        } else if (evalLoss <= 1.0) {
            return "Mistake";
        } else {
            return "Blunder";
        }
    }

    private boolean isValidFEN(String fen) {
        if (fen == null || fen.trim().isEmpty()) return false;
        String[] parts = fen.split(" ");
        return parts.length >= 4;
    }

    private boolean isInOpeningBook(String fenPosition) {
        return BOOK_POSITIONS.contains(fenPosition);
    }

    private Double calculateWinChance(Integer evaluation, Integer mate) {
        if (mate != null) {
            return mate > 0 ? 99.5 : 0.5;
        }
        if (evaluation == null) return 50.0;

        double cp = evaluation / 100.0;
        return 50.0 + 50.0 * (2.0 / (1.0 + Math.exp(-0.00368208 * cp)) - 1.0);
    }

    private String convertUciToSan(String uciMove, String fen) {
        String cacheKey = fen + "|" + uciMove;
        if (SAN_CACHE.containsKey(cacheKey)) {
            return SAN_CACHE.get(cacheKey);
        }

        // Simple conversion for common moves (for demo)
        // In production, use a proper chess library like chesslib
        try {
            if (uciMove == null || uciMove.length() < 4) return uciMove;

            String from = uciMove.substring(0, 2);
            String to = uciMove.substring(2, 4);

            // Simple pawn moves
            if (uciMove.length() == 4) {
                char fromFile = from.charAt(0);
                char toFile = to.charAt(0);

                if (fromFile == toFile) {
                    // Pawn push
                    String san = to;
                    SAN_CACHE.put(cacheKey, san);
                    return san;
                } else {
                    // Pawn capture
                    String san = fromFile + "x" + to;
                    SAN_CACHE.put(cacheKey, san);
                    return san;
                }
            }

            return uciMove; // Fallback to UCI
        } catch (Exception e) {
            return uciMove;
        }
    }

    private void sendCommand(String command) throws IOException {
        if (writer != null) {
            writer.write(command + "\n");
            writer.flush();
            log.debug("Stockfish command: {}", command);
        }
    }

    private String waitForResponse(String expected, long timeoutMs) throws IOException {
        long startTime = System.currentTimeMillis();
        String line;

        while ((line = readLineWithTimeout(timeoutMs - (System.currentTimeMillis() - startTime))) != null) {
            if (line.contains(expected)) {
                return line;
            }
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                break;
            }
        }
        return "";
    }

    private String readLineWithTimeout(long timeoutMs) throws IOException {
        if (reader == null) return null;

        Future<String> future = executorService.submit(() -> {
            try {
                return reader.readLine();
            } catch (IOException e) {
                return null;
            }
        });

        try {
            return future.get(Math.max(timeoutMs, 1), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private int extractMultiPv(String line) {
        try {
            int idx = line.indexOf("multipv ");
            if (idx == -1) return 1;
            String rest = line.substring(idx + 8);
            return Integer.parseInt(rest.split(" ")[0]);
        } catch (Exception e) {
            return 1;
        }
    }

    private Integer extractScore(String line) {
        try {
            int idx = line.indexOf("score cp ");
            if (idx == -1) return null;
            String rest = line.substring(idx + 9);
            return Integer.parseInt(rest.split(" ")[0]);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer extractMate(String line) {
        try {
            int idx = line.indexOf("score mate ");
            if (idx == -1) return null;
            String rest = line.substring(idx + 11);
            return Integer.parseInt(rest.split(" ")[0]);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> extractPv(String line) {
        try {
            int idx = line.indexOf(" pv ");
            if (idx == -1) return Collections.emptyList();
            return Arrays.asList(line.substring(idx + 4).split(" "));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @PreDestroy
    public void closeLocalEngine() {
        try {
            if (writer != null) {
                sendCommand("quit");
                writer.close();
            }
            if (reader != null) reader.close();
            if (stockfishProcess != null) {
                stockfishProcess.destroyForcibly();
            }
            usingLocalEngine = false;
            log.info("Stockfish engine closed");
        } catch (Exception e) {
            log.error("Error closing Stockfish: {}", e.getMessage());
        }
    }

    private static class LineAnalysis {
        String move;
        Integer evaluation;
        Integer mate;
        List<String> pv = new ArrayList<>();
    }
}