/*
  PROJECT: Battle Sudoku - Territory War (Review 2)
  DESCRIPTION:
  A competitive, turn-based Sudoku game where the player competes against cpu
  for board control. Points are awarded for controlling "houses" (rows 9, cols 9, diagonals 2).
  ALGORITHMS USED:
  1. Divide & Conquer: Used for the scoring engine to recursively calculate total 
  territory points across 20 different board houses.
  2. Merge Sort: Used by the CPU to sort and rank potential moves based on a 
  strategic heuristic score in O(N log N) time.
  3. Graph Theory: The board is represented as a graph where nodes are cells and 
  edges represent Sudoku constraints (sharing a row, column, or 3x3 box).
 */

import java.awt.*;
import javax.swing.*;

public class BattleSudokuUI extends JFrame implements GameLogic.GameObserver {

    // THEME
    private static final Color COL_BG = new Color(40, 44, 52);
    private static final Color COL_ACCENT = new Color(60, 64, 72);
    private static final Color COL_HUMAN = new Color(79, 195, 247);
    private static final Color COL_CPU = new Color(229, 115, 115);
    private static final Color COL_FIXED = new Color(220, 220, 220);
    private static final Font FONT_CELL = new Font("Segoe UI", Font.BOLD, 22);

    // BACKEND
    private final GameLogic logic = new GameLogic();

    // BOARD WIDGETS
    private final JButton[] cellButtons = new JButton[81];

    // DASHBOARD WIDGETS
    private final JLabel lblHumanScore = new JLabel("0");
    private final JLabel lblCpuScore = new JLabel("0");
    private final JLabel lblTurn = new JLabel("YOUR TURN");
    private final JTextArea logArea = new JTextArea(6, 25);
    private final JPanel pnlCards = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
    private JButton btnSkip, btnUndo, btnDouble;

    public BattleSudokuUI() {
        super("Battle Sudoku: Territory War");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 750);
        setLayout(new BorderLayout());
        getContentPane().setBackground(COL_BG);

        logic.setObserver(this);
        logic.initGame();

        buildBoardPanel();
        buildDashboard();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    // GAMEOBSERVER CALLBACKS

    @Override
    public void onStateChanged() {
        SwingUtilities.invokeLater(this::refreshUI);
    }

