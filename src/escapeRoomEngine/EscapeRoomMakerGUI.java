package escapeRoomEngine;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;

/**
 * EscapeRoomMakerGUI
 *
 * UPDATE (as requested):
 * - Removed Structure + Win Condition from the editor:
 *   The game is always LINEAR and wins by completing the final room.
 *
 * - Room Editor now toggles settings panels:
 *   * If Room Mode = SEMI_OPEN -> show SEMI_OPEN settings panel
 *   * If Room Mode = OPEN -> show OPEN settings panel
 *   * Otherwise -> hide both
 */
public class EscapeRoomMakerGUI extends JFrame {

    private EscapeRoom escapeRoom;
    private File currentFile;
    private boolean unsavedChanges = false;

    // ---------- Header ----------
    private JTextField txtGameTitle;

    // ---------- Rooms ----------
    private DefaultListModel<String> roomsModel;
    private JList<String> roomsList;
    private JButton btnAddRoom, btnRemoveRoom;

    // ---------- Room editor ----------
    private JTextField txtRoomId, txtRoomTitle;
    private JComboBox<Room.RoomMode> cmbRoomMode;
    private JTextArea txtRoomDesc;
    private JButton btnApplyRoomEdits;

    // Settings wrapper (CardLayout)
    private JPanel pnlModeSettings;
    private CardLayout modeCards;

    // ---------- SEMI_OPEN settings ----------
    private JPanel pnlSemiSettings;
    private JComboBox<Room.SemiOpenUnlockRule> cmbSemiRule;
    private JTextField txtSolvedNeeded;
    private JTextField txtPercentNeeded;
    private JLabel lblSemiHelp;

    // ---------- OPEN settings ----------
    private JPanel pnlOpenSettings;
    private JComboBox<Room.OpenDisplayStyle> cmbOpenStyle;
    private JTextField txtPuzzlesPerWindow;
    private JLabel lblOpenHelp;

    // ---------- Puzzles ----------
    private DefaultListModel<String> puzzlesModel;
    private JList<String> puzzlesList;
    private JButton btnAddPuzzle, btnRemovePuzzle;

    // ---------- Puzzle editor ----------
    private JTextField txtPuzzleId, txtPuzzleAnswer, txtMaxAttempts;
    private JComboBox<Puzzle.PuzzleType> cmbPuzzleType;
    private JComboBox<Puzzle.ValidationMode> cmbValidation;
    private JTextArea txtPuzzlePrompt, txtHint, txtOptions;
    private JButton btnApplyPuzzleEdits;

    // ---------- Bottom ----------
    private JButton btnNew, btnOpen, btnSave, btnSaveAs, btnBackToMenu;

