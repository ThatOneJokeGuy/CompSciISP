package escapeRoomEngine;

/*
 * Name (Made By): Braydon Castle
 * Date: Jan 7th - 16th, 2026
 * Description: This class holds the information regarding each puzzle type, although it does not contain any form
 * of logic for the puzzles
 */
 
public class PuzzleModule { //class identifier

	//object variables
	private String desc = "";
	private String name = "";
	private String puzzleType = "";
	private String solution = "";
	private int attempts = 0;
	private int attemptsToSolve = 0;
	private int puzzleID = 0;
	private boolean solved = false;
	
	//constructor method
	public PuzzleModule(String name, String desc, String puzzleType, String solution, int attemptsToSolve, boolean solved) {
		this.name = name;
		this.desc = desc;
		this.puzzleType = puzzleType;
		this.solved = solved;
		this.attemptsToSolve = attemptsToSolve;
		this.puzzleID = puzzleID;	
	}
	
	//method for handling the solving of a puzzle
	public boolean solve(String attempt) {
		if (attempt.equals(solution)) { //checks if attempt equals solution
			solved = true; //sets solved to true
		}
		else { //checks if attempt doesn't equal solution
			solved = false; //sets solved to false
		}
		return solved; //returns false
	}
	
	//all of the getter and setter methods for the class
	public String getName() { return name; }
	public String getDesc() { return desc; }
	public String getPuzzleType() { return puzzleType; }
	public String getSolution() { return solution; }
	public boolean isSolved() { return solved; }
	public int getAttempts() { return attempts; }
	public void setSolved(boolean solved) {this.solved = solved; }
}
