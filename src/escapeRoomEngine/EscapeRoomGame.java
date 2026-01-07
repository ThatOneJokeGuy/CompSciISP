package escapeRoomEngine;

import java.util.ArrayList;

/**
 * EscapeRoomGame
 *
 * Main engine controller for playing an EscapeRoom (logic only, no GUI).
 * The GUI can call these methods to:
 * - get current room/puzzles
 * - attempt answers
 * - use hints
 * - move between rooms (linear or branching)
 * - check win condition
 *
 * IMPORTANT FIX:
 * Your old version called methods that do NOT exist in PlayerProgress:
 *   syncWithEscapeRoom(), applyToEscapeRoom(), captureFromEscapeRoom()
 *
 * This version compiles with your current PlayerProgress by doing manual capture.
 */
public class EscapeRoomGame {

    private EscapeRoom escapeRoom;
    private PlayerProgress progress;

    public EscapeRoomGame(EscapeRoom escapeRoom, PlayerProgress progress) {
        this.escapeRoom = escapeRoom;
        this.progress = (progress == null) ? new PlayerProgress() : progress;

        // If no current room set, default to first room
        if (this.escapeRoom != null) {
            if (this.progress.getCurrentRoomId() == null || this.progress.getCurrentRoomId().trim().isEmpty()) {
                if (this.escapeRoom.getRoomCount() > 0 && this.escapeRoom.getRoomByIndex(0) != null) {
                    this.progress.setCurrentRoomId(this.escapeRoom.getRoomByIndex(0).getId());
                }
            }

            // Apply saved progress into puzzles (solved/attempts/hints)
            applyProgressToEscapeRoom(this.escapeRoom, this.progress);
        }
    }

    // -------------------------
    // Basic accessors for UI
    // -------------------------

    public String getGameTitle() {
        return (escapeRoom == null) ? "" : escapeRoom.getTitle();
    }

    public String getCurrentRoomId() {
        return (progress == null) ? "" : progress.getCurrentRoomId();
    }

    public Room getCurrentRoom() {
        if (escapeRoom == null || progress == null) return null;
        return escapeRoom.getRoomById(progress.getCurrentRoomId());
    }

    public boolean isWon() {
        if (escapeRoom == null) return false;
        return escapeRoom.isWon();
    }

    // Add getter for PlayerProgress so GUIs can access/save progress
    public PlayerProgress getProgress() {
        return this.progress;
    }

    // -------------------------
    // Puzzle interaction
    // -------------------------

    public AttemptResult attemptCurrentRoomPuzzle(int puzzleIndex, String userInput) {
        Room room = getCurrentRoom();
        if (room == null) return AttemptResult.error("No current room.");

        if (!room.canAttemptPuzzle(puzzleIndex)) {
            return AttemptResult.error("That puzzle is locked right now.");
        }

        Puzzle puzzle = room.getPuzzle(puzzleIndex);
        if (puzzle == null) return AttemptResult.error("Invalid puzzle.");

        if (!puzzle.hasAttemptsRemaining()) {
            return AttemptResult.error("No attempts remaining for this puzzle.");
        }

        boolean solvedNow = puzzle.attemptAnswer(userInput);

        // Keep progress synced for saving later
        captureProgressFromEscapeRoom(escapeRoom, progress);

        if (solvedNow) {
            return AttemptResult.success("Correct! Puzzle solved.");
        } else {
            int left = puzzle.getAttemptsRemaining();
            if (left <= 0) return AttemptResult.fail("Incorrect. No attempts remaining.");
            return AttemptResult.fail("Incorrect. Attempts remaining: " + left);
        }
    }

    public HintResult useHintForCurrentRoomPuzzle(int puzzleIndex) {
        Room room = getCurrentRoom();
        if (room == null) return HintResult.error("No current room.");

        Puzzle puzzle = room.getPuzzle(puzzleIndex);
        if (puzzle == null) return HintResult.error("Invalid puzzle.");

        String hint = puzzle.useHint();

        // Keep progress synced
        captureProgressFromEscapeRoom(escapeRoom, progress);

        if (hint == null || hint.isEmpty()) {
            return HintResult.ok("(No hint provided for this puzzle.)");
        }
        return HintResult.ok(hint);
    }

    // -------------------------
    // Room navigation
    // -------------------------

    public ArrayList<String> getAvailableNextRoomIds() {
        if (escapeRoom == null || progress == null) return new ArrayList<>();
        return escapeRoom.getAvailableNextRoomIds(progress.getCurrentRoomId());
    }

