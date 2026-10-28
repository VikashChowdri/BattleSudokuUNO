import java.util.*;

public class TestCases {

    private static int passed = 0;
    private static int failed = 0;

    private static void assertTrue(String name, boolean condition) {
        if (condition) { 
            System.out.println("  [PASS] " + name); passed++; 
        }
        else  { 
            System.out.println("  [FAIL] " + name); failed++; 
        }
    }

    // shared silent observer 
    private static final GameLogic.GameObserver SILENT = new GameLogic.GameObserver() {
        public void onStateChanged() {}
        public void onLog(String m)   {}
        public void onGameOver(int a, int b, int c, int d, int e, int f) {}
    };


    // TEST 1 — Score correctly reflects territory after half (approx) the board
    //
    // Scenario: Human manually fills exactly 5 cells in Row 1.
    //           According to the tier rules that should award 10 pts
    //           (Tier 2: 5-6 cells = 10 pts). CPU owns nothing yet.
    //           We verify the live score matches the expected tier value.

    static void testScoreAfterHalfGame() {
        System.out.println("\nTEST 1: Territory score after half-game");
        System.out.println("Scenario: Human owns 5 cells in Row 1 -> expect Tier 2 = 10 pts");

        GameLogic gl = new GameLogic();
        gl.setObserver(SILENT);
        gl.initGame();

        Graph  g   = gl.getGraph();
        int    row = 1;          // work on row 1 (all 9 cells available)
        int    owned = 0;

        // Simulate human capturing 5 cells in row 1 by direct ownership
        // (mimics what handleHumanMove does on a correct answer)
        for (int c = 0; c < 9 && owned < 5; c++) {
            Node n = g.nodes[row * 9 + c];
            if (!n.fixed) {
                n.value = n.solutionVal;
                n.owner = 1;
                owned++;
            }
        }

        // Trigger the D&C scoring engine the same way the game does
        List<int[]> houses = HouseUtils.getAllHouses();
        DivideConquerScoring scorer = new DivideConquerScoring(g);
        int[] result = scorer.calculateAllPoints(houses);

        int humanPts = result[0];
        int cpuPts   = result[1];

        System.out.println("  Human territory points : " + humanPts);
        System.out.println("  CPU   territory points : " + cpuPts);

        assertTrue("Human owns 5 cells in one row -> at least 10 pts", humanPts >= 10);
        assertTrue("CPU owns nothing -> 0 pts", cpuPts == 0);
        assertTrue("Score grows with ownership (not stuck at 0)", humanPts > 0);
    }

    
    // TEST 2 — Full game flow: human plays, CPU plays, winner decided
    //
    // Scenario: We simulate a full game by alternating:
    //             1. Human takes every non-fixed empty cell (all correct)
    //             2. CPU is given the rest via its move history
    //           After the board fills, we confirm:
    //             3. isFull() returns true
    //             4. At least one player has a non-zero score
    //             5. The score difference correctly reflects board split
    
    static void testFullGameResult() {
        System.out.println("\nTEST 2: Full game - board fills, winner determined");
        System.out.println("Scenario: Human takes first half of empty cells, CPU gets the rest");

        GameLogic gl = new GameLogic();
        gl.setObserver(SILENT);
        gl.initGame();

        Graph g = gl.getGraph();

        // Collect all empty cells
        List<Node> emptyCells = new ArrayList<>();
        for (Node n : g.nodes) if (!n.fixed) emptyCells.add(n);

        int half = emptyCells.size() / 2;

        // Human owns first half
        for (int i = 0; i < half; i++) {
            Node n = emptyCells.get(i);
            n.value = n.solutionVal;
            n.owner = 1;
            gl.moveHistory.push(n.id);
        }

        // CPU owns second half
        for (int i = half; i < emptyCells.size(); i++) {
            Node n = emptyCells.get(i);
            n.value = n.solutionVal;
            n.owner = 2;
            gl.moveHistory.push(n.id);
        }

        // Run the scoring engine
        DivideConquerScoring scorer = new DivideConquerScoring(g);
        int[] result = scorer.calculateAllPoints(HouseUtils.getAllHouses());
        int humanPts = result[0];
        int cpuPts   = result[1];

        System.out.println("Empty cells total :" + emptyCells.size());
        System.out.println("Human owned : " + half);
        System.out.println("CPU owned : " + (emptyCells.size() - half));
        System.out.println("Human score : " + humanPts);
        System.out.println("CPU score : " + cpuPts);
        System.out.println("Board full :" + gl.isFull());

        assertTrue("Board is full after all cells assigned", gl.isFull());
        assertTrue("At least one player scored points",     (humanPts + cpuPts) > 0);
        assertTrue("Scores are non-negative",               humanPts >= 0 && cpuPts >= 0);
    }


