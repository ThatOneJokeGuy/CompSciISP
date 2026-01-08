package escapeRoomEngine;

import java.io.*;
import java.util.ArrayList;
import javax.swing.JOptionPane;

/**
 * SaveManager
 *
 * Saves and loads PlayerProgress to/from a simple .txt save file.
 * Uses ArrayLists only (no HashMap).
 *
 * Save file format (example):
 * SAVE_BEGIN
 * ROOM_FILE=C:\...\MyEscapeRoom.txt
 * CURRENT_ROOM=foyer
 * SOLVED=foy_p1||foy_p2||lab_p2
 * ATTEMPT=foy_p1||2
 * ATTEMPT=lab_p2||1
 * HINT=foy_p1||1
 * SAVE_END
 *
 * Notes:
 * - Each player should have ONE save file (you choose a folder/name).
 * - This class does NOT decide where the save file is located. The Player GUI does.
 */
public class SaveManager {

    /**
     * Saves the player's progress to a text file.
     *
     * @param saveFilePath path to the player's save file (example: saves/player1.txt)
     * @param roomFilePath path to the escape room file being played
     * @param progress the current player progress
     * @throws IOException if writing fails
     */
    public static void saveProgress(String saveFilePath, String roomFilePath, PlayerProgress progress) throws IOException {
        if (saveFilePath == null || saveFilePath.trim().isEmpty()) {
            throw new IOException("Save file path is empty.");
        }
        if (progress == null) {
            throw new IOException("Progress is null.");
        }
        if (roomFilePath == null) roomFilePath = "";

        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileWriter(saveFilePath));

            out.println("SAVE_BEGIN");
            out.println("ROOM_FILE=" + roomFilePath);
            out.println("CURRENT_ROOM=" + safe(progress.getCurrentRoomId()));
            out.println(progress.toSolvedLine());

            ArrayList<String> attemptLines = progress.toAttemptLines();
            for (int i = 0; i < attemptLines.size(); i++) {
                out.println(attemptLines.get(i));
            }

            ArrayList<String> hintLines = progress.toHintLines();
            for (int i = 0; i < hintLines.size(); i++) {
                out.println(hintLines.get(i));
            }

            out.println("SAVE_END");
        } finally {
            if (out != null) out.close();
        }
    }

    /**
     * Loads PlayerProgress from a save file.
     *
     * @param saveFilePath path to the player's save file
     * @return a LoadedSave object which contains:
     *         - roomFilePath (which escape room to load)
     *         - progress (the player's progress)
     * @throws IOException if reading fails or file is missing/invalid
     */
    public static LoadedSave loadProgress(String saveFilePath) throws IOException {
        if (saveFilePath == null || saveFilePath.trim().isEmpty()) {
            throw new IOException("Save file path is empty.");
        }

        File f = new File(saveFilePath);
        if (!f.exists()) {
            throw new IOException("Save file not found:\n" + saveFilePath);
        }

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(f));

            String roomFilePath = "";
            PlayerProgress progress = new PlayerProgress();

            boolean started = false;

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) continue;

                if (line.equals("SAVE_BEGIN")) {
                    started = true;
                    continue;
                }
                if (line.equals("SAVE_END")) {
                    break;
                }

                if (!started) continue; // ignore junk before SAVE_BEGIN

                if (line.startsWith("ROOM_FILE=")) {
                    roomFilePath = valueAfterEquals(line);
                } else if (line.startsWith("CURRENT_ROOM=")) {
                    progress.setCurrentRoomId(valueAfterEquals(line));
                } else if (line.startsWith("SOLVED=")) {
                    progress.loadSolvedFromLine(line);
                } else if (line.startsWith("ATTEMPT=")) {
                    progress.loadAttemptFromLine(line);
                } else if (line.startsWith("HINT=")) {
                    progress.loadHintFromLine(line);
                }
                // unknown lines are ignored (so old saves won't crash newer code)
            }

            return new LoadedSave(roomFilePath, progress);

        } finally {
            if (br != null) br.close();
        }
    }

    // -------------------------
    // Helper object (returned by loadProgress)
    // -------------------------

    public static class LoadedSave {
        private String roomFilePath;
        private PlayerProgress progress;

        public LoadedSave(String roomFilePath, PlayerProgress progress) {
            this.roomFilePath = (roomFilePath == null) ? "" : roomFilePath;
            this.progress = progress;
        }

        public String getRoomFilePath() {
            return roomFilePath;
        }

        public PlayerProgress getProgress() {
            return progress;
        }
    }

    // -------------------------
    // Helpers
    // -------------------------

    private static String valueAfterEquals(String line) {
        int eq = line.indexOf('=');
        if (eq < 0) return "";
        return line.substring(eq + 1).trim();
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    // -------------------------
    // Optional quick test (you can delete this)
    // -------------------------
    public static void main(String[] args) {
        try {
            PlayerProgress p = new PlayerProgress();
            p.setCurrentRoomId("foyer");
            p.markPuzzleSolved("foy_p1");
            p.markPuzzleSolved("foy_p2");
            p.setAttemptCount("foy_p1", 2);
            p.setHintsUsed("foy_p1", 1);

            String savePath = "test_save.txt";
            String roomPath = "The_Clockwork_Manor.txt";

            saveProgress(savePath, roomPath, p);

            LoadedSave loaded = loadProgress(savePath);
            JOptionPane.showMessageDialog(null,
                    "Loaded!\nRoom file: " + loaded.getRoomFilePath()
                            + "\nCurrent room: " + loaded.getProgress().getCurrentRoomId()
                            + "\nSolved foy_p1? " + loaded.getProgress().isPuzzleSolved("foy_p1")
                            + "\nAttempts foy_p1: " + loaded.getProgress().getAttemptCount("foy_p1")
                            + "\nHints foy_p1: " + loaded.getProgress().getHintsUsed("foy_p1"));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Test failed:\n" + e.getMessage());
        }
    }
}
