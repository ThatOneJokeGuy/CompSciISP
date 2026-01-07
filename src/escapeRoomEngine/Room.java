package escapeRoomEngine;

import java.util.ArrayList;

/**
 * Represents one room in the escape room.
 * A room contains puzzles and controls how they unlock (RoomMode).
 *
 * NEW (Open mode display settings):
 * - OPEN can be shown as:
 *    1) SWITCHER: one window lets you switch between puzzles in the room
 *    2) MULTI_WINDOWS: multiple puzzle windows open at once (one per puzzle)
 * - puzzlesPerWindow is included to support grouping puzzles later if you want.
 */
public class Room {

    public enum RoomMode {
        LINEAR,     // puzzles unlock in order
        SEMI_OPEN,  // most puzzles available, but the last puzzle has a custom unlock rule
        OPEN        // all puzzles available immediately
    }

    // How SEMI_OPEN unlocks the LAST puzzle:
    public enum SemiOpenUnlockRule {
        LAST_UNLOCK_AFTER_N_SOLVED,           // unlock last after N puzzles solved
        LAST_UNLOCK_AFTER_PERCENT_SOLVED,     // unlock last after % solved (rounded up)
        LAST_UNLOCK_AFTER_ALL_OTHERS_SOLVED   // unlock last after all other puzzles solved
    }

    // NEW: How OPEN rooms should appear to the player (your "open idea")
    public enum OpenDisplayStyle {
        SWITCHER,      // one window, player switches between puzzles
        MULTI_WINDOWS  // multiple windows at once (one per puzzle)
    }

    private String id;
    private String title;
    private String description;
    private RoomMode mode;

    private ArrayList<Puzzle> puzzles;

    // --- Custom SEMI_OPEN settings ---
    private SemiOpenUnlockRule semiOpenRule;
    private int semiOpenN;        // used when rule = LAST_UNLOCK_AFTER_N_SOLVED
    private int semiOpenPercent;  // used when rule = LAST_UNLOCK_AFTER_PERCENT_SOLVED (0-100)

    // --- NEW: OPEN display settings ---
    private OpenDisplayStyle openDisplayStyle; // SWITCHER or MULTI_WINDOWS
    private int puzzlesPerWindow;              // mainly used later; default 1

    public Room(String id, String title, String description, RoomMode mode) {
        this.id = (id == null) ? "" : id;
        this.title = (title == null) ? "" : title;
        this.description = (description == null) ? "" : description;
        this.mode = (mode == null) ? RoomMode.SEMI_OPEN : mode;

        this.puzzles = new ArrayList<>();

        // defaults (safe + reasonable)
        this.semiOpenRule = SemiOpenUnlockRule.LAST_UNLOCK_AFTER_PERCENT_SOLVED;
        this.semiOpenN = 2;
        this.semiOpenPercent = 50;

        // NEW defaults
        this.openDisplayStyle = OpenDisplayStyle.SWITCHER;
        this.puzzlesPerWindow = 1;
    }

    // -------------------------
    // Puzzle management
    // -------------------------

    public void addPuzzle(Puzzle puzzle) {
        if (puzzle != null) puzzles.add(puzzle);
    }

    public int getPuzzleCount() {
        return puzzles.size();
    }

    public Puzzle getPuzzle(int index) {
        if (index < 0 || index >= puzzles.size()) return null;
        return puzzles.get(index);
    }

    // -------------------------
    // Unlock rules
    // -------------------------

