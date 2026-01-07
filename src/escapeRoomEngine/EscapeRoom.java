package escapeRoomEngine;

import java.util.ArrayList;

/**
 * Represents an entire escape room "game" (the content).
 * Holds rooms, the overall room flow (linear / semi-open / branching),
 * and the win condition.
 *
 * NOTE: Branching support here is kept HashMap-free by using a simple
 * list of links (RoomLink). The GUI/maker can create these links.
 */
public class EscapeRoom {

    // How rooms unlock / connect across the whole escape room
    public enum StructureMode {
        LINEAR,     // Room 0 -> Room 1 -> Room 2 ...
        SEMI_OPEN,  // can enter any unlocked room (defined by links / rules)
        BRANCHING   // links decide what room(s) unlock after conditions
    }

    // What ends the game
    public enum WinCondition {
        REACH_FINAL_ROOM_AND_COMPLETE, // complete the last room in the list
        COMPLETE_ALL_ROOMS,            // all rooms complete
        COMPLETE_SPECIFIC_ROOM         // complete a particular room ID
    }

    /**
     * A link that can unlock / allow travel from one room to another.
     * - If requiredRoomId is empty, link is always allowed.
     * - If requiredRoomId is set, that room must be complete to allow this link.
     *
     * This avoids HashMaps and still supports branching graphs.
     */
    public static class RoomLink {
        private String fromRoomId;
        private String toRoomId;
        private String requiredRoomId; // room that must be complete (optional)

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
    private StructureMode structureMode;
    private WinCondition winCondition;

    private ArrayList<Room> rooms;
    private ArrayList<RoomLink> links;

    // used only if winCondition == COMPLETE_SPECIFIC_ROOM
    private String winRoomId;

    public EscapeRoom(String title, StructureMode structureMode, WinCondition winCondition) {
        this.title = (title == null) ? "" : title;
        this.structureMode = (structureMode == null) ? StructureMode.LINEAR : structureMode;
        this.winCondition = (winCondition == null) ? WinCondition.REACH_FINAL_ROOM_AND_COMPLETE : winCondition;

        this.rooms = new ArrayList<>();
        this.links = new ArrayList<>();
        this.winRoomId = "";
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

    public StructureMode getStructureMode() {
        return structureMode;
    }

    public WinCondition getWinCondition() {
        return winCondition;
    }

    // -------------------------
    // Branching links
    // -------------------------

    public void addLink(String fromRoomId, String toRoomId, String requiredRoomId) {
        links.add(new RoomLink(fromRoomId, toRoomId, requiredRoomId));
    }

    public ArrayList<RoomLink> getLinksCopy() {
        return new ArrayList<>(links);
    }

    /**
     * Returns the room IDs you are allowed to travel to from the current room,
     * based on link requirements and room completion states.
     */
    public ArrayList<String> getAvailableNextRoomIds(String currentRoomId) {
        ArrayList<String> next = new ArrayList<>();
        if (currentRoomId == null) return next;

        // LINEAR: next is simply the next index (if current exists)
        if (structureMode == StructureMode.LINEAR) {
            int idx = getRoomIndexById(currentRoomId);
            int nextIdx = idx + 1;
            if (idx != -1 && nextIdx >= 0 && nextIdx < rooms.size()) {
                next.add(rooms.get(nextIdx).getId());
            }
            return next;
        }

        // SEMI_OPEN/BRANCHING: use links; if no links exist, fall back to "any room"
        if (links.size() == 0) {
            for (Room r : rooms) next.add(r.getId());
            return next;
        }

        for (RoomLink link : links) {
            if (!currentRoomId.equals(link.getFromRoomId())) continue;

            // If no requirement, always allowed
            if (link.getRequiredRoomId().isEmpty()) {
                next.add(link.getToRoomId());
            } else {
                Room required = getRoomById(link.getRequiredRoomId());
                if (required != null && required.isComplete()) {
                    next.add(link.getToRoomId());
                }
            }
        }

        return next;
    }

    // -------------------------
    // Win condition handling
    // -------------------------

    public void setWinRoomId(String winRoomId) {
        this.winRoomId = (winRoomId == null) ? "" : winRoomId;
    }

    public String getWinRoomId() {
        return winRoomId;
    }

    /**
     * Returns true if the escape room is won, based on the configured win condition.
     */
    public boolean isWon() {
        if (rooms.size() == 0) return false;

        switch (winCondition) {
            case REACH_FINAL_ROOM_AND_COMPLETE:
                return rooms.get(rooms.size() - 1).isComplete();

            case COMPLETE_ALL_ROOMS:
                for (Room r : rooms) {
                    if (!r.isComplete()) return false;
                }
                return true;

            case COMPLETE_SPECIFIC_ROOM:
                if (winRoomId.isEmpty()) return false;
                Room winRoom = getRoomById(winRoomId);
                return winRoom != null && winRoom.isComplete();

            default:
                return false;
        }
    }
}

