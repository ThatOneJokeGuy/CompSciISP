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
 * UPDATE:
 * - Prevents duplicate "completed" popups by NOT showing a "Correct!" popup.
 *   (The window closes + next puzzle/room opens instead.)
 * - Prevents duplicate WIN popup using a winShown boolean.
 *
 * OPEN + MULTI_WINDOWS:
 * - Solving one puzzle closes only that puzzle window.
 * - It will ONLY advance to next room when:
 *      (room is complete) AND (all puzzle windows are closed)
 */
public class PlayEscapeRoom {

    private EscapeRoom escapeRoom;
    private String roomFilePath;
    private PlayerProgress progress;

    private String saveFilePath = null;

    // Track currently open puzzle windows
    private ArrayList<PuzzleWindow> openWindows = new ArrayList<>();

    // Prevent duplicate win popup
    private boolean winShown = false;

    public PlayEscapeRoom(EscapeRoom escapeRoom, String roomFilePath, PlayerProgress progress) {
        this.escapeRoom = escapeRoom;
        this.roomFilePath = (roomFilePath == null) ? "" : roomFilePath;
        this.progress = (progress == null) ? new PlayerProgress() : progress;

        if (this.escapeRoom == null) {
            JOptionPane.showMessageDialog(null, "EscapeRoom is null. Cannot start game.");
            return;
        }

        // Default to first room if none set
        if (this.progress.getCurrentRoomId() == null || this.progress.getCurrentRoomId().trim().isEmpty()) {
            if (escapeRoom.getRoomCount() > 0) {
                this.progress.setCurrentRoomId(escapeRoom.getRoomByIndex(0).getId());
            }
        }

        // Apply loaded progress
        applyProgressToEscapeRoom(this.escapeRoom, this.progress);

        // Start
        enterCurrentRoom();
    }

    // --------------------------------------------------
    // Room Flow
    // --------------------------------------------------

    private void enterCurrentRoom() {
        Room room = getCurrentRoom();
        if (room == null) {
            JOptionPane.showMessageDialog(null, "No valid current room. Game cannot continue.");
            return;
        }

        // Close old windows
        closeAllPuzzleWindows();

        // If already won, show win
        if (escapeRoom.isWon()) {
            showWinAndReturn();
            return;
        }

        // Decide how to display puzzles
        if (room.getMode() == Room.RoomMode.OPEN) {
            if (room.getOpenDisplayStyle() == Room.OpenDisplayStyle.MULTI_WINDOWS) {
                openAllAvailablePuzzles(room);
            } else {
                // SWITCHER
                openFirstAvailablePuzzle(room, true);
            }
        } else {
            // LINEAR or SEMI_OPEN
            openFirstAvailablePuzzle(room, false);
        }
    }

    private Room getCurrentRoom() {
        if (escapeRoom == null || progress == null) return null;
        return escapeRoom.getRoomById(progress.getCurrentRoomId());
    }

    private int getCurrentRoomIndex() {
        if (escapeRoom == null || progress == null) return -1;
        return escapeRoom.getRoomIndexById(progress.getCurrentRoomId());
    }

    /**
     * Advances to the next room if it exists, otherwise checks win.
     * (Your EscapeRoom is now basically linear)
     */
    private void advanceToNextRoomIfPossible() {
        if (escapeRoom.isWon()) {
            showWinAndReturn();
            return;
        }

        int idx = getCurrentRoomIndex();
        int next = idx + 1;

        if (next >= 0 && next < escapeRoom.getRoomCount()) {
            progress.setCurrentRoomId(escapeRoom.getRoomByIndex(next).getId());
            enterCurrentRoom();
        } else {
            // No more rooms
            if (escapeRoom.isWon()) showWinAndReturn();
            else JOptionPane.showMessageDialog(null, "No more rooms. If you didn't win, check your last room puzzles.");
        }
    }

    /**
     * Called when a puzzle window closes.
     * For OPEN+MULTI, only advance once ALL windows are closed AND the room is complete.
     */
    private void checkAdvanceAfterWindowClosed(Room room) {
        if (room == null) return;

        // Win: show only after last window is gone (prevents double win)
        if (escapeRoom.isWon()) {
            if (openWindows.size() == 0) showWinAndReturn();
            return;
        }

        // Only special-case OPEN + MULTI_WINDOWS
        if (room.getMode() == Room.RoomMode.OPEN &&
                room.getOpenDisplayStyle() == Room.OpenDisplayStyle.MULTI_WINDOWS) {

            if (room.isComplete() && openWindows.size() == 0) {
                advanceToNextRoomIfPossible();
            }
        }
    }

    // --------------------------------------------------
    // Puzzle Opening Helpers
    // --------------------------------------------------

