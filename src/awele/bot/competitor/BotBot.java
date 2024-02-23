package awele.bot.competitor;

import awele.bot.CompetitorBot;
import awele.core.Board;
import awele.core.InvalidBotException;

import java.util.HashMap;


/**
 * @author Aaro Karhu
 * Email: aaro.karhu19@gmail.com
 * this is my school project from the course M1 Informatique - Intelligence artificielle
 * in University of Lorraine in France 2024. to goal was tho develop a bot that can play Awele
 * a board game that is from Africa. This is my implementation of the given problem.
 */
public class BotBot extends CompetitorBot {
    private static final int MAX_DEPTH = 10; // max depth of search in the minmax
    private int BASE_DEPTH = 3; // variable used for dynamic depth
    private HashMap<String, Double> stateCache = new HashMap<>(); // already visited game states are stored here for optimization

    /**
     * construct the bot give it a name and add the authors name
     * @throws InvalidBotException
     */
    public BotBot() throws InvalidBotException {
        addAuthor("Aaro Karhu");
        setBotName("KalaKala");
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

    /**
     * Calling this method initializes the minmax algorithm
     * @param board État du plateau de jeu
     * @return double[] evaluated scores of the minmax
     */
    @Override
    public double[] getDecision(Board board) {
        double[] scores = new double[Board.NB_HOLES];
        int dynamicDepth = adjustDepth(board, BASE_DEPTH); // Adjust depth dynamically based on current board state

        for (int i = 0; i < Board.NB_HOLES; i++) {
            if (board.getPlayerHoles()[i] > 0) { // Validate move
                Board copy = (Board) board.clone(); // clone the board for next move
                try {
                    double[] decision = new double[Board.NB_HOLES];
                    decision[i] = 1; // Mark the move
                    copy = copy.playMoveSimulationBoard(copy.getCurrentPlayer(), decision); // make move on the cloned board
                    scores[i] = minMax(copy, dynamicDepth, Double.NEGATIVE_INFINITY,
                            Double.POSITIVE_INFINITY, false); // call the minmax to evaluate the scores
                } catch (InvalidBotException e) {
                    e.printStackTrace();
                }
            } else {
                scores[i] = Double.NEGATIVE_INFINITY; // mark undesired moves as small of number as possible
            }
        }
        return scores;
    }

    /**
     * minMax the main algorithm of the bot.
     * uses depth of search to evaluate game moves.
     * heavily depends upon the depth of search to
     * calculate the best moves.
     * uses alpha beta pruning to optimize the search.
     *
     * @param board current board
     * @param depth depth of search
     * @param alpha used for alpha beta pruning
     * @param beta used for alpha beta pruning
     * @param isMaximizingPlayer max if maximizing else minimize
     * @return double, the score of the evaluation. low if not maximizing player
     * @throws InvalidBotException
     */
    private double minMax(Board board, int depth, double alpha, double beta, boolean isMaximizingPlayer) throws InvalidBotException {
        String boardKey = generateBoardKey(board, isMaximizingPlayer);
        // Check cache
        if (stateCache.containsKey(boardKey)) {
            return stateCache.get(boardKey); // return value from cache
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
                    copy = copy.playMoveSimulationBoard (copy.getCurrentPlayer (), decision);
                    double eval = minMax(copy, depth - 1, alpha, beta, false);
                    maxEval = Math.max(maxEval, eval);

                    // alpha beta pruning
                    if(maxEval > beta) break;
                    alpha = Math.max(alpha, maxEval);
                }
            }
            stateCache.put(boardKey, maxEval);
            return maxEval;
        } else {
            double minEval = Double.POSITIVE_INFINITY;
            // Iterate over possible moves
            for (int i = 0; i < Board.NB_HOLES; i++) {
                if (board.getPlayerHoles()[i] > 0) {
                    Board copy = (Board) board.clone();
                    double[] decision = new double[Board.NB_HOLES];
                    decision[i] = 1; // Simulate the move
                    copy = copy.playMoveSimulationBoard (copy.getCurrentPlayer (), decision);
                    double eval = minMax(copy, depth - 1, alpha, beta, true);
                    minEval = Math.min(minEval, eval);
                    if(minEval < alpha) {
                        break;
                    }
                    beta = Math.min(beta, minEval);
                }
            }
            stateCache.put(boardKey, minEval);
            return minEval;
        }
    }

    public int adjustDepth(Board board, int baseDepth) {
        int adjustedDepth = baseDepth; // Start with a base depth

        // Example metric: Game phase based on total seeds on the board
        int totalSeeds = board.getNbSeeds();
        if (totalSeeds > 48) { // Early game
            adjustedDepth = Math.max(3, baseDepth - 1); // Shallow search in early game
        } else if (totalSeeds <= 48 && totalSeeds > 24) { // Mid game
            adjustedDepth = baseDepth; // Standard depth
        } else { // Late game
            adjustedDepth = Math.min(MAX_DEPTH, baseDepth + 1); // Deeper search in late game
        }

        // Example metric: Time constraints (pseudocode)
        // if (remainingTime < timeThreshold) {
        //     adjustedDepth = Math.max(3, baseDepth - 1); // Reduce depth under time pressure
        // }

        return adjustedDepth;
    }


    private String generateBoardKey(Board board, boolean isMaximizingPlayer) {
        // Generate a unique string representation of the board state
        // This should include the board's seed distribution and scores
        // For simplicity, here's a basic approach. Consider including more details as needed
        StringBuilder keyBuilder = new StringBuilder();
        for (int seeds : board.getPlayerHoles()) {
            keyBuilder.append(seeds).append(",");
        }
        keyBuilder.append("|");
        for (int seeds : board.getOpponentHoles()) {
            keyBuilder.append(seeds).append(",");
        }
        keyBuilder.append("|").append(board.getScore(0)).append(",").append(board.getScore(1));
        keyBuilder.append("|").append(isMaximizingPlayer); // Include the player's perspective in the key

        return keyBuilder.toString();
    }

    private boolean gameEndingCondition(Board board) {
        return board.getScore(0) >= 25 || board.getScore(1) >= 25 || board.getNbSeeds() <= 6;
    }

    private double evaluateBoard(Board board, boolean isMaximizingPlayer) {
        int currentPlayerIndex = isMaximizingPlayer ? board.getCurrentPlayer() : Board.otherPlayer(board.getCurrentPlayer());
        int opponentIndex = Board.otherPlayer(currentPlayerIndex);
        double scoreDifference = board.getScore(currentPlayerIndex) - board.getScore(opponentIndex);

        double boardLayoutScore = evaluateBoardLayout(board, currentPlayerIndex, isMaximizingPlayer);

        return scoreDifference + boardLayoutScore;
    }


    private double evaluateBoardLayout(Board board, int currentPlayerIndex, boolean isMaximizingPlayer) {
        double layoutScore = 0;
        int[] playerHoles = board.getPlayerHoles(); // Current player's holes
        int[] opponentHoles = board.getOpponentHoles(); // Opponent's holes

        // Adjust strategy based on the state of the game
        for (int seeds : playerHoles) {
            if (seeds == 2 || seeds == 3) {
                layoutScore += 0.5; // Favor positions that are ripe for capturing
            }
        }

        // Consider opponent's potential captures and try to minimize them
        for (int seeds : opponentHoles) {
            if (seeds == 2 || seeds == 3) {
                layoutScore -= 0.5; // Penalize positions that allow the opponent easy captures
            }
        }

        // Additional considerations for future moves and preventing opponent's captures
        // This can be further elaborated based on strategic insights

        return layoutScore;
    }
}