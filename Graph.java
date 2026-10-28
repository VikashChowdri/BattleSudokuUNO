/*
 Graph.java
 Builds and stores the Sudoku constraint graph.
 81 nodes are connected by edges wherever two cells share a row,
 column, or 3x3 box (standard Sudoku constraints).

 Data structure: Adjacency list stored inside each Node.
 */


public class Graph {

    public Node[] nodes = new Node[81];

    
    //Initialise every node from the given puzzle/solution arrays, then wire up all constraint edges.
    public void init(int[][] puzzle, int[][] sol) {
        for (int i = 0; i < 81; i++) {
            int r   = i / 9;
            int c   = i % 9;
            boolean fix = (puzzle[r][c] != 0);
            nodes[i] = new Node(i, r, c, sol[r][c], fix);
        }

        // Build edges: O(81^2 / 2) ≈ O(1) for a fixed 9x9 board
        for (int i = 0; i < 81; i++) {
            for (int j = i + 1; j < 81; j++) {
                if (shareConstraint(nodes[i], nodes[j])) {
                    nodes[i].neighbors.add(nodes[j]);
                    nodes[j].neighbors.add(nodes[i]);
                }
            }
        }
    }

    
    //Returns true when two nodes share at least one Sudoku constraint (same row, same column, or same 3x3 box).
     
    private boolean shareConstraint(Node n1, Node n2) {
        boolean sameRow = n1.r == n2.r;
        boolean sameCol = n1.c == n2.c;
        boolean sameBox = (n1.r / 3 == n2.r / 3) && (n1.c / 3 == n2.c / 3);
        return sameRow || sameCol || sameBox;
    }

    // Resets all non-fixed cells for a fresh game on the same graph.
    public void reset(int[][] puzzle, int[][] sol) {
        init(puzzle, sol);
    }
}