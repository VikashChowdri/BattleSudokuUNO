/*
 Card.java
 Enumeration of all special-action cards a player can collect and use.

 SKIP   – skips the CPU's next turn entirely.
 UNDO   – reverts the most recent CPU move.
 DOUBLE – grants an extra move after the next correct placement
          (no bonus card is awarded for the extra turn).

Each constant carries a display colour used by the UI.
*/


import java.awt.Color;


public enum Card {
    SKIP(new Color(255, 87, 34)),
    UNDO(new Color(255, 193, 7)),
    DOUBLE(new Color(156, 39, 176));

    public final Color color;

    Card(Color c) {
        this.color = c;
    }
}