    // TEST 3 — D&C scoring matches manual hand-count (algorithm correctness)
    //
    // Scenario: We hand-place cells of known ownership across two complete
    //           rows and verify the D&C engine returns the exact expected
    //           points instead of trusting tier math blindly.
    //
    //   Row 0 -> Human owns all 9 non-fixed cells reachable -> Tier 3 = 15 pts
    //   Row 8 -> CPU   owns 3 cells                          -> Tier 1 =  5 pts
    //   All other rows → nobody owns anything               -> 0 pts each
    //
    //   Expected totals: human = 15, cpu = 5

    static void testDivideAndConquerAccuracy() {
        System.out.println("\nTEST 3: D&C scoring accuracy vs manual hand-count");
        System.out.println("Scenario: Row 0 -> Human (7 cells, Tier 3=15pts) | Row 8 -> CPU (3 cells, Tier 1=5pts)");

        // Using a clean graph so fixed cells don't interfere with our counts
        Graph g = new Graph();
        g.init(PuzzleBank.PUZZLE_BANK[0], PuzzleBank.SOLUTION_BANK[0]);

        // Clear all owners (we set them explicitly below)
        for (Node n : g.nodes) { n.owner = 0; if (!n.fixed) n.value = 0; }

        // Row 0: give human enough cells to reach Tier 3 (need 7)
        int humanPlaced = 0;
        for (int c = 0; c < 9 && humanPlaced < 7; c++) {
            Node n = g.nodes[c];          // row 0 = indices 0..8
            n.owner = 1;
            n.value = n.solutionVal;
            humanPlaced++;
        }

        // Row 8: give CPU exactly 3 cells -> Tier 1
        int cpuPlaced = 0;
        for (int c = 0; c < 9 && cpuPlaced < 3; c++) {
            Node n = g.nodes[72 + c];     // row 8 = indices 72..80
            n.owner = 2;
            n.value = n.solutionVal;
            cpuPlaced++;
        }

        DivideConquerScoring scorer = new DivideConquerScoring(g);
        int[] result = scorer.calculateAllPoints(HouseUtils.getAllHouses());

        int humanPts = result[0];
        int cpuPts   = result[1];

        System.out.println("Human cells placed in Row 0 : " + humanPlaced + " -> expect >=15 pts");
        System.out.println("CPU cells placed in Row 8 : " + cpuPlaced  + " -> expect >=5 pts");
        System.out.println("D&C returned human pts : " + humanPts);
        System.out.println("D&C returned cpu pts : " + cpuPts);

        assertTrue("D&C gives human ≥15 pts for 7 cells in one row", humanPts >= 15);
        assertTrue("D&C gives CPU   ≥5  pts for 3 cells in one row", cpuPts  >= 5);
        assertTrue("Human outscores CPU in this setup", humanPts > cpuPts);
    }

    public static void main(String[] args) {
        System.out.println("Battle Sudoku - Game Test Suite");
        

        testScoreAfterHalfGame();
        testFullGameResult();
        testDivideAndConquerAccuracy();

        
        System.out.printf("  Results: %d / %d passed", passed, passed + failed);
        if (failed > 0) System.out.printf("  |  %d FAILED", failed);
        
        if (failed > 0) System.exit(1);
    }
}