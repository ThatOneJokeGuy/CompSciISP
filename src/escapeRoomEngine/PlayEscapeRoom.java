package escapeRoomEngine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;

/**
 * PlayEscapeRoom
 *
 * Simple gameplay controller + puzzle windows.
 * - Opens puzzle windows based on Room mode:
 *   LINEAR / SEMI_OPEN: one puzzle window at a time (next unsolved unlocked puzzle)
 *   OPEN:
 *     - SWITCHER: one puzzle window at a time + Switch button
 *     - MULTI_WINDOWS: opens one window per available unsolved puzzle
 *
 * NOTE:
 * - We intentionally SKIP already-solved puzzles when choosing what to open next,
 *   because Room.canAttemptPuzzle(...) returns true for solved puzzles (so the UI can show them),
 *   but for gameplay flow we want to move forward.
 */
public class PlayEscapeRoom {

    private EscapeRoom escapeRoom;
    private String roomFilePath;
    private PlayerProgress progress;

    private String saveFilePath = null;
    private ArrayList<PuzzleWindow> openWindows = new ArrayList<>();

    public PlayEscapeRoom(EscapeRoom escapeRoom, String roomFilePath, PlayerProgress progress) {
        this.escapeRoom = escapeRoom;
        this.roomFilePath = (roomFilePath == null) ? "" : roomFilePath;
        this.progress = (progress == null) ? new PlayerProgress() : progress;

        if (this.escapeRoom == null) {
            JOptionPane.showMessageDialog(null, "EscapeRoom is null. Cannot start game.");
            return;
        }

        if (this.progress.getCurrentRoomId() == null || this.progress.getCurrentRoomId().isEmpty()) {
            if (escapeRoom.getRoomCount() > 0) {
                this.progress.setCurrentRoomId(escapeRoom.getRoomByIndex(0).getId());
            }
        }

        applyProgressToEscapeRoom(this.escapeRoom, this.progress);
        enterCurrentRoom();
    }

    // --------------------------------------------------
    // Room flow
    // --------------------------------------------------

    private void enterCurrentRoom() {
        Room room = getCurrentRoom();
        if (room == null) {
            JOptionPane.showMessageDialog(null, "No valid current room. Game cannot continue.");
            return;
        }

        closeAllPuzzleWindows();

        if (escapeRoom.isWon()) {
            showWinAndReturn();
            return;
        }

        if (room.getMode() == Room.RoomMode.OPEN) {
            if (room.getOpenDisplayStyle() == Room.OpenDisplayStyle.MULTI_WINDOWS) {
                openAllAvailableUnsolvedPuzzles(room);
            } else {
                // SWITCHER
                openFirstAvailableUnsolvedPuzzle(room, true);
            }
        } else {
            // LINEAR or SEMI_OPEN
            openFirstAvailableUnsolvedPuzzle(room, false);
        }
    }

    private Room getCurrentRoom() {
        return escapeRoom.getRoomById(progress.getCurrentRoomId());
    }

    private int getCurrentRoomIndex() {
        return escapeRoom.getRoomIndexById(progress.getCurrentRoomId());
    }

