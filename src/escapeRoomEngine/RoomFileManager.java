package escapeRoomEngine;

import java.io.*;
import java.util.ArrayList;

/**
 * RoomFileManager
 *
 * Saves/loads an EscapeRoom to a plain .txt file.
 *
 * Supports per-room settings:
 * - SEMI_RULE
 * - SEMI_N
 * - SEMI_PERCENT
 * - OPEN_STYLE
 * - PUZZLES_PER_WINDOW
 *
 * File format (key=value with section markers).
 */
public class RoomFileManager {

    private static final String SEP = "||"; // separator used inside LINK and OPTIONS
    private static final String ER_BEGIN = "ER_BEGIN";
    private static final String ER_END = "ER_END";
    private static final String ROOM_BEGIN = "ROOM_BEGIN";
    private static final String ROOM_END = "ROOM_END";
    private static final String PUZZLE_BEGIN = "PUZZLE_BEGIN";
    private static final String PUZZLE_END = "PUZZLE_END";

    // -------------------------
    // Public API
    // -------------------------

    public static void saveToFile(String filePath, EscapeRoom er) throws IOException {
        if (filePath == null) throw new IOException("filePath is null");
        if (er == null) throw new IOException("EscapeRoom is null");

        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(filePath));

            bw.write(ER_BEGIN);
            bw.newLine();

            bw.write("TITLE=" + esc(er.getTitle()));
            bw.newLine();

            bw.write("STRUCTURE=" + er.getStructureMode());
            bw.newLine();

            bw.write("WIN=" + er.getWinCondition());
            bw.newLine();

            bw.write("WIN_ROOM_ID=" + esc(nullToEmpty(er.getWinRoomId())));
            bw.newLine();

            // Rooms
            for (int i = 0; i < er.getRoomCount(); i++) {
                Room r = er.getRoomByIndex(i);
                if (r == null) continue;

                bw.write(ROOM_BEGIN);
                bw.newLine();

                bw.write("ID=" + esc(r.getId()));
                bw.newLine();
                bw.write("TITLE=" + esc(r.getTitle()));
                bw.newLine();
                bw.write("MODE=" + r.getMode());
                bw.newLine();
                bw.write("DESC=" + esc(r.getDescription()));
                bw.newLine();

                // SEMI_OPEN customization
                bw.write("SEMI_RULE=" + r.getSemiOpenRule());
                bw.newLine();
                bw.write("SEMI_N=" + r.getSemiOpenN());
                bw.newLine();
                bw.write("SEMI_PERCENT=" + r.getSemiOpenPercent());
                bw.newLine();

                // OPEN customization
                bw.write("OPEN_STYLE=" + r.getOpenDisplayStyle());
                bw.newLine();
                bw.write("PUZZLES_PER_WINDOW=" + r.getPuzzlesPerWindow());
                bw.newLine();

                // Puzzles
                for (int p = 0; p < r.getPuzzleCount(); p++) {
                    Puzzle puz = r.getPuzzle(p);
                    if (puz == null) continue;

                    bw.write(PUZZLE_BEGIN);
                    bw.newLine();

                    bw.write("ID=" + esc(puz.getId()));
                    bw.newLine();
                    bw.write("TYPE=" + puz.getType());
                    bw.newLine();
                    bw.write("PROMPT=" + esc(puz.getPrompt()));
                    bw.newLine();
                    bw.write("ANSWER=" + esc(puz.getAnswerRaw()));
                    bw.newLine();
                    bw.write("VALIDATION=" + puz.getValidationMode());
                    bw.newLine();
                    bw.write("HINT=" + esc(puz.getHint()));
                    bw.newLine();
                    bw.write("MAX_ATTEMPTS=" + puz.getMaxAttempts());
                    bw.newLine();

                    // Options (MULTIPLE_CHOICE only; blank otherwise)
                    bw.write("OPTIONS=" + esc(joinOptions(puz.getOptions())));
                    bw.newLine();

                    bw.write(PUZZLE_END);
                    bw.newLine();
                }

                bw.write(ROOM_END);
                bw.newLine();
            }

            // Links (for branching/semi-open across rooms)
            for (EscapeRoom.RoomLink link : er.getLinksCopy()) {
                if (link == null) continue;

                String from = nullToEmpty(link.getFromRoomId());
                String to = nullToEmpty(link.getToRoomId());
                String req = nullToEmpty(link.getRequiredRoomId()); // may be empty

                bw.write("LINK=" + esc(from) + SEP + esc(to) + SEP + esc(req));
                bw.newLine();
            }

