package escapeRoomEngine;


public class Combination extends PuzzleModule{
	
	public Combination(String name, String desc, String puzzleType, String solution, int attemptsToSolve, boolean solved) {
		super(name, desc, puzzleType, solution, attemptsToSolve, solved);
	}


	@Override
	public boolean solve(String attempt) {
			if (attempt.equals(super.getSolution())) { //checks if attempt equals solution
				super.setSolved(true);
			}
			else { //checks if attempt doesn't equal solution
				super.setSolved(false); //sets solved to false
			}
			return super.isSolved(); //returns false
		}

}