    private void openAllAvailablePuzzles(Room room) {
        if (room == null) return;

        boolean opened = false;

        for (int i = 0; i < room.getPuzzleCount(); i++) {
            Puzzle p = room.getPuzzle(i);
            if (p != null && !p.isSolved() && room.canAttemptPuzzle(i)) {
                PuzzleWindow w = new PuzzleWindow(room, i, false);
                openWindows.add(w);
                w.setVisible(true);
                opened = true;
            }
        }

        if (!opened) {
            // If none available, check if room is complete
            if (room.isComplete()) {
                // For OPEN+MULTI, if nothing opened and no windows exist, advance immediately
                if (openWindows.size() == 0) advanceToNextRoomIfPossible();
            } else {
                JOptionPane.showMessageDialog(null, "No puzzles are available in this room right now.");
            }
        }
    }

    private void openFirstAvailablePuzzle(Room room, boolean allowSwitcher) {
        if (room == null) return;

        // Prefer an UNSOLVED puzzle
        for (int i = 0; i < room.getPuzzleCount(); i++) {
            Puzzle p = room.getPuzzle(i);
            if (p != null && !p.isSolved() && room.canAttemptPuzzle(i)) {
                PuzzleWindow w = new PuzzleWindow(room, i, allowSwitcher);
                openWindows.add(w);
                w.setVisible(true);
                return;
            }
        }

        // None found
        if (room.isComplete()) {
            advanceToNextRoomIfPossible();
        } else {
            JOptionPane.showMessageDialog(null, "No puzzles are unlocked yet in this room.");
        }
    }

    private void closeAllPuzzleWindows() {
        for (PuzzleWindow w : new ArrayList<>(openWindows)) {
            try { w.dispose(); } catch (Exception ignored) {}
        }
        openWindows.clear();
    }

    // --------------------------------------------------
    // Save
    // --------------------------------------------------

    private void saveProgress(Component parent) {
        captureProgressFromEscapeRoom(escapeRoom, progress);

        if (saveFilePath == null || saveFilePath.trim().isEmpty()) {
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
            JOptionPane.showMessageDialog(parent, "Save failed:\n" + e.getMessage());
        }
    }

    // --------------------------------------------------
    // Win
    // --------------------------------------------------

    private void showWinAndReturn() {
        if (winShown) return;          // <- stops double win popup
        winShown = true;

        closeAllPuzzleWindows();
        JOptionPane.showMessageDialog(null, "🎉 You beat the escape room!\n\n" + escapeRoom.getTitle());

        SwingUtilities.invokeLater(() -> new MainMenuGUI().setVisible(true));
    }

    // --------------------------------------------------
    // Puzzle Window
    // --------------------------------------------------

    private class PuzzleWindow extends JFrame {

        private Room room;
        private Puzzle puzzle;
        private int index;

        private JTextArea txtPrompt;
        private JTextField txtAnswer;
        private JComboBox<String> cmbAnswer;
        private JLabel lblStatus;

        private JPanel answerPanel;

