package escapeRoomEngine;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * PlayEscapeRoomGUI
 *
 * Player launcher screen.
 * Lets the user:
 *  - Load an escape room
 *  - Start New game
 *  - Load a save
 *  - Back to main menu
 *  - Open editor
 *
 * This launches PlayEscapeRoom (the actual gameplay engine/controller).
 */
public class PlayEscapeRoomGUI extends JFrame {

    private EscapeRoom escapeRoom;
    private String roomFilePath;

    // UI
    private JLabel lblLoadedFile;
    private JLabel lblStatus;

    private JButton btnLoadRoom;
    private JButton btnStartNew;
    private JButton btnLoadSave;
    private JButton btnOpenEditor;
    private JButton btnBackMenu;

    public PlayEscapeRoomGUI(String roomFilePath, String title) {
        super(title);

        // If you already have a no-arg constructor, call it:
        // this();
        // BUT you can’t call both super(title) and this() together.
        // So instead, move UI building into a private init() method.

        init(); // build UI here (same code you use in your no-arg constructor)

        if (roomFilePath != null && !roomFilePath.trim().isEmpty()) {
            loadEscapeRoomFromPath(roomFilePath); // only if your class has this method
        } else {
            updateButtonsEnabled();
            setStatus("No escape room loaded. Click 'Load Escape Room' to begin.");
        }
    }
    