    private void tryAutoAdvanceRoom() {
        Room room = getCurrentRoom();
        if (room == null) return;

        if (escapeRoom.isWon()) {
            showWinAndReturn();
            return;
        }

        if (escapeRoom.getStructureMode() == EscapeRoom.StructureMode.LINEAR && room.isComplete()) {
            int idx = getCurrentRoomIndex();
            int next = idx + 1;
            if (next < escapeRoom.getRoomCount()) {
                progress.setCurrentRoomId(escapeRoom.getRoomByIndex(next).getId());
                enterCurrentRoom();
            } else {
                // No next room
                JOptionPane.showMessageDialog(null,
                        "You finished the last room.\nIf you didn't win, check your Win Condition.",
                        "Finished",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    // --------------------------------------------------
    // Puzzle opening helpers
    // --------------------------------------------------

    private void openAllAvailableUnsolvedPuzzles(Room room) {
        boolean openedAny = false;

        for (int i = 0; i < room.getPuzzleCount(); i++) {
            Puzzle p = room.getPuzzle(i);
            if (p == null) continue;

            if (room.canAttemptPuzzle(i) && !p.isSolved()) {
                PuzzleWindow w = new PuzzleWindow(room, i, false);
                openWindows.add(w);
                w.setVisible(true);
                openedAny = true;
            }
        }

        if (!openedAny) {
            if (room.isComplete()) {
                tryAutoAdvanceRoom();
            } else {
                JOptionPane.showMessageDialog(null,
                        "No unsolved puzzles are available right now.\n(If this is SEMI_OPEN, solve more to unlock the last puzzle.)");
            }
        }
    }

    private void openFirstAvailableUnsolvedPuzzle(Room room, boolean allowSwitcher) {
        // 1) Prefer an unsolved, attemptable puzzle
        for (int i = 0; i < room.getPuzzleCount(); i++) {
            Puzzle p = room.getPuzzle(i);
            if (p == null) continue;

            if (room.canAttemptPuzzle(i) && !p.isSolved()) {
                PuzzleWindow w = new PuzzleWindow(room, i, allowSwitcher);
                openWindows.add(w);
                w.setVisible(true);
                return;
            }
        }

        // 2) If none found, either room is complete or everything is locked/solved
        if (room.isComplete()) {
            tryAutoAdvanceRoom();
        } else {
            JOptionPane.showMessageDialog(null,
                    "No unsolved puzzles are unlocked yet in this room.",
                    "Locked",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void closeAllPuzzleWindows() {
        for (PuzzleWindow w : openWindows) {
            try { w.dispose(); } catch (Exception ignored) {}
        }
        openWindows.clear();
    }

    // --------------------------------------------------
    // Save
    // --------------------------------------------------

    private void saveProgress(Component parent) {
        captureProgressFromEscapeRoom(escapeRoom, progress);

        if (saveFilePath == null) {
            JFileChooser chooser = new JFileChooser(new File(System.getProperty("user.dir")));
            chooser.setDialogTitle("Choose where to save your progress (.txt)");
            if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

            File f = chooser.getSelectedFile();
            if (f == null) return;

            if (!f.getName().toLowerCase().endsWith(".txt")) {
                f = new File(f.getAbsolutePath() + ".txt");
            }
            saveFilePath = f.getAbsolutePath();
        }

        try {
            SaveManager.saveProgress(saveFilePath, roomFilePath, progress);
            JOptionPane.showMessageDialog(parent, "Progress saved:\n" + saveFilePath);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent, "Save failed:\n" + e.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --------------------------------------------------
    // Win
    // --------------------------------------------------

    private void showWinAndReturn() {
        closeAllPuzzleWindows();
        JOptionPane.showMessageDialog(null, "🎉 You beat the escape room!\n\n" + escapeRoom.getTitle(),
                "You Win!", JOptionPane.INFORMATION_MESSAGE);
        SwingUtilities.invokeLater(() -> new MainMenuGUI().setVisible(true));
    }

    // --------------------------------------------------
    // Puzzle Window
    // --------------------------------------------------

    private class PuzzleWindow extends JFrame {

        private Room room;
        private Puzzle puzzle;
        private int index;
        private boolean allowSwitcher;

        private JTextArea txtPrompt;
        private JTextField txtAnswer;
        private JComboBox<String> cmbAnswer;
        private JLabel lblStatus;

        private JPanel answerPanel; // so we can swap controls

        public PuzzleWindow(Room room, int index, boolean allowSwitcher) {
            this.room = room;
            this.index = index;
            this.allowSwitcher = allowSwitcher;
            this.puzzle = (room == null) ? null : room.getPuzzle(index);

            setTitle((room == null) ? "Puzzle" : room.getTitle());
            setSize(550, 400);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            buildUI();
            refresh();

            addWindowListener(new WindowAdapter() {
                public void windowClosed(WindowEvent e) {
                    openWindows.remove(PuzzleWindow.this);
                }
            });
        }

        private void buildUI() {
            setLayout(new BorderLayout(10, 10));

            // Status at top
            lblStatus = new JLabel(" ", SwingConstants.CENTER);
            add(lblStatus, BorderLayout.NORTH);

            // Prompt center
            txtPrompt = new JTextArea((puzzle == null) ? "" : puzzle.getPrompt());
            txtPrompt.setEditable(false);
            txtPrompt.setLineWrap(true);
            txtPrompt.setWrapStyleWord(true);
            add(new JScrollPane(txtPrompt), BorderLayout.CENTER);

            // Answer panel
            answerPanel = new JPanel(new BorderLayout(6, 6));
            answerPanel.setBorder(BorderFactory.createTitledBorder("Your Answer"));

            txtAnswer = new JTextField();
            cmbAnswer = new JComboBox<>();

            setupAnswerInput(); // IMPORTANT: actually put the right control in the panel

            // Buttons row
            JPanel buttons = new JPanel(new GridLayout(1, 5, 5, 5));

            JButton btnSubmit = new JButton("Submit");
            JButton btnHint = new JButton("Hint");
            JButton btnSave = new JButton("Save");
            JButton btnMenu = new JButton("Menu");

            btnSubmit.addActionListener(e -> submit());
            btnHint.addActionListener(e -> hint());
            btnSave.addActionListener(e -> saveProgress(this));
            btnMenu.addActionListener(e -> {
                closeAllPuzzleWindows();
                new MainMenuGUI().setVisible(true);
            });

            buttons.add(btnSubmit);
            buttons.add(btnHint);

            if (allowSwitcher && room != null && room.getMode() == Room.RoomMode.OPEN
                    && room.getOpenDisplayStyle() == Room.OpenDisplayStyle.SWITCHER) {
                JButton btnSwitch = new JButton("Switch");
                btnSwitch.setToolTipText("Close this puzzle and open another puzzle in this room.");
                btnSwitch.addActionListener(e -> switchPuzzle());
                buttons.add(btnSwitch);
            } else {
                buttons.add(new JLabel()); // spacer
            }

            buttons.add(btnSave);
            buttons.add(btnMenu);

            JPanel bottomStack = new JPanel(new BorderLayout(6, 6));
            bottomStack.add(answerPanel, BorderLayout.CENTER);
            bottomStack.add(buttons, BorderLayout.SOUTH);

            add(bottomStack, BorderLayout.SOUTH);
        }

        private void setupAnswerInput() {
            answerPanel.removeAll();

            if (puzzle == null) {
                answerPanel.add(new JLabel("No puzzle loaded."), BorderLayout.CENTER);
            } else if (puzzle.getType() == Puzzle.PuzzleType.MULTIPLE_CHOICE) {
                cmbAnswer.removeAllItems();
                for (String opt : puzzle.getOptions()) cmbAnswer.addItem(opt);
                answerPanel.add(cmbAnswer, BorderLayout.CENTER);
            } else if (puzzle.getType() == Puzzle.PuzzleType.TRUE_FALSE) {
                cmbAnswer.removeAllItems();
                cmbAnswer.addItem("True");
                cmbAnswer.addItem("False");
                answerPanel.add(cmbAnswer, BorderLayout.CENTER);
            } else {
                // TEXT or MATH
                txtAnswer.setText("");
                answerPanel.add(txtAnswer, BorderLayout.CENTER);
            }

            answerPanel.revalidate();
            answerPanel.repaint();
        }

        private void refresh() {
            if (puzzle == null) return;

            if (puzzle.isSolved()) {
                lblStatus.setText("Solved ✅");
            } else if (!puzzle.hasAttemptsRemaining()) {
                lblStatus.setText("No attempts left ❌");
            } else {
                lblStatus.setText("Attempts left: " + puzzle.getAttemptsRemaining());
            }

            txtPrompt.setText(puzzle.getPrompt());
        }

        private String getUserInput() {
            if (puzzle == null) return "";

            if (puzzle.getType() == Puzzle.PuzzleType.MULTIPLE_CHOICE || puzzle.getType() == Puzzle.PuzzleType.TRUE_FALSE) {
                Object sel = cmbAnswer.getSelectedItem();
                return (sel == null) ? "" : sel.toString();
            }
            return txtAnswer.getText();
        }

        private void submit() {
            if (room == null || puzzle == null) return;

            if (!room.canAttemptPuzzle(index)) {
                JOptionPane.showMessageDialog(this, "Puzzle is locked.");
                return;
            }

            if (puzzle.isSolved()) {
                JOptionPane.showMessageDialog(this, "This puzzle is already solved.");
                return;
            }

            if (!puzzle.hasAttemptsRemaining()) {
                JOptionPane.showMessageDialog(this, "No attempts remaining for this puzzle.");
                return;
            }

            boolean solved = puzzle.attemptAnswer(getUserInput());

            captureProgressFromEscapeRoom(escapeRoom, progress);
            refresh();

            if (solved) {
                JOptionPane.showMessageDialog(this, "Correct!");
                dispose();

                if (room.isComplete()) {
                    tryAutoAdvanceRoom();
                } else {
                    boolean nextAllowSwitcher =
                            room.getMode() == Room.RoomMode.OPEN &&
                            room.getOpenDisplayStyle() == Room.OpenDisplayStyle.SWITCHER;

                    openFirstAvailableUnsolvedPuzzle(room, nextAllowSwitcher);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Incorrect.\nAttempts left: " + puzzle.getAttemptsRemaining());
            }
        }

        private void hint() {
            if (puzzle == null) return;

            String h = puzzle.useHint();
            captureProgressFromEscapeRoom(escapeRoom, progress);
            refresh();

            if (h == null || h.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "(No hint provided.)");
            } else {
                JOptionPane.showMessageDialog(this, h, "Hint", JOptionPane.INFORMATION_MESSAGE);
            }
        }

        private void switchPuzzle() {
            if (room == null) return;

            ArrayList<String> labels = new ArrayList<>();
            ArrayList<Integer> idxs = new ArrayList<>();

            for (int i = 0; i < room.getPuzzleCount(); i++) {
                if (!room.canAttemptPuzzle(i)) continue;
                Puzzle p = room.getPuzzle(i);
                if (p == null) continue;

                String label = p.getId();
                if (p.isSolved()) label += " (Solved)";
                labels.add(label);
                idxs.add(i);
            }

            if (labels.size() == 0) {
                JOptionPane.showMessageDialog(this, "No puzzles available to switch to.");
                return;
            }

            Object choice = JOptionPane.showInputDialog(
                    this,
                    "Choose a puzzle:",
                    "Switch Puzzle",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    labels.toArray(),
                    labels.get(0)
            );

            if (choice == null) return;

            int sel = labels.indexOf(choice.toString());
            if (sel < 0) return;

            int newIndex = idxs.get(sel);

            dispose();

            PuzzleWindow w = new PuzzleWindow(room, newIndex, true);
            openWindows.add(w);
            w.setVisible(true);
        }
    }

    // --------------------------------------------------
    // Progress sync
    // --------------------------------------------------

    private static void applyProgressToEscapeRoom(EscapeRoom er, PlayerProgress p) {
        if (er == null || p == null) return;

        for (int r = 0; r < er.getRoomCount(); r++) {
            Room room = er.getRoomByIndex(r);
            if (room == null) continue;

            for (int i = 0; i < room.getPuzzleCount(); i++) {
                Puzzle puz = room.getPuzzle(i);
                if (puz == null) continue;

                if (p.isPuzzleSolved(puz.getId())) puz.setSolved(true);

                puz.setAttemptsUsed(p.getAttemptCount(puz.getId()));
                puz.setHintsUsed(p.getHintsUsed(puz.getId()));
            }
        }
    }

    private static void captureProgressFromEscapeRoom(EscapeRoom er, PlayerProgress p) {
        if (er == null || p == null) return;

        for (int r = 0; r < er.getRoomCount(); r++) {
            Room room = er.getRoomByIndex(r);
            if (room == null) continue;

            for (int i = 0; i < room.getPuzzleCount(); i++) {
                Puzzle puz = room.getPuzzle(i);
                if (puz == null) continue;

                if (puz.isSolved()) p.markPuzzleSolved(puz.getId());

                p.setAttemptCount(puz.getId(), puz.getAttemptsUsed());
                p.setHintsUsed(puz.getId(), puz.getHintsUsed());
            }
        }
    }
}
