/*
  HouseUtils.java
  Utility class for building and naming the 20 "houses" used in
  Territory War scoring.

 A house is an ordered array of 9 cell indices that form a scoring unit:
   – 9 rows  (indices 0..8)
   – 9 cols  (indices 9..17)
   – 2 diags (indices 18..19)
 Node IDs follow the convention:  id = row * 9 + col
*/


import java.util.ArrayList;
import java.util.List;

public class HouseUtils {

    /*
     * Builds and returns all 20 houses in the order described above.
     * Diagonals: index 18 = top-left → bottom-right,
     * index 19 = top-right → bottom-left.
     */
    public static List<int[]> getAllHouses() {
        List<int[]> houses = new ArrayList<>();

        // 9 Rows
        for (int r = 0; r < 9; r++) {
            int[] house = new int[9];
            for (int c = 0; c < 9; c++) {
                house[c] = r * 9 + c;
            }
            houses.add(house);
        }

        // 9 Columns
        for (int c = 0; c < 9; c++) {
            int[] house = new int[9];
            for (int r = 0; r < 9; r++) {
                house[r] = r * 9 + c;
            }
            houses.add(house);
        }

        // Main diagonal (top-left → bottom-right)
        int[] diag1 = new int[9];
        int[] diag2 = new int[9];
        for (int i = 0; i < 9; i++) {
            diag1[i] = i * 9 + i;
            diag2[i] = i * 9 + (8 - i);
        }
        houses.add(diag1);
        houses.add(diag2);

        return houses;
    }

    /*
     * Returns all houses that contain the given node.
     * A node belongs to exactly 1 row, 1 column, and possibly 1–2 diagonals.
     */
    public static List<int[]> getAffectedHouses(Node n) {
        List<int[]> result = new ArrayList<>();
        List<int[]> allHouses = getAllHouses();
        for (int[] house : allHouses) {
            for (int nodeId : house) {
                if (nodeId == n.id) {
                    result.add(house);
                    break;
                }
            }
        }
        return result;
    }

    /*
     * Human-readable name for a house by its list index.
     * Used in the game log to identify scoring houses.
     */
    public static String getHouseName(int index) {
        if (index < 9)
            return "Row " + (index + 1);
        else if (index < 18)
            return "Col " + (index - 8);
        else if (index == 18)
            return "Diag \\";
        else
            return "Diag /";
    }
}