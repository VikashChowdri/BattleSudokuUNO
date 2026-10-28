/*
  GameLogic.java
  Owns all mutable game state and enforces the rules of Territory War.
 
  Responsibilities :
   1. Track whose turn it is (human / CPU)
   2. Validate and apply human moves
   3. Use cards (SKIP, UNDO, DOUBLE)
   4. Trigger the CPU turn (Merge-Sort-based move selection)
   5. Evaluate CPU heuristic scores (tier-based D&C analysis)
   6. Maintain penalty counters
   7. Delegate territory scoring to DivideConquerScoring
   8. Notify the UI observer when state changes
 
 The class is UI-agnostic: all output goes through the GameObserver interface.
*/

import java.util.*;
import javax.swing.Timer;

public class GameLogic {

    // OBSERVER (implemented by BattleSudokuUI)

    public interface GameObserver {
        void onStateChanged();

        void onLog(String message);

        void onGameOver(int humanFinal, int cpuFinal,
                int humanTerr, int humanPenalty,
                int cpuTerr, int cpuPenalty);
    }

    // FIELDS
    private final Graph graph = new Graph();
    private final DivideConquerScoring scorer;
    private final Random rand = new Random();
    private GameObserver observer;

    // Turn & card state
    public boolean humanTurn = true;
    public boolean doubleMoveActive = false;
    public boolean skipCpuNext = false;
    public boolean cardUsedThisTurn = false;
    public final List<Card> humanHand = new ArrayList<>();
    public final Deque<Integer> moveHistory = new ArrayDeque<>();

    // Scores
    public int humanTerritoryPoints = 0;
    public int cpuTerritoryPoints = 0;
    public int humanPenalty = 0;
    public int cpuPenalty = 0;

    // Currently selected board cell (-1 = none)
    public int selectedIndex = -1;

    // Which puzzle is active
    private int currentPuzzleIndex = 0;

    // INIT

    public GameLogic() {
        this.scorer = new DivideConquerScoring(graph);
        this.dpCPU = new DynamicProgrammingCPU(graph);
        this.cardOptimizer = new BacktrackingCardOptimizer(this); 
    }

    public void setObserver(GameObserver obs) {
        this.observer = obs;
    }

    // Re-initialises the game using the current puzzle index.

    public void initGame() {
        int idx = currentPuzzleIndex % PuzzleBank.size();
        graph.init(PuzzleBank.PUZZLE_BANK[idx], PuzzleBank.SOLUTION_BANK[idx]);

        humanHand.clear();
        moveHistory.clear();
        humanTurn = true;
        doubleMoveActive = false;
        skipCpuNext = false;
        cardUsedThisTurn = false;
        humanTerritoryPoints = 0;
        cpuTerritoryPoints = 0;
        humanPenalty = 0;
        cpuPenalty = 0;
        selectedIndex = -1;

        log("=== TERRITORY WAR STARTED ===");
        log("Puzzle #" + (currentPuzzleIndex + 1));
        log("SCORING TIERS:");
        log("  3-4 cells = 5pts | 5-6 cells = 10pts");
        log("  7-9 cells = 15pts");
        log("Wrong Move: -1pt penalty");


        this.dpCPU = new DynamicProgrammingCPU(graph);  // Refresh for new puzzle
        notifyChange();
    }

    public void nextPuzzle() {
        currentPuzzleIndex = (currentPuzzleIndex + 1) % PuzzleBank.size();
        initGame();
    }

    public Graph getGraph() {
        return graph;
    }

    // HUMAN MOVE