    /**
     * Returns true if every puzzle in this room is solved.
     * Used by the engine to decide if the player can leave the room (for linear games)
     * and for win conditions.
     */
    public boolean isComplete() {
        if (puzzles == null || puzzles.size() == 0) {
            // If a room has no puzzles, treat it as complete (you can change this if you want).
            return true;
        }

        for (Puzzle p : puzzles) {
            if (p != null && !p.isSolved()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether the player is allowed to ATTEMPT this puzzle right now.
     * NOTE: this does not check attempts remaining (Puzzle does that).
     */
    public boolean canAttemptPuzzle(int index) {
        if (index < 0 || index >= puzzles.size()) return false;

        Puzzle p = puzzles.get(index);
        if (p == null) return false;

        // If already solved, allow (UI can still display it)
        if (p.isSolved()) return true;

        if (mode == RoomMode.OPEN) {
            return true;
        }

        if (mode == RoomMode.LINEAR) {
            if (index == 0) return true;
            Puzzle prev = puzzles.get(index - 1);
            return prev != null && prev.isSolved();
        }

        // SEMI_OPEN:
        // - all puzzles except last are available
        // - last puzzle unlocks based on creator's chosen rule
        int total = puzzles.size();
        int lastIndex = total - 1;

        if (total <= 1) {
            // only puzzle is also "last" -> allow it immediately
            return true;
        }

        if (index != lastIndex) {
            return true;
        }

        // index IS last puzzle -> apply custom rule
        int solved = countSolvedPuzzles();

        // last puzzle is currently unsolved, so solved count is safe (doesn't include it)
        switch (semiOpenRule) {
            case LAST_UNLOCK_AFTER_ALL_OTHERS_SOLVED:
                // must solve every puzzle except the last one
                return solved >= (total - 1);

            case LAST_UNLOCK_AFTER_N_SOLVED:
                // clamp required to [0 .. total-1]
                int requiredN = semiOpenN;
                if (requiredN < 0) requiredN = 0;
                if (requiredN > total - 1) requiredN = total - 1;
                return solved >= requiredN;

            case LAST_UNLOCK_AFTER_PERCENT_SOLVED:
            default:
                // percent is based on total puzzles
                int percent = semiOpenPercent;
                if (percent < 0) percent = 0;
                if (percent > 100) percent = 100;

                // required solved count = ceil(total * percent / 100)
                int required = (int) Math.ceil(total * (percent / 100.0));

                // but last puzzle can't unlock by itself, so cap to total-1
                if (required > total - 1) required = total - 1;

                return solved >= required;
        }
    }

    private int countSolvedPuzzles() {
        int count = 0;
        for (Puzzle p : puzzles) {
            if (p != null && p.isSolved()) count++;
        }
        return count;
    }

    // -------------------------
    // Getters / setters
    // -------------------------

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public RoomMode getMode() {
        return mode;
    }

    public void setTitle(String title) {
        this.title = (title == null) ? "" : title;
    }

    public void setDescription(String description) {
        this.description = (description == null) ? "" : description;
    }

    public void setMode(RoomMode mode) {
        if (mode != null) this.mode = mode;
    }

    // --- Semi-open customization getters/setters ---

    public SemiOpenUnlockRule getSemiOpenRule() {
        return semiOpenRule;
    }

    public void setSemiOpenRule(SemiOpenUnlockRule semiOpenRule) {
        if (semiOpenRule != null) this.semiOpenRule = semiOpenRule;
    }

    public int getSemiOpenN() {
        return semiOpenN;
    }

    public void setSemiOpenN(int semiOpenN) {
        this.semiOpenN = semiOpenN;
    }

    public int getSemiOpenPercent() {
        return semiOpenPercent;
    }

    public void setSemiOpenPercent(int semiOpenPercent) {
        this.semiOpenPercent = semiOpenPercent;
    }

    // --- NEW: Open display settings getters/setters ---

    public OpenDisplayStyle getOpenDisplayStyle() {
        return openDisplayStyle;
    }

    public void setOpenDisplayStyle(OpenDisplayStyle style) {
        if (style != null) this.openDisplayStyle = style;
    }

    public int getPuzzlesPerWindow() {
        return puzzlesPerWindow;
    }

    public void setPuzzlesPerWindow(int puzzlesPerWindow) {
        if (puzzlesPerWindow < 1) puzzlesPerWindow = 1;
        this.puzzlesPerWindow = puzzlesPerWindow;
    }
}
