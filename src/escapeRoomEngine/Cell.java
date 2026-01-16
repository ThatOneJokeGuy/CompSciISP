/*
 * Name: Anoosh B.
 * Date: 9/1/2026
 * Description: An escape room is made up of multiple cells, each of which
 * have a puzzle associated with them. A cell has a name, description, and can be locked
 * or unlocked, which can control the progression of the game.
 */

public class Cell {
	
	// the puzzle associated with this cell
	private PuzzleModule cellPuzzle;
	// gives the cell a specific name
	private String name;
	// gives a description for the cell
	private String desc;
	// a message that replaces the description when the cell is solved
	private String solveMessage;
	// a boolean that correlates to if the room has been solved or not
	private boolean solved;
	// a boolean that controls if a cell gives an item or a clue
	private boolean giveItem;
	// the item or clue the user will recieve upon completing the puzzle
	private Item reward;
	
	public Cell(String name, String desc, String solveMessage, boolean solved, boolean giveItem, Item reward, PuzzleModule cellPuzzle) {
		this.name = name;
		this.desc = desc;
		this.solveMessage = solveMessage;
		this.solved = solved;
		this.giveItem = giveItem;
		this.reward = reward;
		this.cellPuzzle = cellPuzzle;
	}
	
	// returns the puzzle of the cell
	public PuzzleModule getPuzzle() { return cellPuzzle; }
	
	// returns the name of the cell
	public String getName() { return name; }
	
	// returns the description of the cell
	public String getDesc() { return desc; }
	
	// returns the solve message of the cell
	public String getSolveMessage() { return solveMessage; }
	
	// returns if the cell is solved or not
	public boolean isSolved() { return solved; }
	
	// returns if the cell gives an item or an article
	public boolean givesItem() { return giveItem; }
	
	// returns the reward of the cell
	public Item getReward() { return reward; }
	

}
