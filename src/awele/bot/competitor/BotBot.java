package awele.bot.competitor;

import awele.bot.CompetitorBot;
import awele.core.Board;
import awele.core.InvalidBotException;

import java.util.HashMap;

public class BotBot extends CompetitorBot {
    private static final int MAX_DEPTH = 3;

    public BotBot() throws InvalidBotException {
        addAuthor("Aaro Karhu");
        setBotName("KalaKala");
    }

    static class CacheEntry {
        public double score;
        public int depth;

        public CacheEntry(double score, int depth) {
            this.score = score;
            this.depth = depth;
        }
    }

    private static final HashMap<Long, CacheEntry> cache = new HashMap<>();

    private Double checkCache(long hash, int depth) {
        CacheEntry entry = cache.get(hash);
        if (entry != null && entry.depth <= depth) {
            return entry.score;
        }
        return null;
    }

    private void updateCache(long hash, double score, int depth) {
        cache.put(hash, new CacheEntry(score, depth));
    }


    @Override
    public void initialize() {
    }

    @Override
    public void learn() {
    }

    @Override
    public void finish() {
    }

    @Override
    public double[] getDecision(Board board) {
        double[] scores = new double[Board.NB_HOLES];
        for (int i = 0; i < Board.NB_HOLES; i++) {
            if (board.getPlayerHoles()[i] > 0) { // Validate move
                Board copy = (Board) board.clone();
                try {
                    double[] decision = new double[Board.NB_HOLES];
                    decision[i] = 1; // Mark the move
                    copy = copy.playMoveSimulationBoard (copy.getCurrentPlayer (), decision);
                    scores[i] = minMax(copy, MAX_DEPTH, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false);
                } catch (InvalidBotException e) {
                    e.printStackTrace();
                }
            } else {
                scores[i] = Double.NEGATIVE_INFINITY;
            }
        }
        return scores;
    }

    private double minMax(Board board, int depth, double alpha, double beta, boolean isMaximizingPlayer) throws InvalidBotException {
        long hash = generateGameStateHash(board, isMaximizingPlayer, depth);
        Double cachedScore = checkCache(hash, depth);
        if (cachedScore != null) {
            return cachedScore;
        }


        if (depth == 0 || gameEndingCondition(board)) {
            return evaluateBoard(board, isMaximizingPlayer);
        }

        if (isMaximizingPlayer) {
            double maxEval = Double.NEGATIVE_INFINITY;
            // Iterate over possible moves
            for (int i = 0; i < Board.NB_HOLES; i++) {
                if (board.getPlayerHoles()[i] > 0) {
                    Board copy = (Board) board.clone();
                    double[] decision = new double[Board.NB_HOLES];
                    decision[i] = 1; // Simulate the move
                    copy = copy.playMoveSimulationBoard(copy.getCurrentPlayer(), decision);
                    double eval = minMax(copy, depth - 1, alpha, beta, false);
                    maxEval = Math.max(maxEval, eval);
                    alpha = Math.max(alpha, maxEval);
                    if (beta <= alpha) {
                        break; // beta cut-off
                    }
                }
            }
            updateCache(hash, maxEval, depth);
            return maxEval;
        } else {
            double minEval = Double.POSITIVE_INFINITY;
            // Iterate over possible moves
            for (int i = 0; i < Board.NB_HOLES; i++) {
                if (board.getPlayerHoles()[i] > 0) {
                    Board copy = (Board) board.clone();
                    double[] decision = new double[Board.NB_HOLES];
                    decision[i] = 1; // Simulate the move
                    copy = copy.playMoveSimulationBoard(copy.getCurrentPlayer(), decision);
                    double eval = minMax(copy, depth - 1, alpha, beta, true);
                    minEval = Math.min(minEval, eval);
                    beta = Math.min(beta, minEval);
                    if (beta <= alpha) {
                        break; // alpha cut-off
                    }
                }
            }
            updateCache(hash, minEval, depth);
            return minEval;
        }
    }

    private boolean gameEndingCondition(Board board) {
        return board.getScore(0) >= 25 || board.getScore(1) >= 25 || board.getNbSeeds() <= 6;
    }

    private double evaluateBoard(Board board, boolean isMaximizingPlayer) {
        int currentPlayerIndex = isMaximizingPlayer ? board.getCurrentPlayer() : Board.otherPlayer(board.getCurrentPlayer());
        int opponentIndex = Board.otherPlayer(currentPlayerIndex);
        double scoreDifference = board.getScore(currentPlayerIndex) - board.getScore(opponentIndex);

        double boardLayoutScore = evaluateBoardLayout(board);

        return scoreDifference + boardLayoutScore;
    }

    private double evaluateBoardLayout(Board board) {
        double layoutScore = 0;
        int[] playerHoles = board.getPlayerHoles(); // Current player's holes
        int[] opponentHoles = board.getOpponentHoles(); // Opponent's holes

        // Adjust strategy based on the state of the game
        for (int seeds : playerHoles) {
            if (seeds == 2 || seeds == 3) {
                layoutScore += 0.6; // Favor positions that are ripe for capturing
            }
        }

        // Consider opponent's potential captures and try to minimize them
        for (int seeds : opponentHoles) {
            if (seeds == 2 || seeds == 3) {
                layoutScore -= 0.4; // Penalize positions that allow the opponent easy captures
            }
        }
        return layoutScore;
    }

    private long generateGameStateHash(Board board, boolean isMaximizingPlayer, int depth) {
        long hash = 0;
        int[] playerHoles = board.getPlayerHoles();
        int[] opponentHoles = board.getOpponentHoles();
        int prime = 31;

        // Combine player and opponent holes into the hash
        for (int seeds : playerHoles) {
            hash = hash * prime + seeds;
        }
        for (int seeds : opponentHoles) {
            hash = hash * prime + seeds;
        }

        // Incorporate scores and current player into the hash
        hash = hash * prime + board.getScore(0);
        hash = hash * prime + board.getScore(1);
        hash = hash * prime + (isMaximizingPlayer ? 1 : 2);

        // Consider depth if evaluations at different depths should be cached separately
        hash = hash * prime + depth;

        return hash;
    }

}