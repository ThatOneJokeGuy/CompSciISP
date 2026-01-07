package escapeRoomEngine;

import java.util.ArrayList;

/**
 * PlayerProgress
 *
 * Stores a player's progress while playing an escape room.
 * Saved data includes:
 *  - currentRoomId
 *  - solved puzzle IDs
 *  - attempt counts per puzzle
 *  - hint counts per puzzle
 *
 * IMPORTANT: Uses ArrayLists only (no HashMap) for ICS4U.
 * We store attempts/hints using parallel lists:
 *   attemptPuzzleIds[i] matches attemptCounts[i]
 */
public class PlayerProgress {

    private String currentRoomId;

    // Solved puzzles
    private ArrayList<String> solvedPuzzleIds;

    // Attempts tracking (parallel lists)
    private ArrayList<String> attemptPuzzleIds;
    private ArrayList<Integer> attemptCounts;

    // Hints tracking (parallel lists)
    private ArrayList<String> hintPuzzleIds;
    private ArrayList<Integer> hintCounts;

    public PlayerProgress() {
        currentRoomId = "";
        solvedPuzzleIds = new ArrayList<>();

        attemptPuzzleIds = new ArrayList<>();
        attemptCounts = new ArrayList<>();

        hintPuzzleIds = new ArrayList<>();
        hintCounts = new ArrayList<>();
    }

    // -------------------------
    // Current room
    // -------------------------

    public String getCurrentRoomId() {
        return currentRoomId;
    }

    public void setCurrentRoomId(String currentRoomId) {
        if (currentRoomId == null) currentRoomId = "";
        this.currentRoomId = currentRoomId;
    }

    // -------------------------
    // Solved puzzles
    // -------------------------

    public boolean isPuzzleSolved(String puzzleId) {
        if (puzzleId == null) return false;
        for (int i = 0; i < solvedPuzzleIds.size(); i++) {
            if (puzzleId.equals(solvedPuzzleIds.get(i))) return true;
        }
        return false;
    }

    public void markPuzzleSolved(String puzzleId) {
        if (puzzleId == null) return;
        if (!isPuzzleSolved(puzzleId)) {
            solvedPuzzleIds.add(puzzleId);
        }
    }

    public ArrayList<String> getSolvedPuzzleIdsCopy() {
        return new ArrayList<>(solvedPuzzleIds);
    }

    // -------------------------
    // Attempts
    // -------------------------

    public int getAttemptCount(String puzzleId) {
        int idx = indexOf(attemptPuzzleIds, puzzleId);
        if (idx == -1) return 0;
        return attemptCounts.get(idx);
    }

    public void setAttemptCount(String puzzleId, int count) {
        if (puzzleId == null) return;
        if (count < 0) count = 0;

        int idx = indexOf(attemptPuzzleIds, puzzleId);
        if (idx == -1) {
            attemptPuzzleIds.add(puzzleId);
            attemptCounts.add(count);
        } else {
            attemptCounts.set(idx, count);
        }
    }

    // -------------------------
    // Hints
    // -------------------------

    public int getHintsUsed(String puzzleId) {
        int idx = indexOf(hintPuzzleIds, puzzleId);
        if (idx == -1) return 0;
        return hintCounts.get(idx);
    }

    public void setHintsUsed(String puzzleId, int count) {
        if (puzzleId == null) return;
        if (count < 0) count = 0;

        int idx = indexOf(hintPuzzleIds, puzzleId);
        if (idx == -1) {
            hintPuzzleIds.add(puzzleId);
            hintCounts.add(count);
        } else {
            hintCounts.set(idx, count);
        }
    }

    // -------------------------
    // Helpers
    // -------------------------

    private int indexOf(ArrayList<String> list, String value) {
        if (list == null || value == null) return -1;
        for (int i = 0; i < list.size(); i++) {
            if (value.equals(list.get(i))) return i;
        }
        return -1;
    }