            bw.write(ER_END);
            bw.newLine();

        } finally {
            if (bw != null) bw.close();
        }
    }

    public static EscapeRoom loadFromFile(String filePath) throws IOException {
        if (filePath == null) throw new IOException("filePath is null");

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filePath));

            String line;
            EscapeRoom er = null;

            Room currentRoom = null;
            ArrayList<String> pendingLinks = new ArrayList<>();

            String title = "Untitled";
            EscapeRoom.StructureMode structure = EscapeRoom.StructureMode.LINEAR;
            EscapeRoom.WinCondition win = EscapeRoom.WinCondition.REACH_FINAL_ROOM_AND_COMPLETE;
            String winRoomId = "";

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) continue;

                if (line.equals(ER_BEGIN)) continue;
                if (line.equals(ER_END)) break;

                if (line.equals(ROOM_BEGIN)) {
                    currentRoom = new Room("", "", "", Room.RoomMode.SEMI_OPEN);
                    continue;
                }

                if (line.equals(ROOM_END)) {
                    if (er == null) {
                        er = new EscapeRoom(title, structure, win);
                        er.setWinRoomId(winRoomId);
                    }
                    if (currentRoom != null) er.addRoom(currentRoom);
                    currentRoom = null;
                    continue;
                }

                if (line.equals(PUZZLE_BEGIN)) {
                    Puzzle puzzle = readPuzzleBlock(br);
                    if (currentRoom != null && puzzle != null) {
                        currentRoom.addPuzzle(puzzle);
                    }
                    continue;
                }

                if (line.startsWith("LINK=")) {
                    pendingLinks.add(line.substring("LINK=".length()));
                    continue;
                }

                int eq = line.indexOf('=');
                if (eq < 0) continue;

                String key = line.substring(0, eq).trim();
                String val = line.substring(eq + 1).trim();

                if (currentRoom == null) {
                    // Header
                    if (key.equalsIgnoreCase("TITLE")) {
                        title = unesc(val);
                    } else if (key.equalsIgnoreCase("STRUCTURE")) {
                        structure = parseStructure(val, structure);
                    } else if (key.equalsIgnoreCase("WIN")) {
                        win = parseWin(val, win);
                    } else if (key.equalsIgnoreCase("WIN_ROOM_ID")) {
                        winRoomId = unesc(val);
                    }
                } else {
                    // Room fields
                    if (key.equalsIgnoreCase("ID")) {
                        String rid = unesc(val);
                        currentRoom = rebuildRoomWithId(currentRoom, rid);
                    } else if (key.equalsIgnoreCase("TITLE")) {
                        currentRoom.setTitle(unesc(val));
                    } else if (key.equalsIgnoreCase("MODE")) {
                        currentRoom.setMode(parseRoomMode(val, currentRoom.getMode()));
                    } else if (key.equalsIgnoreCase("DESC")) {
                        currentRoom.setDescription(unesc(val));
                    }

                    // SEMI settings
                    else if (key.equalsIgnoreCase("SEMI_RULE")) {
                        currentRoom.setSemiOpenRule(parseSemiRule(val, currentRoom.getSemiOpenRule()));
                    } else if (key.equalsIgnoreCase("SEMI_N")) {
                        currentRoom.setSemiOpenN(parseIntSafe(val, currentRoom.getSemiOpenN()));
                    } else if (key.equalsIgnoreCase("SEMI_PERCENT")) {
                        currentRoom.setSemiOpenPercent(parseIntSafe(val, currentRoom.getSemiOpenPercent()));
                    }

                    // OPEN settings
                    else if (key.equalsIgnoreCase("OPEN_STYLE")) {
                        currentRoom.setOpenDisplayStyle(parseOpenStyle(val, currentRoom.getOpenDisplayStyle()));
                    } else if (key.equalsIgnoreCase("PUZZLES_PER_WINDOW")) {
                        currentRoom.setPuzzlesPerWindow(parseIntSafe(val, currentRoom.getPuzzlesPerWindow()));
                    }
                }
            }

            if (er == null) {
                er = new EscapeRoom(title, structure, win);
                er.setWinRoomId(winRoomId);
            }

            // Apply links after rooms exist
            for (String raw : pendingLinks) {
                String[] parts = splitBySepPreserve(raw, 3);
                String from = parts.length > 0 ? unesc(parts[0]) : "";
                String to = parts.length > 1 ? unesc(parts[1]) : "";
                String req = parts.length > 2 ? unesc(parts[2]) : "";

                if (req.trim().length() == 0) req = null;

                if (from.length() > 0 && to.length() > 0) {
                    er.addLink(from, to, req);
                }
            }

            return er;

        } finally {
            if (br != null) br.close();
        }
    }

    // -------------------------
    // Puzzle block parsing
    // -------------------------

    private static Puzzle readPuzzleBlock(BufferedReader br) throws IOException {
        String id = "";
        Puzzle.PuzzleType type = Puzzle.PuzzleType.TEXT;
        String prompt = "";
        String answer = "";
        Puzzle.ValidationMode validation = Puzzle.ValidationMode.FLEXIBLE;
        String hint = "";
        int maxAttempts = 3;
        ArrayList<String> options = new ArrayList<>();

        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.equals(PUZZLE_END)) break;
            if (line.length() == 0) continue;

            int eq = line.indexOf('=');
            if (eq < 0) continue;

            String key = line.substring(0, eq).trim();
            String val = line.substring(eq + 1).trim();

            if (key.equalsIgnoreCase("ID")) {
                id = unesc(val);
            } else if (key.equalsIgnoreCase("TYPE")) {
                type = parsePuzzleType(val, type);
            } else if (key.equalsIgnoreCase("PROMPT")) {
                prompt = unesc(val);
            } else if (key.equalsIgnoreCase("ANSWER")) {
                answer = unesc(val);
            } else if (key.equalsIgnoreCase("VALIDATION")) {
                validation = parseValidation(val, validation);
            } else if (key.equalsIgnoreCase("HINT")) {
                hint = unesc(val);
            } else if (key.equalsIgnoreCase("MAX_ATTEMPTS")) {
                maxAttempts = parseIntSafe(val, maxAttempts);
            } else if (key.equalsIgnoreCase("OPTIONS")) {
                options = splitOptions(unesc(val));
            }
        }

        if (type == Puzzle.PuzzleType.MULTIPLE_CHOICE) {
            if (options == null) options = new ArrayList<>();
            return new Puzzle(id, prompt, options, answer, validation, hint, maxAttempts);
        } else {
            return new Puzzle(id, type, prompt, answer, validation, hint, maxAttempts);
        }
    }

    // -------------------------
    // Helpers (escaping, parsing)
    // -------------------------

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String esc(String s) {
        if (s == null) return "";
        String out = s;
        out = out.replace("\\", "\\\\");
        out = out.replace("\r\n", "\n");
        out = out.replace("\r", "\n");
        out = out.replace("\n", "\\n");
        out = out.replace(SEP, "\\sep");
        return out;
    }

    private static String unesc(String s) {
        if (s == null) return "";
        String out = s;
        out = out.replace("\\sep", SEP);
        out = out.replace("\\n", "\n");
        out = out.replace("\\\\", "\\");
        return out;
    }

    private static String joinOptions(ArrayList<String> opts) {
        if (opts == null || opts.size() == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < opts.size(); i++) {
            if (i > 0) sb.append(SEP);
            sb.append(opts.get(i) == null ? "" : opts.get(i));
        }
        return sb.toString();
    }

    private static ArrayList<String> splitOptions(String joined) {
        ArrayList<String> out = new ArrayList<>();
        if (joined == null) return out;
        String t = joined.trim();
        if (t.length() == 0) return out;

        String[] parts = splitBySepPreserve(t, -1);
        for (String p : parts) {
            String v = p == null ? "" : p.trim();
            if (v.length() > 0) out.add(v);
        }
        return out;
    }

    private static String[] splitBySepPreserve(String raw, int expectedParts) {
        if (raw == null) return new String[0];

        ArrayList<String> parts = new ArrayList<>();
        int i = 0;
        while (i <= raw.length()) {
            int j = raw.indexOf(SEP, i);
            if (j < 0) {
                parts.add(raw.substring(i));
                break;
            } else {
                parts.add(raw.substring(i, j));
                i = j + SEP.length();
                if (expectedParts > 0 && parts.size() == expectedParts - 1) {
                    parts.add(raw.substring(i));
                    break;
                }
            }
        }

        String[] arr = new String[parts.size()];
        for (int k = 0; k < parts.size(); k++) arr[k] = parts.get(k);
        return arr;
    }

    private static int parseIntSafe(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static EscapeRoom.StructureMode parseStructure(String s, EscapeRoom.StructureMode fallback) {
        try {
            return EscapeRoom.StructureMode.valueOf(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static EscapeRoom.WinCondition parseWin(String s, EscapeRoom.WinCondition fallback) {
        try {
            return EscapeRoom.WinCondition.valueOf(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static Room.RoomMode parseRoomMode(String s, Room.RoomMode fallback) {
        try {
            return Room.RoomMode.valueOf(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static Room.SemiOpenUnlockRule parseSemiRule(String s, Room.SemiOpenUnlockRule fallback) {
        try {
            return Room.SemiOpenUnlockRule.valueOf(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static Room.OpenDisplayStyle parseOpenStyle(String s, Room.OpenDisplayStyle fallback) {
        try {
            return Room.OpenDisplayStyle.valueOf(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static Puzzle.PuzzleType parsePuzzleType(String s, Puzzle.PuzzleType fallback) {
        try {
            return Puzzle.PuzzleType.valueOf(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static Puzzle.ValidationMode parseValidation(String s, Puzzle.ValidationMode fallback) {
        try {
            return Puzzle.ValidationMode.valueOf(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static Room rebuildRoomWithId(Room existing, String newId) {
        if (existing == null) {
            return new Room(newId, "", "", Room.RoomMode.SEMI_OPEN);
        }

        Room r = new Room(newId,
                existing.getTitle(),
                existing.getDescription(),
                existing.getMode());

        // copy semi-open settings
        r.setSemiOpenRule(existing.getSemiOpenRule());
        r.setSemiOpenN(existing.getSemiOpenN());
        r.setSemiOpenPercent(existing.getSemiOpenPercent());

        // copy open settings
        r.setOpenDisplayStyle(existing.getOpenDisplayStyle());
        r.setPuzzlesPerWindow(existing.getPuzzlesPerWindow());

        // copy puzzles already added
        for (int i = 0; i < existing.getPuzzleCount(); i++) {
            r.addPuzzle(existing.getPuzzle(i));
        }

        return r;
    }
}
