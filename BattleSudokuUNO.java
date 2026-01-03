import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer; 

public class BattleSudokuUNO extends JFrame {

    // COLORS & THEME
    private static final Color COL_BG = new Color(40, 44, 52);       
    private static final Color COL_ACCENT = new Color(60, 64, 72);   
    private static final Color COL_HUMAN = new Color(79, 195, 247);  
    private static final Color COL_CPU = new Color(229, 115, 115);   
    private static final Color COL_FIXED = new Color(220, 220, 220); 
    private static final Font FONT_CELL = new Font("Segoe UI", Font.BOLD, 22);


    // CARDS
    enum Card { 
        SKIP(new Color(255, 87, 34)), 
        UNDO(new Color(255, 193, 7)), 
        DOUBLE(new Color(156, 39, 176)); 

        final Color color;
        Card (Color c) { 
            this.color = c; 
        }
    }


    // GRAPH REPRESENTATION
    class Node {
        int id; 
        int r, c;
        int value;      
        int solutionVal; 
        int owner; // 1 -> Human , 2 -> CPU
        boolean fixed;
        List<Node> neighbors; 

        Node(int id, int r, int c, int sol, boolean fix) {
            this.id = id; this.r = r; this.c = c;
            this.solutionVal = sol; this.fixed = fix;
            this.value = fix ? sol : 0;
            this.owner = 0;
            this.neighbors = new ArrayList<>();
        }
    }

    class Graph {
        Node[] nodes = new Node[81];

        void init(int[][] puzzle, int[][] sol) {
            for(int i=0; i<81; i++) {
                int r = i/9, c = i%9;
                boolean fix = (puzzle[r][c] != 0);
                nodes[i] = new Node(i, r, c, sol[r][c], fix);
            }

            // Edges
            for(int i=0; i<81; i++) {
                for(int j=i+1; j<81; j++) {
                    if(shareConstraint(nodes[i], nodes[j])) {
                        nodes[i].neighbors.add(nodes[j]);
                        nodes[j].neighbors.add(nodes[i]);
                    }
                }
            }
        }

        // CHECH IF THEY ARE NEIGHBOURS
        private boolean shareConstraint(Node n1, Node n2) {
            boolean sameRow = n1.r == n2.r;
            boolean sameCol = n1.c == n2.c;
            boolean sameBox = (n1.r/3 == n2.r/3) && (n1.c/3 == n2.c/3);
            return sameRow || sameCol || sameBox;
        }
    }

    private final Graph gameGraph = new Graph();

    // PUZZLE DATA
    private int currentPuzzleIndex = 0;
    private static final int[][][] PUZZLE_BANK = {
        { {5,3,0,0,7,0,0,0,0}, {6,0,0,1,9,5,0,0,0}, {0,9,8,0,0,0,0,6,0},
          {8,0,0,0,6,0,0,0,3}, {4,0,0,8,0,3,0,0,1}, {7,0,0,0,2,0,0,0,6},
          {0,6,0,0,0,0,2,8,0}, {0,0,0,4,1,9,0,0,5}, {0,0,0,0,8,0,0,7,9} },
        { {0,0,0,2,6,0,7,0,1}, {6,8,0,0,7,0,0,9,0}, {1,9,0,0,0,4,5,0,0},
          {8,2,0,1,0,0,0,4,0}, {0,0,4,6,0,2,9,0,0}, {0,5,0,0,0,3,0,2,8},
          {0,0,9,3,0,0,0,7,4}, {0,4,0,0,5,0,0,3,6}, {7,0,3,0,1,8,0,0,0} },
        { {1,0,0,4,8,9,0,0,6}, {7,3,0,0,0,0,0,4,0}, {0,0,0,0,0,1,2,9,5},
          {0,0,7,1,2,0,6,0,0}, {5,0,0,7,0,3,0,0,8}, {0,0,6,0,9,5,7,0,0},
          {9,1,4,6,0,0,0,0,0}, {0,2,0,0,0,0,0,3,7}, {8,0,0,5,1,2,0,0,4} }
    };
    private static final int[][][] SOLUTION_BANK = {
        { {5,3,4,6,7,8,9,1,2}, {6,7,2,1,9,5,3,4,8}, {1,9,8,3,4,2,5,6,7},
          {8,5,9,7,6,1,4,2,3}, {4,2,6,8,5,3,7,9,1}, {7,1,3,9,2,4,8,5,6},
          {9,6,1,5,3,7,2,8,4}, {2,8,7,4,1,9,6,3,5}, {3,4,5,2,8,6,1,7,9} },
        { {4,3,5,2,6,9,7,8,1}, {6,8,2,5,7,1,4,9,3}, {1,9,7,8,3,4,5,6,2},
          {8,2,6,1,9,5,3,4,7}, {3,7,4,6,8,2,9,1,5}, {9,5,1,7,4,3,6,2,8},
          {5,1,9,3,2,6,8,7,4}, {2,4,8,9,5,7,1,3,6}, {7,6,3,4,1,8,2,5,9} },
        { {1,5,2,4,8,9,3,7,6}, {7,3,9,2,5,6,8,4,1}, {4,6,8,3,7,1,2,9,5},
          {3,8,7,1,2,4,6,5,9}, {5,9,1,7,6,3,4,2,8}, {2,4,6,8,9,5,7,1,3},
          {9,1,4,6,3,7,5,8,2}, {6,2,5,9,4,8,1,3,7}, {8,7,3,5,1,2,9,6,4} }
    };