    public void handleHumanMove(int val) {
        if (!humanTurn || selectedIndex == -1)
            return;

        Node n = graph.nodes[selectedIndex];

        if (n.value != 0) {
            log("Cell occupied!");
            return;
        }

        if (val == n.solutionVal) {
            // ── CORRECT ──
            n.value = val;
            n.owner = 1;
            moveHistory.push(n.id);
            log("Correct! Node " + n.id + " captured.");

            boolean isDoubleBonus = doubleMoveActive;
            if (!isDoubleBonus) {
                Card c = Card.values()[rand.nextInt(Card.values().length)];
                humanHand.add(c);
                log("LOOT: " + c.name() + " card!");
            }

            recalculatePoints();
            cardUsedThisTurn = false;
            notifyChange();
            checkWin();

            if (doubleMoveActive) {
                doubleMoveActive = false;
                log("DOUBLE: Go again! (No card for bonus turn)");
                return; // human keeps the turn
            }

        } else {
            // WRONG
            humanPenalty += 1;
            int totalScore = humanTerritoryPoints - humanPenalty;
            log("✗ Wrong! -1 penalty. Total penalties: -" + humanPenalty
                    + " | Score: " + totalScore);
            cardUsedThisTurn = false;
            notifyChange();
        }

        // Pass turn to CPU
        humanTurn = false;
        notifyChange();

        if (skipCpuNext) {
            skipCpuNext = false;
            humanTurn = true;
            log("SKIP: CPU turn skipped!");
            notifyChange();
        } else {
            Timer t = new Timer(1000, e -> cpuTurn());
            t.setRepeats(false);
            t.start();
        }
    }

    // CARD USE

    public void useCard(Card type) {
        if (!humanHand.contains(type)) {
            log("You don't have this card!");
            return;
        }
        if (cardUsedThisTurn) {
            log("Only 1 card per turn!");
            return;
        }
        if (!humanTurn) {
            log("Can only use cards on your turn!");
            return;
        }

        switch (type) {
            case SKIP -> {
                skipCpuNext = true;
                cardUsedThisTurn = true;
                humanHand.remove(type);
                log("SKIP activated. CPU will skip next turn.");
            }
            case DOUBLE -> {
                doubleMoveActive = true;
                cardUsedThisTurn = true;
                humanHand.remove(type);
                log("DOUBLE activated. Next correct move = 2 turns!");
            }
            case UNDO -> {
                if (moveHistory.isEmpty()) {
                    log("UNDO Failed: No moves to undo.");
                    return;
                }
                int lastId = moveHistory.peek();
                Node node = graph.nodes[lastId];
                if (node.owner == 2) {
                    node.value = 0;
                    node.owner = 0;
                    moveHistory.pop();
                    recalculatePoints();
                    cardUsedThisTurn = true;
                    humanHand.remove(type);
                    log("UNDO: CPU move reverted!");
                } else {
                    log("UNDO Failed: Can only undo CPU moves.");
                }
            }
        }
        notifyChange();
    }

    // CPU TURN (Merge-Sort move selection)
    private void cpuTurn() {
        if (isFull()) { checkWin(); return; }
        
        Node best;
        String algorithm;
        
        if (useDPforCPU) {
            // USE DYNAMIC PROGRAMMING (Review 3)
            best = dpCPU.findBestMove();
            algorithm = "Dynamic Programming (depth=3, memo=" + dpCPU.getMemoHits() + ")";
        } else {
            // FALLBACK: Merge Sort (Review 2)
            List<MergeSortCPU.CPUMove> moves = new ArrayList<>();
            for (Node n : graph.nodes) {
                if (n.value == 0) {
                    moves.add(new MergeSortCPU.CPUMove(n, evaluateMoveScore(n)));
                }
            }
            MergeSortCPU.sort(moves);
            best = moves.get(moves.size() - 1).node;
            algorithm = "Merge Sort O(N log N)";
        }
        
        if (best == null) { log("CPU: No valid moves"); return; }
        
        best.value = best.solutionVal;
        best.owner = 2;
        moveHistory.push(best.id);
        
        log("CPU took Node " + best.id + " | Algorithm: " + algorithm);
        
        recalculatePoints();
        humanTurn = true;
        notifyChange();
        checkWin();
    }

    /*
     * Heuristic score for a CPU candidate cell.
     * Analyses every house the cell belongs to (via D&C house list),
     * rewarding moves that complete tiers and blocking opponent tiers.
     */
    private int evaluateMoveScore(Node n) {
        int score = 0;

        for (int[] house : HouseUtils.getAffectedHouses(n)) {
            int cpuCount = 0, humanCount = 0;
            for (int nodeId : house) {
                Node node = graph.nodes[nodeId];
                if (node.owner == 2)
                    cpuCount++;
                if (node.owner == 1)
                    humanCount++;
            }

            // Offensive: complete a tier
            if (cpuCount == 2)
                score += 60; // Tier 1 (5 pts)
            if (cpuCount == 4)
                score += 120; // Tier 2 (10 pts)
            if (cpuCount == 6)
                score += 180; // Tier 3 (15 pts)

            // Defensive: block opponent tier
            if (humanCount == 2)
                score += 50;
            if (humanCount == 4)
                score += 100;
            if (humanCount == 6)
                score += 150;
        }

        // Neighbour clustering
        for (Node nb : n.neighbors) {
            if (nb.owner == 2)
                score += 15;
            if (nb.owner == 1)
                score += 10;
        }

        // Diagonal bonus (high-value cell)
        if (n.r == n.c || n.r + n.c == 8)
            score += 8;

        // Centre control
        if (n.r >= 3 && n.r <= 5 && n.c >= 3 && n.c <= 5)
            score += 5;

        return score;
    }

