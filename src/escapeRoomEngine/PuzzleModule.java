package escapeRoomEngine;

public class PuzzleModule {

	public static String desc = "";
	public static String name = "";
	public static String solution = "";
	public static int attempts = 0;
	public static boolean solved = false;
	
	public void createPuzzle(String puzzleType, String winCondition, int id, int puzzleID) {
		super();
	}
	
	public void solve(String attempt) {
		
	}
	
	public String getName() { return name; }
	public String getDescription() { return desc; }
	public boolean isSolved() { return solved; }
	public int getAttempt() { return attempts; }
}