    // GAME STATE
    private boolean humanTurn = true;
    private int selectedIndex = -1;
    private boolean doubleMoveActive = false;
    private boolean skipCpuNext = false;
    private final List<Card> humanHand = new ArrayList<>();
    private final Deque<Integer> moveHistory = new ArrayDeque<>(); 
    private final Random rand = new Random();

    // UI COMPONENTS
    private final JButton[] cellButtons = new JButton[81];
    private final JLabel lblHumanScore = new JLabel("0");
    private final JLabel lblCpuScore = new JLabel("0");
    private final JLabel lblTurn = new JLabel("PLAYER TURN");
    private final JTextArea logArea = new JTextArea(6, 25);
    private final JPanel pnlHumanCards = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
    private JButton btnSkip, btnUndo, btnDouble;


    public BattleSudokuUNO() {
        super("Battle Sudoku: Review 1");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 750); 
        setLayout(new BorderLayout());
        getContentPane().setBackground(COL_BG);

        initGameData();
        initUI();
        updateUI();
        
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initGameData() {
        int idx = currentPuzzleIndex % PUZZLE_BANK.length;
        gameGraph.init(PUZZLE_BANK[idx], SOLUTION_BANK[idx]);
        
        humanHand.clear();
        moveHistory.clear();
        humanTurn = true;
        doubleMoveActive = false;
        skipCpuNext = false;
        log("Game Started. Puzzle #" + (currentPuzzleIndex + 1));
    }



    private void initUI() {




        // LEFT: BOARD
        JPanel pnlLeft = new JPanel(new BorderLayout());
        pnlLeft.setBackground(COL_BG);
        pnlLeft.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        
        JPanel pnlBoard = new JPanel(new GridLayout(9, 9));
        pnlBoard.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        
        for(int i=0; i<81; i++) {
            JButton b = new JButton();
            b.setFont(FONT_CELL);
            b.setFocusable(false);
            final int idx = i;
            b.addActionListener(e -> {
                selectedIndex = idx;
                updateUI();
            });
            cellButtons[i] = b;
            pnlBoard.add(b);
        }
        pnlLeft.add(pnlBoard, BorderLayout.CENTER);






        // Numpad
        JPanel pnlNums = new JPanel(new FlowLayout());
        pnlNums.setBackground(COL_BG);
        for(int i=1; i<=9; i++) {
            JButton b = new JButton(String.valueOf(i));
            b.setPreferredSize(new Dimension(50, 40));
            b.setBackground(new Color(100, 181, 246));
            final int val = i;
            b.addActionListener(e -> handleHumanMove(val));
            pnlNums.add(b);
        }
        pnlLeft.add(pnlNums, BorderLayout.SOUTH);
        add(pnlLeft, BorderLayout.CENTER);






        // RIGHT: DASHBOARD
        JPanel pnlRight = new JPanel();
        pnlRight.setLayout(new BoxLayout(pnlRight, BoxLayout.Y_AXIS));
        pnlRight.setBackground(COL_ACCENT);
        pnlRight.setPreferredSize(new Dimension(340, 0));
        pnlRight.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        JLabel title = new JLabel("BATTLE SUDOKU");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlRight.add(title);
        


        // RESTART BUTTON
        pnlRight.add(Box.createVerticalStrut(20));
        JButton btnRestart = createStyledButton("RESTART GAME", new Color(211, 47, 47)); // Red
        btnRestart.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRestart.addActionListener(e -> {
            initGameData();
            updateUI();
        });
        pnlRight.add(btnRestart);
        pnlRight.add(Box.createVerticalStrut(10));
       




        // NEXT PUZZLE BUTTON
        JButton btnNext = createStyledButton("NEXT PUZZLE", new Color(46, 125, 50)); // Green
        btnNext.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnNext.addActionListener(e -> {
            currentPuzzleIndex = (currentPuzzleIndex + 1) % PUZZLE_BANK.length;
            initGameData();
            updateUI();
        });
        pnlRight.add(btnNext);
        pnlRight.add(Box.createVerticalStrut(20));


        lblTurn.setForeground(COL_HUMAN);
        lblTurn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTurn.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlRight.add(lblTurn);

        pnlRight.add(Box.createVerticalStrut(20));
        JPanel scores = new JPanel(new GridLayout(1, 2, 10, 0));
        scores.setOpaque(false);
        scores.add(createScoreBox("YOU", lblHumanScore, COL_HUMAN));
        scores.add(createScoreBox("CPU", lblCpuScore, COL_CPU));
        pnlRight.add(scores);




        
        // Cards Display
        pnlRight.add(Box.createVerticalStrut(20));
        JLabel lblInv = new JLabel("Inventory:");
        lblInv.setForeground(Color.LIGHT_GRAY);
        pnlRight.add(lblInv);
        pnlHumanCards.setBackground(COL_ACCENT);
        pnlHumanCards.setPreferredSize(new Dimension(300, 60));
        pnlRight.add(pnlHumanCards);




        // Card Actions
        JPanel pnlActions = new JPanel(new GridLayout(1, 3, 5, 5));
        pnlActions.setOpaque(false);
        btnSkip = createStyledButton("SKIP", Card.SKIP.color);
        btnUndo = createStyledButton("UNDO", Card.UNDO.color);
        btnDouble = createStyledButton("DOUBLE", Card.DOUBLE.color);
        
        btnSkip.addActionListener(e -> useCard(Card.SKIP));
        btnUndo.addActionListener(e -> useCard(Card.UNDO));
        btnDouble.addActionListener(e -> useCard(Card.DOUBLE));
        
        pnlActions.add(btnSkip);
        pnlActions.add(btnUndo);
        pnlActions.add(btnDouble);
        pnlRight.add(pnlActions);

        pnlRight.add(Box.createVerticalStrut(20));
        logArea.setBackground(new Color(30,30,30));
        logArea.setForeground(Color.GREEN);
        pnlRight.add(new JScrollPane(logArea));

        add(pnlRight, BorderLayout.EAST);
        setCardButtonsEnabled(false);
    }
    