    @Override
    public void onLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("> " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    @Override
    public void onGameOver(int humanFinal, int cpuFinal,
            int humanTerr, int humanPenalty,
            int cpuTerr, int cpuPenalty) {
        String result = humanFinal > cpuFinal ? "🎉 VICTORY!"
                : humanFinal < cpuFinal ? "💀 DEFEAT!"
                        : "🤝 DRAW!";
        String msg = result + "\nYou: " + humanFinal
                + " (" + humanTerr + " - " + humanPenalty + ")"
                + " | CPU: " + cpuFinal;
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, msg));
    }

    // UI CONSTRUCTION

    private void buildBoardPanel() {
        JPanel pnlLeft = new JPanel(new BorderLayout());
        pnlLeft.setBackground(COL_BG);
        pnlLeft.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 9×9 grid
        JPanel pnlBoard = new JPanel(new GridLayout(9, 9));
        pnlBoard.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

        for (int i = 0; i < 81; i++) {
            JButton b = new JButton();
            b.setFont(FONT_CELL);
            b.setFocusable(false);
            final int idx = i;
            b.addActionListener(e -> {
                logic.selectedIndex = idx;
                refreshUI();
            });
            cellButtons[i] = b;
            pnlBoard.add(b);
        }
        pnlLeft.add(pnlBoard, BorderLayout.CENTER);

        // Numpad 1–9
        JPanel pnlNums = new JPanel(new FlowLayout());
        pnlNums.setBackground(COL_BG);
        for (int i = 1; i <= 9; i++) {
            JButton b = new JButton(String.valueOf(i));
            b.setPreferredSize(new Dimension(50, 40));
            b.setBackground(new Color(100, 181, 246));
            final int val = i;
            b.addActionListener(e -> logic.handleHumanMove(val));
            pnlNums.add(b);
        }
        pnlLeft.add(pnlNums, BorderLayout.SOUTH);
        add(pnlLeft, BorderLayout.CENTER);
    }

    private void buildDashboard() {
        JPanel pnlRight = new JPanel();
        pnlRight.setLayout(new BoxLayout(pnlRight, BoxLayout.Y_AXIS));
        pnlRight.setBackground(COL_ACCENT);
        pnlRight.setPreferredSize(new Dimension(340, 0));
        pnlRight.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Title
        JLabel title = new JLabel("TERRITORY WAR");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlRight.add(title);

        pnlRight.add(Box.createVerticalStrut(20));

        // Restart
        JButton btnRestart = styledButton("RESTART GAME", new Color(211, 47, 47));
        btnRestart.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRestart.addActionListener(e -> {
            logic.initGame();
            logArea.setText("");
        });
        pnlRight.add(btnRestart);
        pnlRight.add(Box.createVerticalStrut(10));

        // New Game
        JButton btnNext = styledButton("NEW GAME", new Color(46, 125, 50));
        btnNext.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnNext.addActionListener(e -> {
            logic.nextPuzzle();
            logArea.setText("");
        });
        pnlRight.add(btnNext);
        pnlRight.add(Box.createVerticalStrut(20));

        // Turn label
        lblTurn.setForeground(COL_HUMAN);
        lblTurn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTurn.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlRight.add(lblTurn);

        pnlRight.add(Box.createVerticalStrut(20));

        // Score boxes
        JPanel scores = new JPanel(new GridLayout(1, 2, 10, 0));
        scores.setOpaque(false);
        scores.add(scoreBox("YOU", lblHumanScore, COL_HUMAN));
        scores.add(scoreBox("CPU", lblCpuScore, COL_CPU));
        pnlRight.add(scores);

        // Card inventory
        pnlRight.add(Box.createVerticalStrut(20));
        JLabel lblInv = new JLabel("Inventory:");
        lblInv.setForeground(Color.LIGHT_GRAY);
        pnlRight.add(lblInv);
        pnlCards.setBackground(COL_ACCENT);
        pnlCards.setPreferredSize(new Dimension(300, 60));
        pnlRight.add(pnlCards);

        // Card action buttons
        JPanel pnlActions = new JPanel(new GridLayout(1, 3, 5, 5));
        pnlActions.setOpaque(false);
        btnSkip = styledButton("SKIP", Card.SKIP.color);
        btnUndo = styledButton("UNDO", Card.UNDO.color);
        btnDouble = styledButton("DOUBLE", Card.DOUBLE.color);
        btnUndo.setForeground(Color.BLACK);

        btnSkip.addActionListener(e -> logic.useCard(Card.SKIP));
        btnUndo.addActionListener(e -> logic.useCard(Card.UNDO));
        btnDouble.addActionListener(e -> logic.useCard(Card.DOUBLE));

        pnlActions.add(btnSkip);
        pnlActions.add(btnUndo);
        pnlActions.add(btnDouble);
        pnlRight.add(pnlActions);

        pnlRight.add(Box.createVerticalStrut(20));




        pnlRight.add(Box.createVerticalStrut(10));

        // Strategy hint button (uses Backtracking)
        JButton btnHint = styledButton("💡 CARD STRATEGY", new Color(33, 150, 243));
        btnHint.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnHint.addActionListener(e -> {
            String hint = logic.suggestCardStrategy();
            JOptionPane.showMessageDialog(this, hint, "Backtracking Optimizer", 
                                        JOptionPane.INFORMATION_MESSAGE);
        });
        pnlRight.add(btnHint);

        // Log area
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(Color.GREEN);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logArea.setEditable(false);
        pnlRight.add(new JScrollPane(logArea));

        add(pnlRight, BorderLayout.EAST);
        setCardButtonsEnabled(false);
    }

    // REFRESH

    private void refreshUI() {
        Graph graph = logic.getGraph();

        for (int i = 0; i < 81; i++) {
            Node n = graph.nodes[i];
            JButton b = cellButtons[i];

            // Thick borders at 3×3 box boundaries
            int r = i / 9, col = i % 9;
            int top = (r % 3 == 0) ? 3 : 1;
            int lft = (col % 3 == 0) ? 3 : 1;
            int bot = (r % 3 == 2) ? 3 : 1;
            int rgt = (col % 3 == 2) ? 3 : 1;
            Color borderCol = (i == logic.selectedIndex) ? Color.ORANGE : Color.GRAY;
            b.setBorder(BorderFactory.createMatteBorder(top, lft, bot, rgt, borderCol));

            if (n.value != 0) {
                b.setText(String.valueOf(n.value));
                if (n.fixed) {
                    b.setBackground(COL_FIXED);
                    b.setForeground(Color.BLACK);
                } else if (n.owner == 1) {
                    b.setBackground(COL_HUMAN);
                    b.setForeground(Color.BLACK);
                } else {
                    b.setBackground(COL_CPU);
                    b.setForeground(Color.BLACK);
                }
            } else {
                b.setText("");
                b.setBackground(Color.WHITE);
            }
        }

        lblHumanScore.setText(String.valueOf(logic.getHumanScore()));
        lblCpuScore.setText(String.valueOf(logic.getCpuScore()));
        lblTurn.setText(logic.humanTurn ? "YOUR TURN" : "CPU THINKING...");

        // Card inventory
        pnlCards.removeAll();
        if (logic.humanHand.isEmpty()) {
            JLabel l = new JLabel("(No cards)");
            l.setForeground(Color.GRAY);
            pnlCards.add(l);
        } else {
            for (Card card : logic.humanHand) {
                JLabel lbl = new JLabel(card.name());
                lbl.setOpaque(true);
                lbl.setBackground(card.color);
                lbl.setForeground(card == Card.UNDO ? Color.BLACK : Color.WHITE);
                lbl.setFont(new Font("Arial", Font.BOLD, 10));
                lbl.setBorder(BorderFactory.createLineBorder(Color.WHITE));
                pnlCards.add(lbl);
            }
        }
        pnlCards.revalidate();
        pnlCards.repaint();

        setCardButtonsEnabled(logic.humanTurn && !logic.humanHand.isEmpty());
    }

    // HELPERS

    private void setCardButtonsEnabled(boolean en) {
        btnSkip.setEnabled(en);
        btnUndo.setEnabled(en);
        btnDouble.setEnabled(en);
    }

    private JButton styledButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        return b;
    }

    private JPanel scoreBox(String labelText, JLabel valueLabel, Color c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(c.darker());
        p.setBorder(BorderFactory.createLineBorder(c));
        JLabel title = new JLabel(labelText, SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 20));
        p.add(title, BorderLayout.NORTH);
        p.add(valueLabel, BorderLayout.CENTER);
        return p;
    }

    // ENTRY POINT
    public static void main(String[] args) {
        SwingUtilities.invokeLater(BattleSudokuUI::new);
    }
}