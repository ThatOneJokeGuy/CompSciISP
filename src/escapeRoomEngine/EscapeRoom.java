package escapeRoomEngine;

import java.util.ArrayList;

/**
 * Represents an entire escape room "game" (the content).
 *
 * UPDATE (as requested):
 * - Structure is ALWAYS LINEAR.
 * - Win condition is ALWAYS "complete the final room".
 *
 * We keep older enums/methods for compatibility with the rest of your code,
 * but the game logic will always behave as linear + final-room win.
 */
public class EscapeRoom {

    // Kept for compatibility (but ignored)
    public enum StructureMode {
        LINEAR,
        SEMI_OPEN,
        BRANCHING
    }

    // Kept for compatibility (but ignored)
    public enum WinCondition {
        REACH_FINAL_ROOM_AND_COMPLETE,
        COMPLETE_ALL_ROOMS,
        COMPLETE_SPECIFIC_ROOM
    }

    /**
     * Optional link support (kept for compatibility).
     * NOTE: Current project logic ignores links because structure is forced LINEAR.
     */
    public static class RoomLink {
        private String fromRoomId;
        private String toRoomId;
        private String requiredRoomId;

        public RoomLink(String fromRoomId, String toRoomId, String requiredRoomId) {
            this.fromRoomId = (fromRoomId == null) ? "" : fromRoomId;
            this.toRoomId = (toRoomId == null) ? "" : toRoomId;
            this.requiredRoomId = (requiredRoomId == null) ? "" : requiredRoomId;
        }

        public String getFromRoomId() { return fromRoomId; }
        public String getToRoomId() { return toRoomId; }
        public String getRequiredRoomId() { return requiredRoomId; }
    }

    private String title;

    private ArrayList<Room> rooms;
    private ArrayList<RoomLink> links; // kept, but unused by forced-linear flow

    // -------------------------
    // Constructors
    // -------------------------

    /** New simple constructor (recommended). */
    public EscapeRoom(String title) {
        this.title = (title == null) ? "" : title;
        this.rooms = new ArrayList<>();
        this.links = new ArrayList<>();
    }

    /** Old constructor kept so older code still compiles. Structure/win are ignored. */
    public EscapeRoom(String title, StructureMode structureMode, WinCondition winCondition) {
        this(title);
    }

    // -------------------------
    // Room management
    // -------------------------

    public void addRoom(Room room) {
        if (room != null) rooms.add(room);
    }

    public int getRoomCount() {
        return rooms.size();
    }

    public Room getRoomByIndex(int index) {
        if (index < 0 || index >= rooms.size()) return null;
        return rooms.get(index);
    }

    public Room getRoomById(String id) {
        if (id == null) return null;
        for (Room r : rooms) {
            if (id.equals(r.getId())) return r;
        }
        return null;
    }

    public int getRoomIndexById(String id) {
        if (id == null) return -1;
        for (int i = 0; i < rooms.size(); i++) {
            if (id.equals(rooms.get(i).getId())) return i;
        }
        return -1;
    }

    public String getTitle() {
        return title;
    }

    /** Forced LINEAR */
    public StructureMode getStructureMode() {
        return StructureMode.LINEAR;
    }

    /** Forced FINAL ROOM COMPLETE */
    public WinCondition getWinCondition() {
        return WinCondition.REACH_FINAL_ROOM_AND_COMPLETE;
    }

    // -------------------------
    // Links (kept for compatibility)
    // -------------------------

    public void addLink(String fromRoomId, String toRoomId, String requiredRoomId) {
        links.add(new RoomLink(fromRoomId, toRoomId, requiredRoomId));
    }

    public ArrayList<RoomLink> getLinksCopy() {
        return new ArrayList<>(links);
    }

    /**
     * Returns the room IDs you are allowed to travel to from the current room.
     * UPDATE: Always uses LINEAR rule: next room in the list.
     */
    public ArrayList<String> getAvailableNextRoomIds(String currentRoomId) {
        ArrayList<String> next = new ArrayList<>();
        if (currentRoomId == null) return next;

        int idx = getRoomIndexById(currentRoomId);
        int nextIdx = idx + 1;
        if (idx != -1 && nextIdx >= 0 && nextIdx < rooms.size()) {
            next.add(rooms.get(nextIdx).getId());
        }
        return next;
    }

    // -------------------------
    // Win condition handling
    // -------------------------

    /**
     * Forced win condition: complete the last room in the list.
     */
    public boolean isWon() {
        if (rooms.size() == 0) return false;
        Room last = rooms.get(rooms.size() - 1);
        return last != null && last.isComplete();
    }

    // Older API kept so other classes compile (no longer used)
    public void setWinRoomId(String winRoomId) { /* ignored */ }
    public String getWinRoomId() { return ""; }
}
