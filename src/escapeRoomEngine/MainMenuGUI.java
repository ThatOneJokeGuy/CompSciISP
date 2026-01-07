package escapeRoomEngine;

import javax.swing.*;
import java.awt.*;

/**
 * MainMenuGUI
 *
 * FIXED:
 * - "Play" no longer tries to open EscapeRoomGame (logic class, not a GUI)
 * - "Play" now opens EscapeRoomPlayerGUI (your launcher screen)
 * - "Create / Edit" opens EscapeRoomMakerGUI
 */
public class MainMenuGUI extends JFrame {

    private JButton btnPlay;
    private JButton btnCreate;
    private JButton btnExit;

    public MainMenuGUI() {
        super("Escape Room Engine");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 320);
        setLocationRelativeTo(null);

        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout());

        // ---------- Title ----------
        JLabel lblTitle = new JLabel("Escape Room Engine", SwingConstants.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
        add(lblTitle, BorderLayout.NORTH);

        // ---------- Buttons ----------
        JPanel center = new JPanel(new GridLayout(3, 1, 12, 12));
        center.setBorder(BorderFactory.createEmptyBorder(25, 70, 25, 70));

        btnPlay = new JButton("Play");
        btnCreate = new JButton("Create / Edit");
        btnExit = new JButton("Exit");

        btnPlay.setToolTipText("Go to the Player screen to load an escape room and start playing.");
        btnCreate.setToolTipText("Open the editor to create or edit an escape room.");
        btnExit.setToolTipText("Close the program.");

        center.add(btnPlay);
        center.add(btnCreate);
        center.add(btnExit);

        add(center, BorderLayout.CENTER);

        // ---------- Actions ----------
        btnPlay.addActionListener(e -> openPlayer());
        btnCreate.addActionListener(e -> openEditor());
        btnExit.addActionListener(e -> System.exit(0));
    }

    private void openPlayer() {
        SwingUtilities.invokeLater(() -> {
            // Your launcher constructor is: EscapeRoomPlayerGUI(String roomFilePath, String title)
            PlayEscapeRoomGUI player = new PlayEscapeRoomGUI(null, "Escape Room Player");
            player.setVisible(true);
            dispose();
        });
    }

    private void openEditor() {
        SwingUtilities.invokeLater(() -> {
            EscapeRoomMakerGUI maker = new EscapeRoomMakerGUI("Escape Room Maker");
            maker.setVisible(true);
            dispose();
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainMenuGUI().setVisible(true));
    }
}
