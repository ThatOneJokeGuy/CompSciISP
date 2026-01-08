package escapeRoomEngine;

import java.util.ArrayList;

/**
 * Represents a single puzzle in an escape room.
 * Supports multiple puzzle types, attempt limits, hints, and answer validation rules.
 */
public class Puzzle {

    public enum PuzzleType {
        TEXT,
        MULTIPLE_CHOICE,
        TRUE_FALSE,
        MATH
    }

    public enum ValidationMode {
        EXACT,
        FLEXIBLE
    }

    private String id;
    private PuzzleType type;
    private String prompt;

    // Stored exactly as entered by creator
    private String answerRaw;

    private ValidationMode validationMode;
    private ArrayList<String> options;
    private String hint;
    private int maxAttempts;

    // Runtime state (should stay in sync with PlayerProgress)
    private int attemptsUsed;
    private int hintsUsed;
    private boolean solved;

    /**
     * Constructor for non-multiple-choice puzzles.
     */
    public Puzzle(String id,
                  PuzzleType type,
                  String prompt,
                  String answer,
                  ValidationMode validationMode,
                  String hint,
                  int maxAttempts) {

        this.id = (id == null) ? "" : id;
        this.type = (type == null) ? PuzzleType.TEXT : type;
        this.prompt = (prompt == null) ? "" : prompt;
        this.answerRaw = (answer == null) ? "" : answer;
        this.validationMode = (validationMode == null)
                ? ValidationMode.FLEXIBLE
                : validationMode;

        this.options = new ArrayList<>();
        this.hint = (hint == null) ? "" : hint;
        this.maxAttempts = Math.max(1, maxAttempts);

        resetProgress();
    }

    /**
     * Constructor for multiple choice puzzles.
     */
    public Puzzle(String id,
                  String prompt,
                  ArrayList<String> options,
                  String answer,
                  ValidationMode validationMode,
                  String hint,
                  int maxAttempts) {

        this(id, PuzzleType.MULTIPLE_CHOICE, prompt, answer, validationMode, hint, maxAttempts);

        if (options != null) {
            this.options.addAll(options);
        }
    }

    // -------------------------
    // Gameplay methods
    // -------------------------

    public boolean attemptAnswer(String userInput) {
        if (solved) return false;
        if (!hasAttemptsRemaining()) return false;

        String input = (userInput == null) ? "" : userInput;

        if (matchesAnswer(input)) {
            solved = true;
            return true;
        } else {
            attemptsUsed++;
            return false;
        }
    }

    public boolean canUseHint() {
        return !solved && hint.length() > 0;
    }

    public String useHint() {
        if (!canUseHint()) return "";
        hintsUsed++;
        return hint;
    }

    public boolean hasAttemptsRemaining() {
        return !solved && attemptsUsed < maxAttempts;
    }

    public int getAttemptsRemaining() {
        return Math.max(0, maxAttempts - attemptsUsed);
    }

    // -------------------------
    // Answer checking
    // -------------------------

    private boolean matchesAnswer(String userInput) {
        if (validationMode == ValidationMode.EXACT) {
            return userInput.equals(answerRaw);
        } else {
            return normalize(userInput).equals(normalize(answerRaw));
        }
    }

    private String normalize(String s) {
        return (s == null) ? "" : s.toLowerCase().trim();
    }

    // -------------------------
    // Getters
    // -------------------------

    public String getId() {
        return id;
    }

    public PuzzleType getType() {
        return type;
    }

    public String getPrompt() {
        return prompt;
    }

    public ArrayList<String> getOptions() {
        return new ArrayList<>(options);
    }

    public ValidationMode getValidationMode() {
        return validationMode;
    }

    public String getHint() {
        return hint;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public int getAttemptsUsed() {
        return attemptsUsed;
    }

    public int getHintsUsed() {
        return hintsUsed;
    }

    public boolean isSolved() {
        return solved;
    }

    public String getAnswerRaw() {
        return answerRaw;
    }

    // -------------------------
    // Setters (for loading saves)
    // -------------------------

    public void setSolved(boolean solved) {
        this.solved = solved;
    }

    public void setAttemptsUsed(int attemptsUsed) {
        this.attemptsUsed = Math.max(0, attemptsUsed);
    }

    public void setHintsUsed(int hintsUsed) {
        this.hintsUsed = Math.max(0, hintsUsed);
    }

    /**
     * Reset puzzle to an unsolved state.
     * Useful for "Start New" or restarting a room.
     */
    public void resetProgress() {
        this.attemptsUsed = 0;
        this.hintsUsed = 0;
        this.solved = false;
    }
}