    // SCORING

    private void recalculatePoints() {
        List<int[]> houses = HouseUtils.getAllHouses();

        if (houses.size() != 20) {
            log("WARNING: Expected 20 houses, got " + houses.size());
        }

        int[] result = scorer.calculateAllPoints(houses);
        humanTerritoryPoints = result[0];
        cpuTerritoryPoints = result[1];

        logScoringBreakdown(houses);
    }

    private void logScoringBreakdown(List<int[]> houses) {
        int humanHouses = 0, cpuHouses = 0;
        int humanTotal = 0, cpuTotal = 0;
        StringBuilder details = new StringBuilder();

        for (int i = 0; i < houses.size(); i++) {
            int[] house = houses.get(i);
            int hc = 0, cc = 0;
            for (int nodeId : house) {
                Node n = graph.nodes[nodeId];
                if (n.owner == 1)
                    hc++;
                if (n.owner == 2)
                    cc++;
            }

            int hPts = DivideConquerScoring.tierPoints(hc);
            int cPts = DivideConquerScoring.tierPoints(cc);

            if (hPts > 0) {
                details.append("\n  ").append(HouseUtils.getHouseName(i))
                        .append(": ").append(hc).append(" cells = ").append(hPts).append("pts");
                humanTotal += hPts;
                humanHouses++;
            }
            if (cPts > 0) {
                cpuTotal += cPts;
                cpuHouses++;
            }
        }

        if (humanHouses > 0 || cpuHouses > 0) {
            log("Houses - You: " + humanHouses + " (" + humanTotal + "pts) | CPU: "
                    + cpuHouses + " (" + cpuTotal + "pts)");
            if (details.length() > 0)
                log("YOUR HOUSES:" + details);
        }
    }

    // UTILITIES

    public boolean isFull() {
        for (Node n : graph.nodes)
            if (n.value == 0)
                return false;
        return true;
    }

    private void checkWin() {
        if (!isFull())
            return;
        int hFinal = humanTerritoryPoints - humanPenalty;
        int cFinal = cpuTerritoryPoints - cpuPenalty;
        if (observer != null) {
            observer.onGameOver(hFinal, cFinal,
                    humanTerritoryPoints, humanPenalty,
                    cpuTerritoryPoints, cpuPenalty);
        }
    }

    private void log(String s) {
        if (observer != null)
            observer.onLog(s);
    }

    private void notifyChange() {
        if (observer != null)
            observer.onStateChanged();
    }

    // Expose read-only scores for the UI
    public int getHumanScore() {
        return humanTerritoryPoints - humanPenalty;
    }

    public int getCpuScore() {
        return cpuTerritoryPoints - cpuPenalty;
    }





    private boolean useDPforCPU = true;  // Toggle between DP and MergeSort
    private DynamicProgrammingCPU dpCPU;
    private BacktrackingCardOptimizer cardOptimizer;
    


     // NEW: Suggest optimal card to human
    public String suggestCardStrategy() {
        int turnsLeft = 0;
        for (Node n : graph.nodes) if (n.value == 0) turnsLeft++;
        turnsLeft /= 2;  // Estimate turns (each player alternates)
        
        Card suggested = cardOptimizer.suggestBestCard(turnsLeft);
        if (suggested == null) {
            return "💡 Strategy: Save cards for later.";
        } else {
            return "💡 Suggestion: " + cardOptimizer.getStrategyExplanation();
        }
    }
    
    // NEW: Toggle between DP and MergeSort (for testing)
    public void setUseDPforCPU(boolean use) {
        this.useDPforCPU = use;
    }

}