    private JButton createStyledButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(text.equals("UNDO") ? Color.BLACK : Color.WHITE);
        b.setFocusPainted(false);
        return b;
    }

    private JPanel createScoreBox(String t, JLabel l, Color c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(c.darker());
        p.setBorder(BorderFactory.createLineBorder(c));
        JLabel title = new JLabel(t, SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Arial", Font.BOLD, 20));
        p.add(title, BorderLayout.NORTH);
        p.add(l, BorderLayout.CENTER);
        return p;
    }

    // GAMEPLAY LOGIC

    private void handleHumanMove(int val) {
        if(!humanTurn || selectedIndex == -1) return;
        
        Node n = gameGraph.nodes[selectedIndex];
        
        if(n.value != 0) { log("Cell occupied!"); return; }

        if(val == n.solutionVal) {
            n.value = val; n.owner = 1;
            moveHistory.push(n.id);
            log("Correct! Captured Node " + n.id);
            
            // Card drop rate : 100 %
            if(rand.nextInt(100) < 100) { 
                Card c = Card.values()[rand.nextInt(Card.values().length)];
                humanHand.add(c);
                log("LOOT: Captured " + c.name());
            }
            
            updateUI();
            setCardButtonsEnabled(true);
            checkWin();

            if(doubleMoveActive) {
                doubleMoveActive = false;
                log("DOUBLE Effect: Go again!");
                return;
            }
        } else {
            log("Wrong value! Turn lost.");
            setCardButtonsEnabled(false);
        }
        
        humanTurn = false;
        updateUI();
        
        if(skipCpuNext) {
            skipCpuNext = false;
            humanTurn = true;
            log("SKIP Effect: CPU turn skipped!");
            updateUI();
        } else {
            Timer t = new Timer(1000, e -> cpuGreedyGraphTurn());
            t.setRepeats(false); t.start();
        }
    }

    private void useCard(Card type) {
        if(!humanHand.contains(type)) return;
        
        if(type == Card.SKIP) { skipCpuNext = true; log("SKIP activated."); }
        if(type == Card.DOUBLE) { doubleMoveActive = true; log("DOUBLE activated."); }
        if(type == Card.UNDO && !moveHistory.isEmpty()) {
            int lastId = moveHistory.peek();
            Node n = gameGraph.nodes[lastId];
            if(n.owner == 2) {
                n.value = 0; n.owner = 0; moveHistory.pop();
                log("UNDO: CPU move reverted.");
            } else {
                 log("UNDO Failed: Can only undo CPU moves.");
                 return;
            }
        }
        
        humanHand.remove(type);
        updateUI();
    }






    /*
      3 GREEDY ALGORITHMS IMPLEMENTED HERE 

      1. OFFENSIVE GREEDY: Clustering (Building territory)
      2. DEFENSIVE GREEDY: Blocking (Stopping the human)
      3. POSITIONAL GREEDY: Center Control (Taking strategic nodes)

     */
    private void cpuGreedyGraphTurn() {
        if(isFull()) { checkWin(); return; }

        Node bestNode = null;
        int maxScore = -999;

        for(Node n : gameGraph.nodes) {
            if(n.value == 0) {
                int score = 0;

                
                for(Node neighbor : n.neighbors) {
                    if(neighbor.owner == 2) score += 20; // ALGO 1: Offensive (Cluster)
                    if(neighbor.owner == 1) score += 10; // ALGO 2: Defensive (Block)
                }
                boolean isCenter = (n.r >= 3 && n.r <= 5) && (n.c >= 3 && n.c <= 5);
                if(isCenter) score += 5; // ALGO 3: Positional (Center control)

                if(score > maxScore) {
                    maxScore = score;
                    bestNode = n;
                }
            }
        }

        // Fallback
        if(bestNode == null) {
            for(Node n : gameGraph.nodes) if(n.value==0) { bestNode = n; break; }
        }

        if(bestNode != null) {
            bestNode.value = bestNode.solutionVal;
            bestNode.owner = 2; // CPU
            moveHistory.push(bestNode.id);
            log("CPU took Node " + bestNode.id + " (Score: " + maxScore + ")");
        }

        humanTurn = true;
        updateUI();
        checkWin();
    }





    private void updateUI() {
        int h = 0, c = 0;
        
        // Update Board
        for(int i=0; i<81; i++) {
            Node n = gameGraph.nodes[i];
            JButton b = cellButtons[i];
            
            if(n.owner == 1) h++;
            if(n.owner == 2) c++;

            int r = i/9, col = i%9;
            int top=(r%3==0)?3:1, left=(col%3==0)?3:1, bot=(r%3==2)?3:1, right=(col%3==2)?3:1;
            Color borderCol = (i==selectedIndex)?Color.ORANGE : Color.GRAY;
            b.setBorder(BorderFactory.createMatteBorder(top, left, bot, right, borderCol));

            if(n.value != 0) {
                b.setText(String.valueOf(n.value));
                if(n.fixed) {
                    b.setBackground(COL_FIXED); b.setForeground(Color.BLACK);
                } else if(n.owner == 1) {
                    b.setBackground(COL_HUMAN); b.setForeground(Color.BLACK);
                } else {
                    b.setBackground(COL_CPU); b.setForeground(Color.BLACK);
                }
            } else {
                b.setText(""); b.setBackground(Color.WHITE);
            }
        }
        
        lblHumanScore.setText(String.valueOf(h));
        lblCpuScore.setText(String.valueOf(c));
        lblTurn.setText(humanTurn ? "YOUR TURN" : "CPU THINKING...");
        



        // Update Cards
        pnlHumanCards.removeAll();
        if(humanHand.isEmpty()) {
             JLabel l = new JLabel("(No cards)");
             l.setForeground(Color.GRAY);
             pnlHumanCards.add(l);
        } else {
            for(Card card : humanHand) {
                JLabel lbl = new JLabel(card.name());
                lbl.setOpaque(true);
                lbl.setBackground(card.color);
                lbl.setForeground(card==Card.UNDO ? Color.BLACK : Color.WHITE);
                lbl.setFont(new Font("Arial", Font.BOLD, 10));
                lbl.setBorder(BorderFactory.createLineBorder(Color.WHITE));
                pnlHumanCards.add(lbl);
            }
        }
        pnlHumanCards.revalidate(); pnlHumanCards.repaint();
    }
    
    private void setCardButtonsEnabled(boolean en) {
        btnSkip.setEnabled(en); btnUndo.setEnabled(en); btnDouble.setEnabled(en);
    }
    private boolean isFull() { for(Node n : gameGraph.nodes) if(n.value == 0) return false; return true; }
    private void checkWin() {
        if(!isFull()) return;
        int h = Integer.parseInt(lblHumanScore.getText());
        int c = Integer.parseInt(lblCpuScore.getText());
        JOptionPane.showMessageDialog(this, (h > c) ? "VICTORY!" : "DEFEAT!");
    }
    private void log(String s) { 
        logArea.append("> " + s + "\n"); logArea.setCaretPosition(logArea.getDocument().getLength()); 
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BattleSudokuUNO::new);
    }
}