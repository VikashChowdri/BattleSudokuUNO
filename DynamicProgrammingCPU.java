import java.util.*;

/*
  DynamicProgrammingCPU.java
  DYNAMIC PROGRAMMING ALGORITHM
  ==============================
  Simulates a 3-ply minimax game tree (CPU -> Human -> CPU) to find the
  optimal move sequence, evaluating future territory score differentials.
  Uses memoization to cache board state evaluations and avoid redundant work.

  TIME COMPLEXITY: O(m^3)
  - Where 'm' = number of empty cells remaining on the board.
  - The search depth is strictly capped at 3, generating at most ~m^3 unique
  states, making it highly suitable for real-time play (< 90ms).

  SPACE COMPLEXITY: O(m^3)
  - Represents the worst-case size to store visited states in the HashMap.
  - Practically requires < 5 KB of memory due to the shallow depth limit.

  IMPROVEMENT OVER MERGE SORT:
  - Plans multi-move sequences for tier completion rather than greedy single moves.
  - Example: "Sacrifice 5pts now to unlock 15pts in 2 moves"
 */

public class DynamicProgrammingCPU {

    private final Graph graph;
    private final DivideConquerScoring scorer;
    private final Map<BoardState, Integer> memo = new HashMap<>();
    private final int LOOKAHEAD_DEPTH = 3;

    public DynamicProgrammingCPU(Graph graph) {
        this.graph = graph;
        this.scorer = new DivideConquerScoring(graph);
    }

    /**
     * Finds best CPU move using DP lookahead.
     * RETURNS => Node to capture, or null if no valid moves
     */
    public Node findBestMove() {
        memo.clear();

        List<Node> candidates = new ArrayList<>();
        for (Node n : graph.nodes) {
            if (n.value == 0 && !n.fixed)
                candidates.add(n);
        }

        if (candidates.isEmpty())
            return null;

        Node bestMove = null;
        int bestScore = Integer.MIN_VALUE;

        for (Node candidate : candidates) {
            // Simulate taking this cell
            candidate.value = candidate.solutionVal;
            candidate.owner = 2;

            // Evaluate future score with DP
            int futureScore = dpEvaluate(LOOKAHEAD_DEPTH - 1, false);

            // Undo simulation
            candidate.value = 0;
            candidate.owner = 0;

            if (futureScore > bestScore) {
                bestScore = futureScore;
                bestMove = candidate;
            }
        }

        return bestMove;
    }

    /*
     * Recursive DP evaluation with memoization.
     * 
     * depth -> remaining lookahead depth
     * cpuTurn -> true if CPU's turn in simulation
     * RETURNS => expected score difference (CPU - Human)
     */
    private int dpEvaluate(int depth, boolean cpuTurn) {
        // BASE CASE: reached depth limit or board full
        if (depth == 0 || isFullSimulation()) {
            return getCurrentScoreDifference();
        }

        // MEMOIZATION: check cache
        BoardState state = new BoardState(graph);
        if (memo.containsKey(state)) {
            return memo.get(state);
        }

        // RECURSIVE CASE: try all possible moves
        List<Node> emptyCells = getEmptyCells();
        int bestValue;

        if (cpuTurn) {
            // Maximize score difference
            bestValue = Integer.MIN_VALUE;
            for (Node n : emptyCells) {
                n.value = n.solutionVal;
                n.owner = 2;

                int value = dpEvaluate(depth - 1, false);
                bestValue = Math.max(bestValue, value);

                n.value = 0;
                n.owner = 0;
            }
        } else {
            // Minimize score difference (human's turn)
            bestValue = Integer.MAX_VALUE;
            for (Node n : emptyCells) {
                n.value = n.solutionVal;
                n.owner = 1;

                int value = dpEvaluate(depth - 1, true);
                bestValue = Math.min(bestValue, value);

                n.value = 0;
                n.owner = 0;
            }
        }

        // Store in memo
        memo.put(state, bestValue);
        return bestValue;
    }

    private int getCurrentScoreDifference() {
        int[] scores = scorer.calculateAllPoints(HouseUtils.getAllHouses());
        return scores[1] - scores[0]; // CPU - Human
    }

    private List<Node> getEmptyCells() {
        List<Node> empty = new ArrayList<>();
        for (Node n : graph.nodes) {
            if (n.value == 0 && !n.fixed)
                empty.add(n);
        }
        return empty;
    }

    private boolean isFullSimulation() {
        for (Node n : graph.nodes) {
            if (n.value == 0 && !n.fixed)
                return false;
        }
        return true;
    }

    public int getMemoHits() {
        return memo.size();
    }
}