        public PuzzleWindow(Room room, int index, boolean allowSwitcher) {
            this.room = room;
            this.index = index;
            this.puzzle = (room == null) ? null : room.getPuzzle(index);

            setTitle(room.getTitle());
            setSize(560, 420);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            buildUI(allowSwitcher);
            swapAnswerInputForType();
            refresh();

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    openWindows.remove(PuzzleWindow.this);
                    checkAdvanceAfterWindowClosed(PuzzleWindow.this.room);
                }
            });
        }

        private void buildUI(boolean allowSwitcher) {
            setLayout(new BorderLayout(10, 10));

            lblStatus = new JLabel(" ", SwingConstants.CENTER);
            lblStatus.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
            add(lblStatus, BorderLayout.NORTH);

            txtPrompt = new JTextArea(puzzle.getPrompt());
            txtPrompt.setEditable(false);
            txtPrompt.setLineWrap(true);
            txtPrompt.setWrapStyleWord(true);
            txtPrompt.setFont(new Font("SansSerif", Font.PLAIN, 14));
            add(new JScrollPane(txtPrompt), BorderLayout.CENTER);

            // Answer panel
            answerPanel = new JPanel(new BorderLayout(6, 6));
            answerPanel.setBorder(BorderFactory.createTitledBorder("Your Answer"));

            txtAnswer = new JTextField();
            cmbAnswer = new JComboBox<>();
            answerPanel.add(txtAnswer, BorderLayout.CENTER);

            // Buttons
            JPanel buttons = new JPanel(new GridLayout(1, 5, 8, 8));
            buttons.setBorder(BorderFactory.createEmptyBorder(6, 10, 10, 10));

            JButton btnSubmit = new JButton("Submit");
            JButton btnHint = new JButton("Hint");
            JButton btnSave = new JButton("Save");
            JButton btnMenu = new JButton("Main Menu");

            btnSubmit.addActionListener(e -> submit());
            btnHint.addActionListener(e -> hint());
            btnSave.addActionListener(e -> saveProgress(this));
            btnMenu.addActionListener(e -> {
                closeAllPuzzleWindows();
                SwingUtilities.invokeLater(() -> new MainMenuGUI().setVisible(true));
            });

            buttons.add(btnSubmit);
            buttons.add(btnHint);

            if (allowSwitcher &&
                    room.getMode() == Room.RoomMode.OPEN &&
                    room.getOpenDisplayStyle() == Room.OpenDisplayStyle.SWITCHER) {

                JButton btnSwitch = new JButton("Switch");
                btnSwitch.setToolTipText("Close this puzzle and open another puzzle in the same room.");
                btnSwitch.addActionListener(e -> switchPuzzle());
                buttons.add(btnSwitch);
            } else {
                JButton spacer = new JButton(" ");
                spacer.setEnabled(false);
                buttons.add(spacer);
            }

            buttons.add(btnSave);
            buttons.add(btnMenu);

            JPanel bottomStack = new JPanel(new BorderLayout(6, 6));
            bottomStack.add(answerPanel, BorderLayout.CENTER);
            bottomStack.add(buttons, BorderLayout.SOUTH);

            add(bottomStack, BorderLayout.SOUTH);
        }

        private void swapAnswerInputForType() {
            answerPanel.removeAll();

            Puzzle.PuzzleType type = puzzle.getType();
            if (type == Puzzle.PuzzleType.MULTIPLE_CHOICE) {
                cmbAnswer.removeAllItems();
                for (String opt : puzzle.getOptions()) cmbAnswer.addItem(opt);
                answerPanel.add(cmbAnswer, BorderLayout.CENTER);
            } else if (type == Puzzle.PuzzleType.TRUE_FALSE) {
                cmbAnswer.removeAllItems();
                cmbAnswer.addItem("True");
                cmbAnswer.addItem("False");
                answerPanel.add(cmbAnswer, BorderLayout.CENTER);
            } else {
                txtAnswer.setText("");
                answerPanel.add(txtAnswer, BorderLayout.CENTER);
            }

            answerPanel.revalidate();
            answerPanel.repaint();
        }

        private void refresh() {
            lblStatus.setText(
                    puzzle.isSolved() ? "Solved ✅" :
                            puzzle.hasAttemptsRemaining() ? "Attempts left: " + puzzle.getAttemptsRemaining()
                                    : "No attempts left ❌"
            );
        }

        private String getUserInput() {
            Puzzle.PuzzleType type = puzzle.getType();
            if (type == Puzzle.PuzzleType.MULTIPLE_CHOICE || type == Puzzle.PuzzleType.TRUE_FALSE) {
                Object sel = cmbAnswer.getSelectedItem();
                return (sel == null) ? "" : sel.toString();
            }
            return txtAnswer.getText();
        }

        private void submit() {
            if (!room.canAttemptPuzzle(index)) {
                JOptionPane.showMessageDialog(this, "Puzzle is locked.");
                return;
            }
            if (puzzle.isSolved()) {
                JOptionPane.showMessageDialog(this, "This puzzle is already solved.");
                return;
            }
            if (!puzzle.hasAttemptsRemaining()) {
                JOptionPane.showMessageDialog(this, "No attempts remaining.");
                return;
            }

            boolean solved = puzzle.attemptAnswer(getUserInput());

            captureProgressFromEscapeRoom(escapeRoom, progress);
            refresh();

            if (!solved) {
                JOptionPane.showMessageDialog(this, "Incorrect.\nAttempts left: " + puzzle.getAttemptsRemaining());
                return;
            }

            // SOLVED:
            // Show "Correct!" only if we are NOT about to trigger the win popup.
            boolean willWinNow = escapeRoom.isWon();
            if (!willWinNow) {
                JOptionPane.showMessageDialog(this, "Correct! ✅");
            }


            // OPEN + MULTI_WINDOWS: close ONLY this window.
            if (room.getMode() == Room.RoomMode.OPEN &&
                    room.getOpenDisplayStyle() == Room.OpenDisplayStyle.MULTI_WINDOWS) {
                dispose();
                return;
            }

            // Otherwise: close and open next puzzle / advance.
            dispose();

            if (room.isComplete()) {
                advanceToNextRoomIfPossible();
            } else {
                openFirstAvailablePuzzle(room, false);
            }
        }

        private void hint() {
            String hint = puzzle.useHint();
            captureProgressFromEscapeRoom(escapeRoom, progress);
            refresh();

            if (hint == null || hint.trim().isEmpty()) hint = "(No hint provided for this puzzle.)";
            JOptionPane.showMessageDialog(this, hint, "Hint", JOptionPane.INFORMATION_MESSAGE);
        }

        private void switchPuzzle() {
            dispose();
            openFirstAvailablePuzzle(room, true);
        }
    }

    // --------------------------------------------------
    // Progress Sync
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