    private int parseIntSafe(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return fallback;
        }
    }

    private String joinWithPipes(ArrayList<String> list) {
        if (list == null || list.size() == 0) return "";
        String out = "";
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) out += "||";
            out += list.get(i);
        }
        return out;
    }

    // -------------------------
    // NEW: Methods needed by EscapeRoomGame
    // -------------------------

    /**
     * Ensures the progress has a valid current room and has entries ready for every puzzle.
     * This prevents "null room" and makes saving/loading smoother.
     */
    public void syncWithEscapeRoom(EscapeRoom er) {
        if (er == null) return;

        // If current room is blank or invalid, default to first room (if any)
        if (currentRoomId == null) currentRoomId = "";
        if (currentRoomId.trim().isEmpty() || er.getRoomById(currentRoomId) == null) {
            if (er.getRoomCount() > 0 && er.getRoomByIndex(0) != null) {
                currentRoomId = er.getRoomByIndex(0).getId();
            }
        }

        // Ensure attempts/hints lists have entries for each puzzle ID (at least 0)
        for (int r = 0; r < er.getRoomCount(); r++) {
            Room room = er.getRoomByIndex(r);
            if (room == null) continue;

            for (int p = 0; p < room.getPuzzleCount(); p++) {
                Puzzle puz = room.getPuzzle(p);
                if (puz == null) continue;

                String pid = puz.getId();
                if (pid == null || pid.trim().isEmpty()) continue;

                // initialize if missing
                if (indexOf(attemptPuzzleIds, pid) == -1) {
                    attemptPuzzleIds.add(pid);
                    attemptCounts.add(0);
                }
                if (indexOf(hintPuzzleIds, pid) == -1) {
                    hintPuzzleIds.add(pid);
                    hintCounts.add(0);
                }
            }
        }
    }

    /**
     * Applies this saved progress onto the puzzles inside the EscapeRoom object
     * (solved state + attempts used + hints used).
     */
    public void applyToEscapeRoom(EscapeRoom er) {
        if (er == null) return;

        for (int r = 0; r < er.getRoomCount(); r++) {
            Room room = er.getRoomByIndex(r);
            if (room == null) continue;

            for (int p = 0; p < room.getPuzzleCount(); p++) {
                Puzzle puz = room.getPuzzle(p);
                if (puz == null) continue;

                String pid = puz.getId();

                // solved
                puz.setSolved(isPuzzleSolved(pid));

                // attempts/hints used
                puz.setAttemptsUsed(getAttemptCount(pid));
                puz.setHintsUsed(getHintsUsed(pid));
            }
        }
    }

    /**
     * Reads the current puzzle states from the EscapeRoom object and stores them into this progress.
     * (Useful right before saving.)
     */
    public void captureFromEscapeRoom(EscapeRoom er) {
        if (er == null) return;

        for (int r = 0; r < er.getRoomCount(); r++) {
            Room room = er.getRoomByIndex(r);
            if (room == null) continue;

            for (int p = 0; p < room.getPuzzleCount(); p++) {
                Puzzle puz = room.getPuzzle(p);
                if (puz == null) continue;

                String pid = puz.getId();
                if (pid == null || pid.trim().isEmpty()) continue;

                if (puz.isSolved()) {
                    markPuzzleSolved(pid);
                }

                setAttemptCount(pid, puz.getAttemptsUsed());
                setHintsUsed(pid, puz.getHintsUsed());
            }
        }
    }

    // -------------------------
    // SAVE FORMAT HELPERS
    // -------------------------

    public String toSolvedLine() {
        return "SOLVED=" + joinWithPipes(solvedPuzzleIds);
    }

    public ArrayList<String> toAttemptLines() {
        ArrayList<String> lines = new ArrayList<>();
        for (int i = 0; i < attemptPuzzleIds.size(); i++) {
            lines.add("ATTEMPT=" + attemptPuzzleIds.get(i) + "||" + attemptCounts.get(i));
        }
        return lines;
    }

    public ArrayList<String> toHintLines() {
        ArrayList<String> lines = new ArrayList<>();
        for (int i = 0; i < hintPuzzleIds.size(); i++) {
            lines.add("HINT=" + hintPuzzleIds.get(i) + "||" + hintCounts.get(i));
        }
        return lines;
    }

    public void loadSolvedFromLine(String line) {
        // expects SOLVED=a||b||c
        if (line == null) return;
        int eq = line.indexOf('=');
        if (eq < 0) return;

        String data = line.substring(eq + 1).trim();
        solvedPuzzleIds.clear();

        if (data.length() == 0) return;

        String[] parts = data.split("\\|\\|");
        for (int i = 0; i < parts.length; i++) {
            String id = parts[i].trim();
            if (id.length() > 0) solvedPuzzleIds.add(id);
        }
    }

    public void loadAttemptFromLine(String line) {
        // expects ATTEMPT=puzzleId||number
        if (line == null) return;
        int eq = line.indexOf('=');
        if (eq < 0) return;

        String data = line.substring(eq + 1).trim();
        String[] parts = data.split("\\|\\|");
        if (parts.length < 2) return;

        String puzzleId = parts[0].trim();
        int count = parseIntSafe(parts[1].trim(), 0);
        setAttemptCount(puzzleId, count);
    }

    public void loadHintFromLine(String line) {
        // expects HINT=puzzleId||number
        if (line == null) return;
        int eq = line.indexOf('=');
        if (eq < 0) return;

        String data = line.substring(eq + 1).trim();
        String[] parts = data.split("\\|\\|");
        if (parts.length < 2) return;

        String puzzleId = parts[0].trim();
        int count = parseIntSafe(parts[1].trim(), 0);
        setHintsUsed(puzzleId, count);
    }
}
