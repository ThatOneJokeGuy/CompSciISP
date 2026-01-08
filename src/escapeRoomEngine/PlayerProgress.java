package escapeRoomEngine;

import java.util.ArrayList;

public class PlayerProgress {

    private String currentRoomId = "";

    private ArrayList<String> solvedPuzzleKeys = new ArrayList<>();

    // stored as: key||count (because you're avoiding HashMap)
    private ArrayList<String> attemptLines = new ArrayList<>();
    private ArrayList<String> hintLines = new ArrayList<>();

    // -------------------------
    // Key helper (ROOM + PUZZLE)
    // -------------------------

    private String key(String roomId, String puzzleId) {
        if (roomId == null) roomId = "";
        if (puzzleId == null) puzzleId = "";
        return roomId.trim() + "::" + puzzleId.trim();
    }

    // -------------------------
    // Current room
    // -------------------------

    public String getCurrentRoomId() {
        return currentRoomId;
    }

    public void setCurrentRoomId(String id) {
        currentRoomId = (id == null) ? "" : id;
    }

    // -------------------------
    // SOLVED (NEW: room-aware)
    // -------------------------

    public void markPuzzleSolved(String roomId, String puzzleId) {
        String k = key(roomId, puzzleId);
        if (!solvedPuzzleKeys.contains(k)) solvedPuzzleKeys.add(k);
    }

    public boolean isPuzzleSolved(String roomId, String puzzleId) {
        return solvedPuzzleKeys.contains(key(roomId, puzzleId));
    }

    // Backwards-compatible (old calls)
    public void markPuzzleSolved(String puzzleId) {
        markPuzzleSolved("", puzzleId);
    }

    public boolean isPuzzleSolved(String puzzleId) {
        return isPuzzleSolved("", puzzleId);
    }

    // -------------------------
    // ATTEMPTS (NEW: room-aware)
    // -------------------------

    public void setAttemptCount(String roomId, String puzzleId, int count) {
        String k = key(roomId, puzzleId);
        removeKeyLine(attemptLines, k);
        attemptLines.add(k + "||" + count);
    }

    public int getAttemptCount(String roomId, String puzzleId) {
        String k = key(roomId, puzzleId);
        return getKeyCount(attemptLines, k);
    }

    // Backwards-compatible
    public void setAttemptCount(String puzzleId, int count) {
        setAttemptCount("", puzzleId, count);
    }

    public int getAttemptCount(String puzzleId) {
        return getAttemptCount("", puzzleId);
    }

    // -------------------------
    // HINTS (NEW: room-aware)
    // -------------------------

    public void setHintsUsed(String roomId, String puzzleId, int count) {
        String k = key(roomId, puzzleId);
        removeKeyLine(hintLines, k);
        hintLines.add(k + "||" + count);
    }

    public int getHintsUsed(String roomId, String puzzleId) {
        String k = key(roomId, puzzleId);
        return getKeyCount(hintLines, k);
    }

    // Backwards-compatible
    public void setHintsUsed(String puzzleId, int count) {
        setHintsUsed("", puzzleId, count);
    }

    public int getHintsUsed(String puzzleId) {
        return getHintsUsed("", puzzleId);
    }

    // -------------------------
    // Save format helpers
    // -------------------------

    public String toSolvedLine() {
        StringBuilder sb = new StringBuilder("SOLVED=");
        for (int i = 0; i < solvedPuzzleKeys.size(); i++) {
            if (i > 0) sb.append("||");
            sb.append(solvedPuzzleKeys.get(i));
        }
        return sb.toString();
    }

    public ArrayList<String> toAttemptLines() {
        ArrayList<String> out = new ArrayList<>();
        for (String s : attemptLines) out.add("ATTEMPT=" + s);
        return out;
    }

    public ArrayList<String> toHintLines() {
        ArrayList<String> out = new ArrayList<>();
        for (String s : hintLines) out.add("HINT=" + s);
        return out;
    }

    public void loadSolvedFromLine(String line) {
        solvedPuzzleKeys.clear();
        if (line == null) return;
        int eq = line.indexOf('=');
        if (eq < 0) return;
        String raw = line.substring(eq + 1).trim();
        if (raw.length() == 0) return;

        String[] parts = raw.split("\\Q||\\E");
        for (String p : parts) {
            String t = p.trim();
            if (t.length() > 0) solvedPuzzleKeys.add(t);
        }
    }

    public void loadAttemptFromLine(String line) {
        if (line == null) return;
        int eq = line.indexOf('=');
        if (eq < 0) return;
        String raw = line.substring(eq + 1).trim();
        if (raw.length() > 0) attemptLines.add(raw);
    }

    public void loadHintFromLine(String line) {
        if (line == null) return;
        int eq = line.indexOf('=');
        if (eq < 0) return;
        String raw = line.substring(eq + 1).trim();
        if (raw.length() > 0) hintLines.add(raw);
    }

    // -------------------------
    // Internal list helpers
    // -------------------------

    private void removeKeyLine(ArrayList<String> list, String key) {
        for (int i = 0; i < list.size(); i++) {
            String s = list.get(i);
            if (s != null && s.startsWith(key + "||")) {
                list.remove(i);
                return;
            }
        }
    }

    private int getKeyCount(ArrayList<String> list, String key) {
        for (String s : list) {
            if (s != null && s.startsWith(key + "||")) {
                String[] parts = s.split("\\Q||\\E");
                if (parts.length >= 2) {
                    try { return Integer.parseInt(parts[1].trim()); }
                    catch (Exception ignored) {}
                }
            }
        }
        return 0;
    }
}
