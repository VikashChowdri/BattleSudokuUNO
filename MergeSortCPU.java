/*
MergeSortCPU.java
Encapsulates the Merge Sort algorithm used to rank candidate CPU moves.

ALGORITHM OVERVIEW:
The list of CPUMove objects is sorted ascending by score so that
the last element is always the highest-scoring (best) move.

 Time complexity  : O(N log N)
 Space complexity : O(N)   – auxiliary lists created during merge
 
 WHY ASCENDING ORDER?
 Picking the tail of an ascending list is O(1) and avoids reversing.
 */

import java.util.ArrayList;
import java.util.List;


public class MergeSortCPU {

    //Represents one possible CPU move together with its heuristic score.
     
    public static class CPUMove {
        public final Node node;
        public final int  score;

        public CPUMove(Node n, int s) {
            this.node  = n;
            this.score = s;
        }
    }

    
    // Sorts a list of CPUMove in ascending order of score (in-place)
    public static void sort(List<CPUMove> arr) {
        if (arr == null || arr.size() <= 1) return;
        mergeSort(arr, 0, arr.size() - 1);
    }

    // PRIVATE RECURSIVE CORE

    private static void mergeSort(List<CPUMove> arr, int left, int right) {
        if (left < right) {
            int mid = (left + right) / 2;
            mergeSort(arr, left,    mid);
            mergeSort(arr, mid + 1, right);
            merge(arr, left, mid, right);
        }
    }

    private static void merge(List<CPUMove> arr, int left, int mid, int right) {
        int n1 = mid - left + 1;
        int n2 = right - mid;

        // Copy sub-arrays into temporaries
        List<CPUMove> L = new ArrayList<>(arr.subList(left,    left + n1));
        List<CPUMove> R = new ArrayList<>(arr.subList(mid + 1, mid  + 1 + n2));

        int i = 0, j = 0, k = left;

        // Merge – keep ascending score order
        while (i < n1 && j < n2) {
            if (L.get(i).score <= R.get(j).score) {
                arr.set(k++, L.get(i++));
            } else {
                arr.set(k++, R.get(j++));
            }
        }

        // Drain leftovers
        while (i < n1) arr.set(k++, L.get(i++));
        while (j < n2) arr.set(k++, R.get(j++));
    }
}