    public MoveResult moveToRoom(String targetRoomId) {
        if (escapeRoom == null || progress == null) return MoveResult.error("Game not initialized.");
        if (targetRoomId == null || targetRoomId.isEmpty()) return MoveResult.error("Invalid room.");

        Room current = getCurrentRoom();
        if (current == null) return MoveResult.error("No current room.");

        // LINEAR: require current room complete and target must be the next room
        if (escapeRoom.getStructureMode() == EscapeRoom.StructureMode.LINEAR) {
            if (!current.isComplete()) {
                return MoveResult.error("You must complete this room before moving on.");
            }

            ArrayList<String> next = getAvailableNextRoomIds();
            if (next.size() == 0 || !next.get(0).equals(targetRoomId)) {
                return MoveResult.error("You can only move to the next room.");
            }

            progress.setCurrentRoomId(targetRoomId);
            return MoveResult.ok("Moved to next room.");
        }

        // SEMI_OPEN / BRANCHING: must be in available list
        ArrayList<String> nextRooms = getAvailableNextRoomIds();
        boolean allowed = false;
        for (String id : nextRooms) {
            if (id.equals(targetRoomId)) {
                allowed = true;
                break;
            }
        }

        if (!allowed) return MoveResult.error("That room is not available from here yet.");

        progress.setCurrentRoomId(targetRoomId);
        return MoveResult.ok("Moved to room: " + targetRoomId);
    }

    // -------------------------
    // Manual progress apply/capture
    // -------------------------

    private static void applyProgressToEscapeRoom(EscapeRoom er, PlayerProgress prog) {
        if (er == null || prog == null) return;

        for (int r = 0; r < er.getRoomCount(); r++) {
            Room room = er.getRoomByIndex(r);
            if (room == null) continue;

            for (int p = 0; p < room.getPuzzleCount(); p++) {
                Puzzle puz = room.getPuzzle(p);
                if (puz == null) continue;

                if (prog.isPuzzleSolved(puz.getId())) puz.setSolved(true);

                puz.setAttemptsUsed(prog.getAttemptCount(puz.getId()));
                puz.setHintsUsed(prog.getHintsUsed(puz.getId()));
            }
        }
    }

    private static void captureProgressFromEscapeRoom(EscapeRoom er, PlayerProgress prog) {
        if (er == null || prog == null) return;

        for (int r = 0; r < er.getRoomCount(); r++) {
            Room room = er.getRoomByIndex(r);
            if (room == null) continue;

            for (int p = 0; p < room.getPuzzleCount(); p++) {
                Puzzle puz = room.getPuzzle(p);
                if (puz == null) continue;

                if (puz.isSolved()) prog.markPuzzleSolved(puz.getId());

                prog.setAttemptCount(puz.getId(), puz.getAttemptsUsed());
                prog.setHintsUsed(puz.getId(), puz.getHintsUsed());
            }
        }
    }

    // -------------------------
    // Small result objects (GUI-friendly)
    // -------------------------

    public static class AttemptResult {
        private boolean ok;
        private boolean solved;
        private String message;

        private AttemptResult(boolean ok, boolean solved, String message) {
            this.ok = ok;
            this.solved = solved;
            this.message = message;
        }

        public static AttemptResult success(String message) {
            return new AttemptResult(true, true, message);
        }

        public static AttemptResult fail(String message) {
            return new AttemptResult(true, false, message);
        }

        public static AttemptResult error(String message) {
            return new AttemptResult(false, false, message);
        }

        public boolean isOk() { return ok; }
        public boolean isSolved() { return solved; }
        public String getMessage() { return message; }
    }

    public static class HintResult {
        private boolean ok;
        private String messageOrHint;

        private HintResult(boolean ok, String messageOrHint) {
            this.ok = ok;
            this.messageOrHint = messageOrHint;
        }

        public static HintResult ok(String hint) {
            return new HintResult(true, hint);
        }

        public static HintResult error(String msg) {
            return new HintResult(false, msg);
        }

        public boolean isOk() { return ok; }
        public String getText() { return messageOrHint; }
    }

    public static class MoveResult {
        private boolean ok;
        private String message;

        private MoveResult(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }

        public static MoveResult ok(String msg) {
            return new MoveResult(true, msg);
        }

        public static MoveResult error(String msg) {
            return new MoveResult(false, msg);
        }

        public boolean isOk() { return ok; }
        public String getMessage() { return message; }
    }
}