	private void loadEscapeRoomFromPath(String roomFilePath2) {
		// TODO Auto-generated method stub
		try {
			escapeRoom = RoomFileManager.loadFromFile(roomFilePath2);
			roomFilePath = roomFilePath2;

			File file = new File(roomFilePath2);
			lblLoadedFile.setText("Loaded file: " + file.getName());
			setTitle("Escape Room Player - " + file.getName());
			setStatus("Loaded: " + escapeRoom.getTitle());
			updateButtonsEnabled();

		} catch (Exception ex) {
			setNoRoomLoadedState();
			JOptionPane.showMessageDialog(this,
					"Failed to load escape room:\n" + ex.getMessage(),
					"Load Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void init() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(480, 400);
		setLocationRelativeTo(null);

		buildUI();
		setNoRoomLoadedState();
	}

    // -------------------------
    // UI
    // -------------------------

    private void buildUI() {
        setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("Escape Room Player", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setBorder(BorderFactory.createEmptyBorder(15, 10, 5, 10));
        add(title, BorderLayout.NORTH);

        // Center panel
        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setBorder(BorderFactory.createEmptyBorder(10, 60, 10, 60));

        lblLoadedFile = new JLabel("Loaded file: (none)", SwingConstants.CENTER);
        lblLoadedFile.setFont(new Font("SansSerif", Font.PLAIN, 12));
        center.add(lblLoadedFile, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new GridLayout(5, 1, 10, 10));

        btnLoadRoom = new JButton("Load Escape Room (.txt)");
        btnStartNew = new JButton("Start New");
        btnLoadSave = new JButton("Load Save");
        btnOpenEditor = new JButton("Open Editor");
        btnBackMenu = new JButton("Back to Main Menu");

        btnLoadRoom.setToolTipText("Choose an escape room text file to play.");
        btnStartNew.setToolTipText("Start a fresh playthrough (no save loaded).");
        btnLoadSave.setToolTipText("Load a save file for this escape room.");
        btnOpenEditor.setToolTipText("Open the editor (maker) to edit/create escape rooms.");
        btnBackMenu.setToolTipText("Return to the main menu.");

        buttons.add(btnLoadRoom);
        buttons.add(btnStartNew);
        buttons.add(btnLoadSave);
        buttons.add(btnOpenEditor);
        buttons.add(btnBackMenu);

        center.add(buttons, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        // Bottom status
        lblStatus = new JLabel(" ", SwingConstants.CENTER);
        lblStatus.setBorder(BorderFactory.createEmptyBorder(5, 10, 15, 10));
        add(lblStatus, BorderLayout.SOUTH);

        // Actions
        btnLoadRoom.addActionListener(e -> onLoadRoom());
        btnStartNew.addActionListener(e -> onStartNew());
        btnLoadSave.addActionListener(e -> onLoadSave());
        btnOpenEditor.addActionListener(e -> onOpenEditor());
        btnBackMenu.addActionListener(e -> onBackToMenu());
    }

    private void setStatus(String msg) {
        lblStatus.setText(msg);
    }

    private void updateButtonsEnabled() {
        boolean hasRoomLoaded = (escapeRoom != null);
        btnStartNew.setEnabled(hasRoomLoaded);
        btnLoadSave.setEnabled(hasRoomLoaded);
    }

    private void setNoRoomLoadedState() {
        escapeRoom = null;
        roomFilePath = null;
        lblLoadedFile.setText("Loaded file: (none)");
        updateButtonsEnabled();
        setStatus("No escape room loaded. Click 'Load Escape Room' to begin.");
    }

    // -------------------------
    // Load Room
    // -------------------------

    private void onLoadRoom() {
        JFileChooser chooser = new JFileChooser(new File(System.getProperty("user.dir")));
        chooser.setDialogTitle("Select an Escape Room (.txt)");

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (file == null) return;

        try {
            escapeRoom = RoomFileManager.loadFromFile(file.getAbsolutePath());
            roomFilePath = file.getAbsolutePath();

            lblLoadedFile.setText("Loaded file: " + file.getName());
            setTitle("Escape Room Player - " + file.getName());
            setStatus("Loaded: " + escapeRoom.getTitle());
            updateButtonsEnabled();

        } catch (Exception ex) {
            setNoRoomLoadedState();
            JOptionPane.showMessageDialog(this,
                    "Failed to load escape room:\n" + ex.getMessage(),
                    "Load Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------
    // Start New / Load Save
    // -------------------------

    private void onStartNew() {
        if (escapeRoom == null) return;

        PlayerProgress progress = new PlayerProgress();

        if (escapeRoom.getRoomCount() > 0 && escapeRoom.getRoomByIndex(0) != null) {
            progress.setCurrentRoomId(escapeRoom.getRoomByIndex(0).getId());
        }

        launchGame(progress);
    }

    private void onLoadSave() {
        if (escapeRoom == null) return;

        JFileChooser chooser = new JFileChooser(new File(System.getProperty("user.dir")));
        chooser.setDialogTitle("Select a Save File (.txt)");

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File saveFile = chooser.getSelectedFile();
        if (saveFile == null) return;

        try {
            SaveManager.LoadedSave loaded = SaveManager.loadProgress(saveFile.getAbsolutePath());

            // Use progress from save
            PlayerProgress progress = loaded.getProgress();

            // If current room is missing/invalid, default to first room
            if (progress.getCurrentRoomId() == null
                    || progress.getCurrentRoomId().trim().isEmpty()
                    || escapeRoom.getRoomById(progress.getCurrentRoomId()) == null) {

                if (escapeRoom.getRoomCount() > 0 && escapeRoom.getRoomByIndex(0) != null) {
                    progress.setCurrentRoomId(escapeRoom.getRoomByIndex(0).getId());
                }
            }

            launchGame(progress);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to load save:\n" + ex.getMessage(),
                    "Load Save Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------
    // Navigation
    // -------------------------

    private void onBackToMenu() {
        SwingUtilities.invokeLater(() -> {
            new MainMenuGUI().setVisible(true);
            dispose();
        });
    }

    private void onOpenEditor() {
        SwingUtilities.invokeLater(() -> {
            new EscapeRoomMakerGUI("Escape Room Maker").setVisible(true);
            dispose();
        });
    }

    // -------------------------
    // Launch actual gameplay engine
    // -------------------------

    private void launchGame(PlayerProgress progress) {
        // This is where your real gameplay starts.
        // PlayEscapeRoom should take over and open puzzle windows.

        new PlayEscapeRoom(escapeRoom, roomFilePath, progress);
        dispose();
    }
}
