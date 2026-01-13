package escapeRoomEngine;

public class PuzzleModule {

	private String desc = "";
	private String name = "";
	private String puzzleType = "";
	private String solution = "";
	private int attempts = 0;
	private int attemptsToSolve = 0;
	private int puzzleID = 0;
	private boolean solved = false;
	
	public PuzzleModule(String name, String desc, String puzzleType, String solution, int attemptsToSolve, int puzzleID, boolean needsItem, boolean solved) {
		this.name = name;
		this.desc = desc;
		this.puzzleType = puzzleType;
		this.solved = solved;
		this.attemptsToSolve = attemptsToSolve;
		this.puzzleID = puzzleID;	
	}
	
	public boolean solve(String attempt) {
		if (attempt.equals(solution)) {
			solved = true;
		}
		else {
			solved = false;
		}
		return solved;
	}
	
	public String getName() { return name; }
	public String getDescription() { return desc; }
	public boolean isSolved() { return solved; }
	public int getAttempts() { return attempts; }
}
