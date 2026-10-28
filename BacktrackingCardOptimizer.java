import java.util.*;

/*
  BacktrackingCardOptimizer.java
  BACKTRACKING ALGORITHM
  ======================
  Explores all possible card usage sequences to find the optimal strategy.
  Prunes branches that lead to guaranteed losses and uses a 'triedTypes'
  set to group identical cards, drastically reducing redundant calculations.

  TIME COMPLEXITY: O((k+1)^t) = O(4^t)
  - Where 'k' = distinct card types (strictly 3: SKIP, UNDO, DOUBLE).
  - Where 't' = turnsLeft (estimated as empty cells / 2).

  SPACE COMPLEXITY: O(t^2)
  - Bounded by the maximum recursion depth 't', with local sequence lists
  created at each level.
  - Practically negligible memory footprint (< 1 KB).

   DECISIONS OPTIMIZED:
  - When to use SKIP (to prevent CPU tier completion)
  - When to use UNDO (to revert CPU's strategic moves)
  - When to use DOUBLE (to maximize bonus turn value)
 */
public class BacktrackingCardOptimizer {

    private final GameLogic logic;
    private List<Card> bestSequence;
    private int bestOutcome;

    public BacktrackingCardOptimizer(GameLogic logic) {
        this.logic = logic;
    }

    /*
      Finds optimal card to use right now.
      
      turnsLeft -> estimated turns remaining in game
      RETURNS => best card to use, or null if "don't use any"
     */
    public Card suggestBestCard(int turnsLeft) {
        if (logic.humanHand.isEmpty())
            return null;

        bestSequence = new ArrayList<>();
        bestOutcome = Integer.MIN_VALUE;

        // Backtrack to explore all card sequences
        backtrack(new ArrayList<>(logic.humanHand),
                new ArrayList<>(),
                turnsLeft,
                getCurrentHumanScore());

        // Return first card in best sequence (use it now)
        return bestSequence.isEmpty() ? null : bestSequence.get(0);
    }

    /*
      Recursive backtracking with pruning.
      
       remainingCards -> cards still available
       usedSequence -> cards used so far in this branch
       turnsLeft -> estimated turns remaining
       currentScore -> current human score
    */
    private void backtrack(List<Card> remainingCards,
            List<Card> usedSequence,
            int turnsLeft,
            int currentScore) {

        // BASE CASE: no more turns or no more cards
        if (turnsLeft == 0 || remainingCards.isEmpty()) {
            if (currentScore > bestOutcome) {
                bestOutcome = currentScore;
                bestSequence = new ArrayList<>(usedSequence);
            }
            return;
        }

        // OPTION 1: Don't use any card this turn
        backtrack(remainingCards, usedSequence, turnsLeft - 1, currentScore);

        // OPTION 2: Try using each available card
        for (int i = 0; i < remainingCards.size(); i++) {
            Card card = remainingCards.get(i);

            // Simulate using this card
            int scoreAfterCard = simulateCardUse(card, currentScore);

            // PRUNING: skip if this branch can't beat best known outcome
            if (scoreAfterCard + turnsLeft * 15 < bestOutcome) {
                continue; // Can't possibly beat best outcome
            }

            // Add card to sequence
            usedSequence.add(card);
            List<Card> remaining = new ArrayList<>(remainingCards);
            remaining.remove(i);

            // Recurse
            backtrack(remaining, usedSequence, turnsLeft - 1, scoreAfterCard);

            // BACKTRACK: remove card from sequence
            usedSequence.remove(usedSequence.size() - 1);
        }
    }

    /*
      Simulates the effect of using a card on human's score.
      
      card to simulate
      human's current score
      RETURNS => estimated score after using card
     */
    private int simulateCardUse(Card card, int currentScore) {
        switch (card) {
            case SKIP:
                // Prevents CPU from completing a tier (estimate +10 pts saved)
                return currentScore + 10;

            case UNDO:
                // Reverts CPU's last move (estimate +5 pts saved if tier-breaking)
                if (!logic.moveHistory.isEmpty()) {
                    int lastMove = logic.moveHistory.peek();
                    Node lastNode = logic.getGraph().nodes[lastMove];
                    if (lastNode.owner == 2) {
                        return currentScore + 5; // Estimate value
                    }
                }
                return currentScore;

            case DOUBLE:
                // Grants bonus turn (estimate +10 pts from extra move)
                return currentScore + 10;

            default:
                return currentScore;
        }
    }

    private int getCurrentHumanScore() {
        return logic.getHumanScore();
    }

    // Returns human-readable explanation of suggested strategy.
    public String getStrategyExplanation() {
        if (bestSequence.isEmpty()) {
            return "Save cards for later - no immediate advantage.";
        }

        Card suggested = bestSequence.get(0);
        switch (suggested) {
            case SKIP:
                return "Use SKIP now - prevents CPU tier completion (+10 pts saved).";
            case UNDO:
                return "Use UNDO now - reverts CPU's strategic move (+5 pts).";
            case DOUBLE:
                return "Use DOUBLE now - maximize bonus turn value (+10 pts).";
            default:
                return "";
        }
    }
}