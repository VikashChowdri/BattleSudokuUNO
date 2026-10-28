/*
  Node.java
  Represents a single cell in the Sudoku board as a graph node.
  Each node holds its position (row/col), current value, solution value,
  ownership, and adjacency list for graph-based constraint checking.
*/


import java.util.ArrayList;
import java.util.List;

public class Node {
    public int id;
    public int r, c;
    public int value;
    public int solutionVal;
    public int owner; // 0 = unclaimed, 1 = Human, 2 = CPU
    public boolean fixed;
    public List<Node> neighbors;

    public Node(int id, int r, int c, int sol, boolean fix) {
        this.id = id;
        this.r = r;
        this.c = c;
        this.solutionVal = sol;
        this.fixed = fix;
        this.value = fix ? sol : 0;
        this.owner = 0;
        this.neighbors = new ArrayList<>();
    }
}