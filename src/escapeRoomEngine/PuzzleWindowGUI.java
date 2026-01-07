package escapeRoomEngine;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * PuzzleWindowGUI
 *
 * A basic window that shows ONE puzzle.
 *
 * UI rules (simple):
 * - TEXT / MATH: JTextField input
 * - MULTIPLE_CHOICE / TRUE_FALSE: JComboBox input
 * - Buttons: Submit, Hint, Save, Main Menu
 *
 * This does NOT handle room navigation yet. It simply plays:
 * - the current room in EscapeRoomGame
 * - one puzzle index in that room
 */
public class PuzzleWindowGUI extends JFrame {

    private EscapeRoomGame engine;
    private String roomFilePath;
    private String saveFilePath; // optional (if null, user will choose on Save)

    private int puzzleIndex;

    // UI
    private JLabel lblHeader;
    private JTextArea txtPrompt;

    private JPanel pnlAnswer;
    private JTextField txtAnswer;
    private JComboBox<String> cmbAnswer;

    private JLabel lblAttempts;
    private JLabel lblFeedback;

    private JButton btnSubmit;
    private JButton btnHint;
    private JButton btnSave;
    private JButton btnMainMenu;

    public PuzzleWindowGUI(EscapeRoomGame engine, String roomFilePath, String saveFilePath, int puzzleIndex) {
        super("Puzzle");

        this.engine = engine;
        this.roomFilePath = roomFilePath;
        this.saveFilePath = saveFilePath;
        this.puzzleIndex = puzzleIndex;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(520, 420);
        setLocationRelativeTo(null);

        buildUI();
        loadPuzzleIntoUI();
    }

    // -------------------------
    // UI
    // -------------------------

    private void buildUI() {
        setLayout(new BorderLayout(10, 10));

        lblHeader = new JLabel("Room / Puzzle", SwingConstants.CENTER);
        lblHeader.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblHeader.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        add(lblHeader, BorderLayout.NORTH);

        // Prompt
        txtPrompt = new JTextArea(6, 30);
        txtPrompt.setEditable(false);
        txtPrompt.setLineWrap(true);
        txtPrompt.setWrapStyleWord(true);
        txtPrompt.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JScrollPane promptScroll = new JScrollPane(txtPrompt);
        promptScroll.setBorder(BorderFactory.createTitledBorder("Puzzle"));
        add(promptScroll, BorderLayout.CENTER);

        // South area: answer + status + buttons
        JPanel south = new JPanel(new BorderLayout(8, 8));
        south.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        pnlAnswer = new JPanel(new BorderLayout(6, 6));
        pnlAnswer.setBorder(BorderFactory.createTitledBorder("Your Answer"));

        txtAnswer = new JTextField();
        cmbAnswer = new JComboBox<>();

        // We will switch which component is used in loadPuzzleIntoUI()
        pnlAnswer.add(txtAnswer, BorderLayout.CENTER);

        south.add(pnlAnswer, BorderLayout.NORTH);

        // Status line
        JPanel status = new JPanel(new GridLayout(2, 1));
        lblAttempts = new JLabel("Attempts remaining: ?");
        lblFeedback = new JLabel(" ");
        status.add(lblAttempts);
        status.add(lblFeedback);

        south.add(status, BorderLayout.CENTER);

        // Buttons
        JPanel buttons = new JPanel(new GridLayout(1, 4, 8, 8));
        btnSubmit = new JButton("Submit");
        btnHint = new JButton("Hint");
        btnSave = new JButton("Save");
        btnMainMenu = new JButton("Main Menu");

        buttons.add(btnSubmit);
        buttons.add(btnHint);
        buttons.add(btnSave);
        buttons.add(btnMainMenu);

        south.add(buttons, BorderLayout.SOUTH);

        add(south, BorderLayout.SOUTH);

        // Actions
        btnSubmit.addActionListener(e -> onSubmit());
        btnHint.addActionListener(e -> onHint());
        btnSave.addActionListener(e -> onSave());
        btnMainMenu.addActionListener(e -> onMainMenu());
    }

    // -------------------------
    // Load puzzle data into the UI
    // -------------------------

    private void loadPuzzleIntoUI() {
        if (engine == null) {
            showError("Engine is null.");
            return;
        }

        Room room = engine.getCurrentRoom();
        if (room == null) {
            showError("No current room loaded.");
            return;
        }

        Puzzle puzzle = room.getPuzzle(puzzleIndex);
        if (puzzle == null) {
            showError("Puzzle index " + puzzleIndex + " is invalid.");
            return;
        }

        // Header: Room + Puzzle count
        lblHeader.setText("Room: " + room.getTitle() + "  |  Puzzle " + (puzzleIndex + 1) + "/" + room.getPuzzleCount());

        // Prompt
        txtPrompt.setText(puzzle.getPrompt());

        // Answer input type
        swapAnswerComponentForPuzzle(puzzle);

        // Attempts + feedback
        updateAttemptsAndFeedback("");
        updateLockedState(room, puzzle);
    }

