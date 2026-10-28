import java.util.*;

/**
 * BoardState.java
 * Immutable representation of board for DP memoization.
 */
public class BoardState {
    private final long humanCells;
    private final long cpuCells;
    private final int hashCode;
    
    public BoardState(Graph graph) {
        long h = 0, c = 0;
        for (int i = 0; i < 81; i++) {
            if (graph.nodes[i].owner == 1) h |= (1L << i);
            if (graph.nodes[i].owner == 2) c |= (1L << i);
        }
        this.humanCells = h;
        this.cpuCells = c;
        this.hashCode = Objects.hash(humanCells, cpuCells);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BoardState)) return false;
        BoardState other = (BoardState) obj;
        return this.humanCells == other.humanCells && 
               this.cpuCells == other.cpuCells;
    }
    
    @Override
    public int hashCode() { return hashCode; }
}