    public EscapeRoomMakerGUI(String ignoredTitle) {
        super("Escape Room Maker");

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (confirmDiscardChanges()) dispose();
            }
        });

        setSize(1250, 780);
        setLocationRelativeTo(null);

        buildUI();
        newEscapeRoom();
    }

    // -------------------------
    // UI BUILD
    // -------------------------

    private void buildUI() {
        setLayout(new BorderLayout(10, 10));
        add(buildHeaderPanel(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel(new GridBagLayout());
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        txtGameTitle = new JTextField(28);
        txtGameTitle.setToolTipText("Name shown to players and used in the file.");

        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0;
        header.add(new JLabel("Escape Room Title:"), gc);
        gc.gridx = 1; gc.gridy = 0; gc.weightx = 1;
        header.add(txtGameTitle, gc);

        JLabel note = new JLabel("Note: Structure is always LINEAR and win condition is always 'complete final room'.");
        note.setForeground(new Color(90, 90, 90));
        gc.gridx = 0; gc.gridy = 1; gc.gridwidth = 2; gc.weightx = 1;
        header.add(note, gc);

        return header;
    }

    private JPanel buildCenterPanel() {
        JPanel center = new JPanel(new GridLayout(1, 3, 10, 10));
        center.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        center.add(buildRoomsPanel());
        center.add(buildRoomEditorPanel());
        center.add(buildPuzzlesPanel());

        return center;
    }

    private JPanel buildRoomsPanel() {
        JPanel left = new JPanel(new BorderLayout(6, 6));
        left.setBorder(new TitledBorder("Rooms"));

        roomsModel = new DefaultListModel<>();
        roomsList = new JList<>(roomsModel);
        roomsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRoomSelected();
        });

        btnAddRoom = new JButton("Add Room");
        btnRemoveRoom = new JButton("Remove Room");

        btnAddRoom.addActionListener(e -> addRoom());
        btnRemoveRoom.addActionListener(e -> removeSelectedRoom());

        JPanel buttons = new JPanel(new GridLayout(1, 2, 6, 6));
        buttons.add(btnAddRoom);
        buttons.add(btnRemoveRoom);

        left.add(new JScrollPane(roomsList), BorderLayout.CENTER);
        left.add(buttons, BorderLayout.SOUTH);

        return left;
    }

    private JPanel buildRoomEditorPanel() {
        JPanel mid = new JPanel(new BorderLayout(8, 8));
        mid.setBorder(new TitledBorder("Room Editor"));

        // Top: basic room fields
        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        txtRoomId = new JTextField(18);
        txtRoomTitle = new JTextField(18);

        txtRoomId.setToolTipText("Unique internal ID used by saves. Locked after creation.");
        txtRoomTitle.setToolTipText("Name shown to the player.");

        cmbRoomMode = new JComboBox<>(Room.RoomMode.values());
        cmbRoomMode.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Room.RoomMode) setText(roomModeLabel((Room.RoomMode) value));
                return this;
            }
        });
        cmbRoomMode.setToolTipText("How puzzles unlock inside the room.");
        cmbRoomMode.addActionListener(e -> updateModeSettingsPanel());

        int row = 0;

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        top.add(new JLabel("Room ID:"), gc);
        gc.gridx = 1; gc.gridy = row++; gc.weightx = 1;
        top.add(txtRoomId, gc);

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        top.add(new JLabel("Room Title:"), gc);
        gc.gridx = 1; gc.gridy = row++; gc.weightx = 1;
        top.add(txtRoomTitle, gc);

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        top.add(new JLabel("Room Mode:"), gc);
        gc.gridx = 1; gc.gridy = row++; gc.weightx = 1;
        top.add(cmbRoomMode, gc);

        mid.add(top, BorderLayout.NORTH);

        // Middle: mode-specific settings (cards)
        modeCards = new CardLayout();
        pnlModeSettings = new JPanel(modeCards);

        pnlSemiSettings = buildSemiSettingsPanel();
        pnlOpenSettings = buildOpenSettingsPanel();
        JPanel blank = new JPanel();
        blank.add(new JLabel("No extra settings for this Room Mode."));

        pnlModeSettings.add(blank, "BLANK");
        pnlModeSettings.add(pnlSemiSettings, "SEMI");
        pnlModeSettings.add(pnlOpenSettings, "OPEN");

        mid.add(pnlModeSettings, BorderLayout.CENTER);

        // Bottom: description + apply
        JPanel bottom = new JPanel(new BorderLayout(6, 6));
        txtRoomDesc = new JTextArea(8, 24);
        txtRoomDesc.setLineWrap(true);
        txtRoomDesc.setWrapStyleWord(true);

        bottom.setBorder(new TitledBorder("Room Description (shown to player)"));
        bottom.add(new JScrollPane(txtRoomDesc), BorderLayout.CENTER);

        btnApplyRoomEdits = new JButton("Apply Room Changes");
        btnApplyRoomEdits.addActionListener(e -> applyRoomEdits());
        bottom.add(btnApplyRoomEdits, BorderLayout.SOUTH);

        mid.add(bottom, BorderLayout.SOUTH);

        updateModeSettingsPanel();
        return mid;
    }

    private JPanel buildSemiSettingsPanel() {
        JPanel semi = new JPanel(new GridBagLayout());
        semi.setBorder(new TitledBorder("SEMI_OPEN Settings (only affects last puzzle)"));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        cmbSemiRule = new JComboBox<>(Room.SemiOpenUnlockRule.values());
        cmbSemiRule.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Room.SemiOpenUnlockRule) setText(semiRuleLabel((Room.SemiOpenUnlockRule) value));
                return this;
            }
        });
        cmbSemiRule.addActionListener(e -> updateSemiFieldsEnabled());

        txtSolvedNeeded = new JTextField(8);
        txtPercentNeeded = new JTextField(8);

        lblSemiHelp = new JLabel(" ");
        lblSemiHelp.setForeground(new Color(80, 80, 80));

        int row = 0;

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        semi.add(new JLabel("Unlock last puzzle when:"), gc);
        gc.gridx = 1; gc.gridy = row++; gc.weightx = 1;
        semi.add(cmbSemiRule, gc);

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        semi.add(new JLabel("Solved puzzles needed:"), gc);
        gc.gridx = 1; gc.gridy = row++; gc.weightx = 1;
        semi.add(txtSolvedNeeded, gc);

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        semi.add(new JLabel("Percent solved needed:"), gc);
        gc.gridx = 1; gc.gridy = row++; gc.weightx = 1;
        semi.add(txtPercentNeeded, gc);

        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2; gc.weightx = 1;
        semi.add(lblSemiHelp, gc);

        updateSemiFieldsEnabled();
        return semi;
    }

    private JPanel buildOpenSettingsPanel() {
        JPanel open = new JPanel(new GridBagLayout());
        open.setBorder(new TitledBorder("OPEN Settings"));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        cmbOpenStyle = new JComboBox<>(Room.OpenDisplayStyle.values());
        cmbOpenStyle.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Room.OpenDisplayStyle) setText(openStyleLabel((Room.OpenDisplayStyle) value));
                return this;
            }
        });

        txtPuzzlesPerWindow = new JTextField(8);
        txtPuzzlesPerWindow.setToolTipText("Leave as 1 for now. This is for future grouping if you want.");

        lblOpenHelp = new JLabel(" ");
        lblOpenHelp.setForeground(new Color(80, 80, 80));

        cmbOpenStyle.addActionListener(e -> updateOpenHelp());

        int row = 0;

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        open.add(new JLabel("Open display style:"), gc);
        gc.gridx = 1; gc.gridy = row++; gc.weightx = 1;
        open.add(cmbOpenStyle, gc);

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        open.add(new JLabel("Puzzles per window:"), gc);
        gc.gridx = 1; gc.gridy = row++; gc.weightx = 1;
        open.add(txtPuzzlesPerWindow, gc);

        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2; gc.weightx = 1;
        open.add(lblOpenHelp, gc);

        updateOpenHelp();
        return open;
    }

    private void updateOpenHelp() {
        Room.OpenDisplayStyle s = (Room.OpenDisplayStyle) cmbOpenStyle.getSelectedItem();
        if (s == Room.OpenDisplayStyle.SWITCHER) {
            lblOpenHelp.setText("Meaning: One puzzle window at a time. Switch closes this window and opens another.");
        } else {
            lblOpenHelp.setText("Meaning: Many puzzle windows open at once (one per available puzzle).");
        }
    }

    private void updateSemiFieldsEnabled() {
        Room.SemiOpenUnlockRule rule = (Room.SemiOpenUnlockRule) cmbSemiRule.getSelectedItem();

        boolean needsSolved = (rule == Room.SemiOpenUnlockRule.LAST_UNLOCK_AFTER_N_SOLVED);
        boolean needsPercent = (rule == Room.SemiOpenUnlockRule.LAST_UNLOCK_AFTER_PERCENT_SOLVED);

        txtSolvedNeeded.setEnabled(needsSolved);
        txtSolvedNeeded.setEditable(needsSolved);
        if (!needsSolved) txtSolvedNeeded.setText("");

        txtPercentNeeded.setEnabled(needsPercent);
        txtPercentNeeded.setEditable(needsPercent);
        if (!needsPercent) txtPercentNeeded.setText("");

        if (rule == Room.SemiOpenUnlockRule.LAST_UNLOCK_AFTER_N_SOLVED) {
            if (txtSolvedNeeded.getText().trim().isEmpty()) txtSolvedNeeded.setText("2");
            lblSemiHelp.setText("The LAST puzzle unlocks after this many puzzles are solved.");
        } else if (rule == Room.SemiOpenUnlockRule.LAST_UNLOCK_AFTER_PERCENT_SOLVED) {
            if (txtPercentNeeded.getText().trim().isEmpty()) txtPercentNeeded.setText("50");
            lblSemiHelp.setText("The LAST puzzle unlocks after this % of puzzles are solved (rounded up).");
        } else {
            lblSemiHelp.setText("The LAST puzzle unlocks after every other puzzle is solved.");
        }
    }

    private void updateModeSettingsPanel() {
        Room.RoomMode mode = (Room.RoomMode) cmbRoomMode.getSelectedItem();
        if (mode == Room.RoomMode.SEMI_OPEN) {
            modeCards.show(pnlModeSettings, "SEMI");
            updateSemiFieldsEnabled();
        } else if (mode == Room.RoomMode.OPEN) {
            modeCards.show(pnlModeSettings, "OPEN");
            updateOpenHelp();
        } else {
            modeCards.show(pnlModeSettings, "BLANK");
        }
    }

    private JPanel buildPuzzlesPanel() {
        JPanel right = new JPanel(new BorderLayout(6, 6));
        right.setBorder(new TitledBorder("Puzzles"));

        JPanel top = new JPanel(new BorderLayout(6, 6));

        puzzlesModel = new DefaultListModel<>();
        puzzlesList = new JList<>(puzzlesModel);
        puzzlesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        puzzlesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onPuzzleSelected();
        });

        btnAddPuzzle = new JButton("Add Puzzle");
        btnRemovePuzzle = new JButton("Remove Puzzle");

        btnAddPuzzle.addActionListener(e -> addPuzzle());
        btnRemovePuzzle.addActionListener(e -> removeSelectedPuzzle());

        JPanel buttons = new JPanel(new GridLayout(1, 2, 6, 6));
        buttons.add(btnAddPuzzle);
        buttons.add(btnRemovePuzzle);

        top.add(new JScrollPane(puzzlesList), BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);

        right.add(top, BorderLayout.CENTER);
        right.add(buildPuzzleEditorPanel(), BorderLayout.SOUTH);

        return right;
    }

    private JPanel buildPuzzleEditorPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Puzzle Editor"));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        txtPuzzleId = new JTextField(16);

        cmbPuzzleType = new JComboBox<>(Puzzle.PuzzleType.values());
        cmbPuzzleType.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Puzzle.PuzzleType) setText(puzzleTypeLabel((Puzzle.PuzzleType) value));
                return this;
            }
        });
        cmbPuzzleType.addActionListener(e -> updateOptionsEnabled());

        txtPuzzlePrompt = new JTextArea(3, 18);
        txtPuzzlePrompt.setLineWrap(true);
        txtPuzzlePrompt.setWrapStyleWord(true);

        txtPuzzleAnswer = new JTextField(16);

        cmbValidation = new JComboBox<>(Puzzle.ValidationMode.values());

        txtMaxAttempts = new JTextField(6);
        txtHint = new JTextArea(2, 18);
        txtHint.setLineWrap(true);
        txtHint.setWrapStyleWord(true);

        txtOptions = new JTextArea(3, 18);
        txtOptions.setLineWrap(true);
        txtOptions.setWrapStyleWord(true);

        btnApplyPuzzleEdits = new JButton("Apply Puzzle Changes");
        btnApplyPuzzleEdits.addActionListener(e -> applyPuzzleEdits());

        int row = 0;

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        panel.add(new JLabel("Puzzle ID:"), gc);
        gc.gridx = 1; gc.gridy = row++; gc.weightx = 1;
        panel.add(txtPuzzleId, gc);

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        panel.add(new JLabel("Type:"), gc);
        gc.gridx = 1; gc.gridy = row++; gc.weightx = 1;
        panel.add(cmbPuzzleType, gc);

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        gc.anchor = GridBagConstraints.NORTH;
        panel.add(new JLabel("Prompt:"), gc);
        gc.gridx = 1; gc.gridy = row++; gc.weightx = 1;
        gc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(txtPuzzlePrompt), gc);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.CENTER;

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        panel.add(new JLabel("Answer:"), gc);
        gc.gridx = 1; gc.gridy = row++; gc.weightx = 1;
        panel.add(txtPuzzleAnswer, gc);

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        panel.add(new JLabel("Validation:"), gc);
        gc.gridx = 1; gc.gridy = row++; gc.weightx = 1;
        panel.add(cmbValidation, gc);

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        panel.add(new JLabel("Max Attempts:"), gc);
        gc.gridx = 1; gc.gridy = row++; gc.weightx = 1;
        panel.add(txtMaxAttempts, gc);

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        gc.anchor = GridBagConstraints.NORTH;
        panel.add(new JLabel("Hint:"), gc);
        gc.gridx = 1; gc.gridy = row++; gc.weightx = 1;
        gc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(txtHint), gc);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.CENTER;

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        gc.anchor = GridBagConstraints.NORTH;
        panel.add(new JLabel("Options (MC):"), gc);
        gc.gridx = 1; gc.gridy = row++; gc.weightx = 1;
        gc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(txtOptions), gc);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.CENTER;

        gc.gridx = 1; gc.gridy = row++; gc.weightx = 1;
        panel.add(btnApplyPuzzleEdits, gc);

        updateOptionsEnabled();
        return panel;
    }

    private JPanel buildBottomPanel() {
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        btnBackToMenu = new JButton("Back to Main Menu");
        btnNew = new JButton("New");
        btnOpen = new JButton("Open...");
        btnSave = new JButton("Save");
        btnSaveAs = new JButton("Save As...");

        btnBackToMenu.addActionListener(e -> {
            if (!confirmDiscardChanges()) return;
            SwingUtilities.invokeLater(() -> {
                MainMenuGUI menu = new MainMenuGUI();
                menu.setVisible(true);
                dispose();
            });
        });

        btnNew.addActionListener(e -> newEscapeRoom());
        btnOpen.addActionListener(e -> openEscapeRoom());
        btnSave.addActionListener(e -> saveEscapeRoom(false));
        btnSaveAs.addActionListener(e -> saveEscapeRoom(true));

        bottom.add(btnBackToMenu);
        bottom.add(btnNew);
        bottom.add(btnOpen);
        bottom.add(btnSave);
        bottom.add(btnSaveAs);

        return bottom;
    }

    // -------------------------
    // Labels
    // -------------------------

    private String roomModeLabel(Room.RoomMode m) {
        if (m == null) return "";
        switch (m) {
            case LINEAR: return "LINEAR - Puzzles in order";
            case SEMI_OPEN: return "SEMI_OPEN - Last puzzle locks";
            case OPEN: return "OPEN - All puzzles available";
            default: return m.toString();
        }
    }

    private String semiRuleLabel(Room.SemiOpenUnlockRule r) {
        if (r == null) return "";
        switch (r) {
            case LAST_UNLOCK_AFTER_N_SOLVED:
                return "AFTER N SOLVED - Use 'Solved puzzles needed'";
            case LAST_UNLOCK_AFTER_PERCENT_SOLVED:
                return "AFTER % SOLVED - Use 'Percent solved needed'";
            case LAST_UNLOCK_AFTER_ALL_OTHERS_SOLVED:
                return "AFTER ALL OTHERS - No number needed";
            default:
                return r.toString();
        }
    }

    private String openStyleLabel(Room.OpenDisplayStyle s) {
        if (s == null) return "";
        switch (s) {
            case SWITCHER:
                return "SWITCHER - One puzzle at a time";
            case MULTI_WINDOWS:
                return "MULTI_WINDOWS - Many puzzle windows at once";
            default:
                return s.toString();
        }
    }

    private String puzzleTypeLabel(Puzzle.PuzzleType t) {
        if (t == null) return "";
        switch (t) {
            case TEXT: return "TEXT - Type answer";
            case MULTIPLE_CHOICE: return "MC - Choose option";
            case TRUE_FALSE: return "T/F - True or false";
            case MATH: return "MATH - Numeric answer";
            default: return t.toString();
        }
    }

    // -------------------------
    // Actions
    // -------------------------

    private void newEscapeRoom() {
        escapeRoom = new EscapeRoom("New Escape Room");
        currentFile = null;

        txtGameTitle.setText(escapeRoom.getTitle());

        roomsModel.clear();
        puzzlesModel.clear();
        clearRoomEditor();
        clearPuzzleEditor();

        unsavedChanges = true;
        setTitle("Escape Room Maker - (Unsaved)");
    }

    private void openEscapeRoom() {
        if (!confirmDiscardChanges()) return;

        File f = chooseFile("Open escape room .txt");
        if (f == null) return;

        try {
            escapeRoom = RoomFileManager.loadFromFile(f.getAbsolutePath());
            currentFile = f;

            txtGameTitle.setText(escapeRoom.getTitle());

            refreshRoomsList();
            puzzlesModel.clear();
            clearRoomEditor();
            clearPuzzleEditor();

            unsavedChanges = false;
            setTitle("Escape Room Maker - " + f.getName());
        } catch (Exception ex) {
            showError("Failed to open file:\n" + ex.getMessage());
        }
    }

    private void saveEscapeRoom(boolean forceSaveAs) {
        if (escapeRoom == null) return;

        applyHeaderToEscapeRoom();

        File f = currentFile;
        if (forceSaveAs || f == null) {
            f = chooseSaveFile("Save escape room as .txt");
            if (f == null) return;
        }

        try {
            RoomFileManager.saveToFile(f.getAbsolutePath(), escapeRoom);
            currentFile = f;
            unsavedChanges = false;
            setTitle("Escape Room Maker - " + f.getName());
            JOptionPane.showMessageDialog(this, "Saved successfully.");
        } catch (Exception ex) {
            showError("Failed to save:\n" + ex.getMessage());
        }
    }

    private void addRoom() {
        if (escapeRoom == null) return;

        String id = JOptionPane.showInputDialog(this, "Enter Room ID (unique):",
                "room" + (escapeRoom.getRoomCount() + 1));
        if (id == null) return;
        id = id.trim();
        if (id.isEmpty()) {
            showError("Room ID cannot be empty.");
            return;
        }
        if (escapeRoom.getRoomById(id) != null) {
            showError("That Room ID already exists.");
            return;
        }

        Room room = new Room(id,
                "Room " + (escapeRoom.getRoomCount() + 1),
                "Describe the room here...",
                Room.RoomMode.SEMI_OPEN);

        room.setSemiOpenRule(Room.SemiOpenUnlockRule.LAST_UNLOCK_AFTER_PERCENT_SOLVED);
        room.setSemiOpenPercent(50);
        room.setSemiOpenN(2);

        room.setOpenDisplayStyle(Room.OpenDisplayStyle.SWITCHER);
        room.setPuzzlesPerWindow(1);

        escapeRoom.addRoom(room);

        refreshRoomsList();
        roomsList.setSelectedIndex(escapeRoom.getRoomCount() - 1);
        unsavedChanges = true;
    }

    private void removeSelectedRoom() {
        int idx = roomsList.getSelectedIndex();
        if (idx < 0) return;

        int ok = JOptionPane.showConfirmDialog(this,
                "Remove selected room and its puzzles?",
                "Confirm",
                JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        EscapeRoom rebuilt = new EscapeRoom(txtGameTitle.getText().trim());

        for (int i = 0; i < escapeRoom.getRoomCount(); i++) {
            if (i == idx) continue;
            rebuilt.addRoom(escapeRoom.getRoomByIndex(i));
        }

        // keep existing links (even though gameplay ignores them)
        for (EscapeRoom.RoomLink link : escapeRoom.getLinksCopy()) {
            rebuilt.addLink(link.getFromRoomId(), link.getToRoomId(), link.getRequiredRoomId());
        }

        escapeRoom = rebuilt;

        refreshRoomsList();
        puzzlesModel.clear();
        clearRoomEditor();
        clearPuzzleEditor();
        unsavedChanges = true;
    }

    private void onRoomSelected() {
        int idx = roomsList.getSelectedIndex();
        if (idx < 0 || escapeRoom == null) {
            puzzlesModel.clear();
            clearRoomEditor();
            clearPuzzleEditor();
            return;
        }

        Room room = escapeRoom.getRoomByIndex(idx);
        if (room == null) return;

        txtRoomId.setText(room.getId());
        txtRoomId.setEditable(false);

        txtRoomTitle.setText(room.getTitle());
        cmbRoomMode.setSelectedItem(room.getMode());

        // semi-open
        cmbSemiRule.setSelectedItem(room.getSemiOpenRule());
        txtSolvedNeeded.setText(String.valueOf(room.getSemiOpenN()));
        txtPercentNeeded.setText(String.valueOf(room.getSemiOpenPercent()));

        // open
        cmbOpenStyle.setSelectedItem(room.getOpenDisplayStyle());
        txtPuzzlesPerWindow.setText(String.valueOf(room.getPuzzlesPerWindow()));

        updateModeSettingsPanel();

        txtRoomDesc.setText(room.getDescription());

        refreshPuzzlesList(room);
        clearPuzzleEditor();
    }

    private void applyRoomEdits() {
        int idx = roomsList.getSelectedIndex();
        if (idx < 0 || escapeRoom == null) return;

        Room room = escapeRoom.getRoomByIndex(idx);
        if (room == null) return;

        room.setTitle(txtRoomTitle.getText().trim());
        room.setMode((Room.RoomMode) cmbRoomMode.getSelectedItem());
        room.setDescription(txtRoomDesc.getText().trim());

        if (room.getMode() == Room.RoomMode.SEMI_OPEN) {
            room.setSemiOpenRule((Room.SemiOpenUnlockRule) cmbSemiRule.getSelectedItem());

            if (txtSolvedNeeded.isEnabled()) {
                int n = room.getSemiOpenN();
                try { n = Integer.parseInt(txtSolvedNeeded.getText().trim()); } catch (Exception ignored) {}
                if (n < 0) n = 0;
                room.setSemiOpenN(n);
            }

            if (txtPercentNeeded.isEnabled()) {
                int pct = room.getSemiOpenPercent();
                try { pct = Integer.parseInt(txtPercentNeeded.getText().trim()); } catch (Exception ignored) {}
                if (pct < 0) pct = 0;
                if (pct > 100) pct = 100;
                room.setSemiOpenPercent(pct);
            }
        }

        if (room.getMode() == Room.RoomMode.OPEN) {
            room.setOpenDisplayStyle((Room.OpenDisplayStyle) cmbOpenStyle.getSelectedItem());

            int ppw = room.getPuzzlesPerWindow();
            try { ppw = Integer.parseInt(txtPuzzlesPerWindow.getText().trim()); } catch (Exception ignored) {}
            if (ppw < 1) ppw = 1;
            room.setPuzzlesPerWindow(ppw);
        }

        refreshRoomsList();
        roomsList.setSelectedIndex(idx);
        unsavedChanges = true;
    }

    private void addPuzzle() {
        Room room = getSelectedRoom();
        if (room == null) {
            showError("Select a room first.");
            return;
        }

        String id = JOptionPane.showInputDialog(this,
                "Enter Puzzle ID (unique in whole escape room):",
                "p" + (room.getPuzzleCount() + 1));
        if (id == null) return;
        id = id.trim();
        if (id.isEmpty()) {
            showError("Puzzle ID cannot be empty.");
            return;
        }
        if (puzzleIdExists(id)) {
            showError("That Puzzle ID already exists somewhere in this escape room.");
            return;
        }

        Puzzle puzzle = new Puzzle(
                id,
                Puzzle.PuzzleType.TEXT,
                "Enter the puzzle prompt...",
                "answer",
                Puzzle.ValidationMode.FLEXIBLE,
                "hint",
                3
        );

        room.addPuzzle(puzzle);
        refreshPuzzlesList(room);
        puzzlesList.setSelectedIndex(room.getPuzzleCount() - 1);
        unsavedChanges = true;
    }

    private void removeSelectedPuzzle() {
        Room room = getSelectedRoom();
        if (room == null) return;

        int pIdx = puzzlesList.getSelectedIndex();
        if (pIdx < 0) return;

        int ok = JOptionPane.showConfirmDialog(this, "Remove selected puzzle?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        Room rebuilt = new Room(room.getId(), room.getTitle(), room.getDescription(), room.getMode());
        rebuilt.setSemiOpenRule(room.getSemiOpenRule());
        rebuilt.setSemiOpenN(room.getSemiOpenN());
        rebuilt.setSemiOpenPercent(room.getSemiOpenPercent());
        rebuilt.setOpenDisplayStyle(room.getOpenDisplayStyle());
        rebuilt.setPuzzlesPerWindow(room.getPuzzlesPerWindow());

        for (int i = 0; i < room.getPuzzleCount(); i++) {
            if (i == pIdx) continue;
            rebuilt.addPuzzle(room.getPuzzle(i));
        }

        int roomIdx = roomsList.getSelectedIndex();
        replaceRoomAtIndex(roomIdx, rebuilt);

        refreshRoomsList();
        roomsList.setSelectedIndex(roomIdx);
        refreshPuzzlesList(rebuilt);
        clearPuzzleEditor();
        unsavedChanges = true;
    }

    private void onPuzzleSelected() {
        Room room = getSelectedRoom();
        if (room == null) {
            clearPuzzleEditor();
            return;
        }

        int pIdx = puzzlesList.getSelectedIndex();
        if (pIdx < 0) {
            clearPuzzleEditor();
            return;
        }

        Puzzle puzzle = room.getPuzzle(pIdx);
        if (puzzle == null) return;

        txtPuzzleId.setText(puzzle.getId());
        txtPuzzleId.setEditable(false);

        cmbPuzzleType.setSelectedItem(puzzle.getType());
        txtPuzzlePrompt.setText(puzzle.getPrompt());
        txtPuzzleAnswer.setText(puzzle.getAnswerRaw());
        cmbValidation.setSelectedItem(puzzle.getValidationMode());
        txtMaxAttempts.setText(String.valueOf(puzzle.getMaxAttempts()));
        txtHint.setText(puzzle.getHint());

        StringBuilder sb = new StringBuilder();
        for (String opt : puzzle.getOptions()) sb.append(opt).append("\n");
        txtOptions.setText(sb.toString());

        updateOptionsEnabled();
    }

    private void applyPuzzleEdits() {
        Room room = getSelectedRoom();
        if (room == null) return;

        int pIdx = puzzlesList.getSelectedIndex();
        if (pIdx < 0) {
            showError("Select a puzzle first.");
            return;
        }

        String id = txtPuzzleId.getText().trim();
        Puzzle.PuzzleType type = (Puzzle.PuzzleType) cmbPuzzleType.getSelectedItem();
        String prompt = txtPuzzlePrompt.getText().trim();
        String answer = txtPuzzleAnswer.getText();
        Puzzle.ValidationMode validation = (Puzzle.ValidationMode) cmbValidation.getSelectedItem();
        String hint = txtHint.getText().trim();

        int maxAttempts = 1;
        try { maxAttempts = Integer.parseInt(txtMaxAttempts.getText().trim()); } catch (Exception ignored) {}
        if (maxAttempts < 1) maxAttempts = 1;

        Puzzle newPuzzle;
        if (type == Puzzle.PuzzleType.MULTIPLE_CHOICE) {
            ArrayList<String> opts = parseOptions(txtOptions.getText());
            if (opts.size() < 2) {
                showError("MULTIPLE_CHOICE needs at least 2 options (one per line).");
                return;
            }
            newPuzzle = new Puzzle(id, prompt, opts, answer, validation, hint, maxAttempts);
        } else {
            newPuzzle = new Puzzle(id, type, prompt, answer, validation, hint, maxAttempts);
        }

        Room rebuilt = new Room(room.getId(), room.getTitle(), room.getDescription(), room.getMode());
        rebuilt.setSemiOpenRule(room.getSemiOpenRule());
        rebuilt.setSemiOpenN(room.getSemiOpenN());
        rebuilt.setSemiOpenPercent(room.getSemiOpenPercent());
        rebuilt.setOpenDisplayStyle(room.getOpenDisplayStyle());
        rebuilt.setPuzzlesPerWindow(room.getPuzzlesPerWindow());

        for (int i = 0; i < room.getPuzzleCount(); i++) {
            if (i == pIdx) rebuilt.addPuzzle(newPuzzle);
            else rebuilt.addPuzzle(room.getPuzzle(i));
        }

        int roomIdx = roomsList.getSelectedIndex();
        replaceRoomAtIndex(roomIdx, rebuilt);

        refreshRoomsList();
        roomsList.setSelectedIndex(roomIdx);
        refreshPuzzlesList(rebuilt);
        puzzlesList.setSelectedIndex(pIdx);

        unsavedChanges = true;
    }

    private void applyHeaderToEscapeRoom() {
        if (escapeRoom == null) return;

        EscapeRoom rebuilt = new EscapeRoom(txtGameTitle.getText().trim());

        for (int i = 0; i < escapeRoom.getRoomCount(); i++) rebuilt.addRoom(escapeRoom.getRoomByIndex(i));
        for (EscapeRoom.RoomLink link : escapeRoom.getLinksCopy()) {
            rebuilt.addLink(link.getFromRoomId(), link.getToRoomId(), link.getRequiredRoomId());
        }

        escapeRoom = rebuilt;
        unsavedChanges = true;
    }

    // -------------------------
    // Utilities
    // -------------------------

    private void refreshRoomsList() {
        roomsModel.clear();
        if (escapeRoom == null) return;
        for (int i = 0; i < escapeRoom.getRoomCount(); i++) {
            Room r = escapeRoom.getRoomByIndex(i);
            roomsModel.addElement(r.getId() + " - " + r.getTitle());
        }
    }

    private void refreshPuzzlesList(Room room) {
        puzzlesModel.clear();
        if (room == null) return;
        for (int i = 0; i < room.getPuzzleCount(); i++) {
            Puzzle p = room.getPuzzle(i);
            puzzlesModel.addElement(p.getId() + " (" + p.getType() + ")");
        }
    }

    private Room getSelectedRoom() {
        if (escapeRoom == null) return null;
        int idx = roomsList.getSelectedIndex();
        if (idx < 0) return null;
        return escapeRoom.getRoomByIndex(idx);
    }

    private boolean puzzleIdExists(String puzzleId) {
        if (escapeRoom == null || puzzleId == null) return false;
        for (int r = 0; r < escapeRoom.getRoomCount(); r++) {
            Room room = escapeRoom.getRoomByIndex(r);
            if (room == null) continue;
            for (int p = 0; p < room.getPuzzleCount(); p++) {
                Puzzle puz = room.getPuzzle(p);
                if (puz != null && puzzleId.equals(puz.getId())) return true;
            }
        }
        return false;
    }

    private void replaceRoomAtIndex(int index, Room replacement) {
        if (escapeRoom == null || replacement == null) return;

        EscapeRoom rebuilt = new EscapeRoom(txtGameTitle.getText().trim());

        for (int i = 0; i < escapeRoom.getRoomCount(); i++) {
            if (i == index) rebuilt.addRoom(replacement);
            else rebuilt.addRoom(escapeRoom.getRoomByIndex(i));
        }
        for (EscapeRoom.RoomLink link : escapeRoom.getLinksCopy()) {
            rebuilt.addLink(link.getFromRoomId(), link.getToRoomId(), link.getRequiredRoomId());
        }
        escapeRoom = rebuilt;
    }

    private void clearRoomEditor() {
        txtRoomId.setText("");
        txtRoomId.setEditable(true);
        txtRoomTitle.setText("");
        cmbRoomMode.setSelectedItem(Room.RoomMode.SEMI_OPEN);

        cmbSemiRule.setSelectedItem(Room.SemiOpenUnlockRule.LAST_UNLOCK_AFTER_PERCENT_SOLVED);
        txtSolvedNeeded.setText("2");
        txtPercentNeeded.setText("50");

        cmbOpenStyle.setSelectedItem(Room.OpenDisplayStyle.SWITCHER);
        txtPuzzlesPerWindow.setText("1");

        txtRoomDesc.setText("");
        updateModeSettingsPanel();
    }

    private void clearPuzzleEditor() {
        txtPuzzleId.setText("");
        txtPuzzleId.setEditable(true);
        cmbPuzzleType.setSelectedItem(Puzzle.PuzzleType.TEXT);
        txtPuzzlePrompt.setText("");
        txtPuzzleAnswer.setText("");
        cmbValidation.setSelectedItem(Puzzle.ValidationMode.FLEXIBLE);
        txtMaxAttempts.setText("3");
        txtHint.setText("");
        txtOptions.setText("");
        updateOptionsEnabled();
    }

    private void updateOptionsEnabled() {
        Puzzle.PuzzleType type = (Puzzle.PuzzleType) cmbPuzzleType.getSelectedItem();
        boolean isMC = (type == Puzzle.PuzzleType.MULTIPLE_CHOICE);
        txtOptions.setEnabled(isMC);
        txtOptions.setEditable(isMC);
    }

    private ArrayList<String> parseOptions(String raw) {
        ArrayList<String> opts = new ArrayList<>();
        if (raw == null) return opts;
        String[] lines = raw.split("\n");
        for (String s : lines) {
            String t = s.trim();
            if (!t.isEmpty()) opts.add(t);
        }
        return opts;
    }

    private File chooseFile(String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return null;
        return chooser.getSelectedFile();
    }

    private File chooseSaveFile(String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return null;

        File f = chooser.getSelectedFile();
        if (f == null) return null;
        if (!f.getName().toLowerCase().endsWith(".txt")) f = new File(f.getAbsolutePath() + ".txt");
        return f;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private boolean confirmDiscardChanges() {
        if (!unsavedChanges) return true;
        int choice = JOptionPane.showConfirmDialog(
                this,
                "You have unsaved changes.\nLeave anyway?",
                "Unsaved Changes",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        return choice == JOptionPane.YES_OPTION;
    }

    // -------------------------
    // RUN
    // -------------------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EscapeRoomMakerGUI gui = new EscapeRoomMakerGUI("Escape Room Maker");
            gui.setVisible(true);
        });
    }
}