    private void swapAnswerComponentForPuzzle(Puzzle puzzle) {
        pnlAnswer.removeAll();

        if (puzzle.getType() == Puzzle.PuzzleType.TEXT || puzzle.getType() == Puzzle.PuzzleType.MATH) {
            txtAnswer.setText("");
            pnlAnswer.add(txtAnswer, BorderLayout.CENTER);

        } else if (puzzle.getType() == Puzzle.PuzzleType.TRUE_FALSE) {
            cmbAnswer.removeAllItems();
            cmbAnswer.addItem("True");
            cmbAnswer.addItem("False");
            pnlAnswer.add(cmbAnswer, BorderLayout.CENTER);

        } else if (puzzle.getType() == Puzzle.PuzzleType.MULTIPLE_CHOICE) {
            cmbAnswer.removeAllItems();
            for (String opt : puzzle.getOptions()) {
                cmbAnswer.addItem(opt);
            }
            pnlAnswer.add(cmbAnswer, BorderLayout.CENTER);

        } else {
            // Fallback: treat as text
            txtAnswer.setText("");
            pnlAnswer.add(txtAnswer, BorderLayout.CENTER);
        }

        pnlAnswer.revalidate();
        pnlAnswer.repaint();
    }

    private void updateAttemptsAndFeedback(String feedback) {
        Room room = engine.getCurrentRoom();
        if (room == null) return;

        Puzzle puzzle = room.getPuzzle(puzzleIndex);
        if (puzzle == null) return;

        lblAttempts.setText("Attempts remaining: " + puzzle.getAttemptsRemaining());
        if (feedback == null || feedback.trim().isEmpty()) {
            lblFeedback.setText(" ");
        } else {
            lblFeedback.setText(feedback);
        }
    }

    private void updateLockedState(Room room, Puzzle puzzle) {
        boolean locked = !room.canAttemptPuzzle(puzzleIndex) && !puzzle.isSolved();

        // If solved, disable submit
        if (puzzle.isSolved()) {
            btnSubmit.setEnabled(false);
            updateAttemptsAndFeedback("Solved ✅");
            return;
        }

        // If locked, disable submit
        if (locked) {
            btnSubmit.setEnabled(false);
            updateAttemptsAndFeedback("Locked 🔒 (solve earlier puzzles first)");
            return;
        }

        // If no attempts, disable submit
        if (!puzzle.hasAttemptsRemaining()) {
            btnSubmit.setEnabled(false);
            updateAttemptsAndFeedback("No attempts left.");
            return;
        }

        btnSubmit.setEnabled(true);
    }

    // -------------------------
    // Button actions
    // -------------------------

    private void onSubmit() {
        Room room = engine.getCurrentRoom();
        if (room == null) return;

        Puzzle puzzle = room.getPuzzle(puzzleIndex);
        if (puzzle == null) return;

        String userInput = getUserAnswerInput(puzzle);

        EscapeRoomGame.AttemptResult result = engine.attemptCurrentRoomPuzzle(puzzleIndex, userInput);

        updateAttemptsAndFeedback(result.getMessage());
        updateLockedState(room, puzzle);

        if (result.isOk() && result.isSolved()) {
            JOptionPane.showMessageDialog(this, "Puzzle solved!", "Solved", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private String getUserAnswerInput(Puzzle puzzle) {
        if (puzzle.getType() == Puzzle.PuzzleType.TEXT || puzzle.getType() == Puzzle.PuzzleType.MATH) {
            return txtAnswer.getText();
        }
        Object selected = cmbAnswer.getSelectedItem();
        return (selected == null) ? "" : selected.toString();
    }

    private void onHint() {
        EscapeRoomGame.HintResult hint = engine.useHintForCurrentRoomPuzzle(puzzleIndex);
        if (!hint.isOk()) {
            showError(hint.getText());
            return;
        }

        JOptionPane.showMessageDialog(this, hint.getText(), "Hint", JOptionPane.INFORMATION_MESSAGE);
        updateAttemptsAndFeedback("Hint used.");
    }

    /**
     * Now fully wired:
     * - If no save file chosen yet, asks once
     * - Writes to SaveManager.saveProgress(...)
     */
    private void onSave() {
        if (engine == null || engine.getProgress() == null) {
            showError("Cannot save: progress not initialized.");
            return;
        }

        // We need roomFilePath saved in the save file so load-save works properly
        if (roomFilePath == null) roomFilePath = "";

        // Ask for save file path if we don't already have one
        if (saveFilePath == null || saveFilePath.trim().isEmpty()) {
            JFileChooser chooser = new JFileChooser(new File(System.getProperty("user.dir")));
            chooser.setDialogTitle("Save Progress As (.txt)");

            int result = chooser.showSaveDialog(this);
            if (result != JFileChooser.APPROVE_OPTION) return;

            File f = chooser.getSelectedFile();
            if (f == null) return;

            if (!f.getName().toLowerCase().endsWith(".txt")) {
                f = new File(f.getAbsolutePath() + ".txt");
            }
            saveFilePath = f.getAbsolutePath();
        }

        try {
            SaveManager.saveProgress(saveFilePath, roomFilePath, engine.getProgress());
            JOptionPane.showMessageDialog(this, "Progress saved!", "Saved", JOptionPane.INFORMATION_MESSAGE);
            updateAttemptsAndFeedback("Saved ✅");
        } catch (Exception ex) {
            showError("Save failed:\n" + ex.getMessage());
        }
    }

    private void onMainMenu() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Return to Main Menu?\n(Your progress is not saved unless you click Save.)",
                "Confirm",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (choice != JOptionPane.YES_OPTION) return;

        SwingUtilities.invokeLater(() -> {
            MainMenuGUI menu = new MainMenuGUI();
            menu.setVisible(true);
        });
        dispose();
    }

    // -------------------------
    // Helpers
    // -------------------------

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
