/*
DivideConquerScoring.java
Implements the Divide & Conquer algorithm for territory scoring.

ALGORITHM OVERVIEW: 
Input  : a list of 20 houses (rows, columns, diagonals).
  Output : int[2] → { totalHumanPoints, totalCpuPoints }
 
  DIVIDE  : split the house list in half at the midpoint.
  CONQUER : recursively score each half.
  MERGE   : sum the two halves element-wise.
  BASE    : a single house -> count owner cells -> look up tier points.
 
  SCORING TIERS (per house, per player)
  >= 7 cells owned  -> 15 points  (dominant control)
  >= 5 cells owned  -> 10 points  (majority control)
  >= 3 cells owned  ->  5 points  (partial control)
  < 3 cells owned   ->  0 points
  
  Time complexity : O(H) where H = 20 (constant for standard Sudoku).
*/

import java.util.List;

public class DivideConquerScoring {

    private final Graph graph;

    public DivideConquerScoring(Graph graph) {
        this.graph = graph;
    }

    // Entry point – score all 20 houses and return totals.
    public int[] calculateAllPoints(List<int[]> houses) {
        if (houses.isEmpty())
            return new int[] { 0, 0 };
        return calculatePointsDC(houses, 0, houses.size() - 1);
    }

    // Recursive Divide & Conquer scoring.
    private int[] calculatePointsDC(List<int[]> houses, int left, int right) {

        // BASE CASE
        if (left == right) {
            return scoreHouse(houses.get(left));
        }

        // DIVIDE
        int mid = (left + right) / 2;
        int[] leftResult = calculatePointsDC(houses, left, mid);
        int[] rightResult = calculatePointsDC(houses, mid + 1, right);

        // CONQUER (merge)
        return new int[] { leftResult[0] + rightResult[0],
                leftResult[1] + rightResult[1] };
    }

    // Scores a single house for both players.
    private int[] scoreHouse(int[] house) {
        int humanCount = 0, cpuCount = 0;

        for (int nodeId : house) {
            Node n = graph.nodes[nodeId];
            if (n.owner == 1)
                humanCount++;
            if (n.owner == 2)
                cpuCount++;
        }

        return new int[] { tierPoints(humanCount), tierPoints(cpuCount) };
    }

    // Maps a cell-count to its tier point value.
    public static int tierPoints(int count) {
        if (count >= 7)
            return 15; // Tier 3 – Dominant
        if (count >= 5)
            return 10; // Tier 2 – Majority
        if (count >= 3)
            return 5; // Tier 1 – Partial
        return 0;
    }
}