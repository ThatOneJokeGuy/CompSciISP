package escapeRoomEngine;

/*
 * Name: Anoosh B.
 * Date: 9/1/2026
 * Description: the Room class is responsible for most of the logic and processing of
 * the escape room, including tasks like saving and loading the gamestate, handling the users movement,
 * handling the user solving puzzles, and handling the users inventory of items and clues.
 */

// import statements
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

public class Room {
	// a 2d array to manage the individual sections of the escape room,
	// otherwise known as its "cells"
	private Cell[][] cells;
	// keeps track of the current position on the map where the player is
	private int[] currentCell = new int[2];
	// the items the player currently has
	private ArrayList<Item> items = new ArrayList<Item>();
	// the clues the player currently has
	private ArrayList<Article> clues = new ArrayList<Article>();
	// the number of attempts the user has made on the puzzles in the escape room overall
	private int attempts = 0;
	// the current item the user is looking at in their inventory
	private int currentItem;

	
	public Cell getCell(int r, int c) {
		return cells[r][c];
	}
	
	public int getMapLength() {
		return cells[0].length;
	}
	
	public int getMapHeight() {
		return cells.length;
	}
	
	
	/**
	 * The locomote method will allow the user to travel between rooms after pressing
	 * the relevant buttons in the GUI and giving a direction of movement to this method.
	 * The user will be able to a specific cell given that it isn't a blank space 
	 * on the map.
	 */
	
	public boolean locomote(int r, int c) {
		
		if (cells[r][c] != null) {
			currentCell[0] = r;
			currentCell[1] = c;
			return true;
		}
		
		return false;
	}
	
    // Try to solve the current cell's puzzle with the provided answer
    public boolean solveCurrentCell(String answer) {
    	
    	Cell selectedCell = cells[currentCell[0]][currentCell[1]];
        boolean correct = ((selectedCell.getPuzzle()).solve(answer));
        
    	if (correct) {
    		selectedCell.getPuzzle().setSolved(true);
    		
    		if (selectedCell.givesItem()) {
    			items.add(selectedCell.getReward());
    		}
    		else {
    			clues.add(selectedCell.getReward());
    		}
    		
    		return true;
    	}
    	
    	return false;
    }
	
//	public void locomote(String dir) {
//		// initializes nextPosition as the currentCell position
//		int[] nextPosition = currentCell;
//		
//		// this switch statement examines the input to determine which
//		// direction the user will be moving, and adjusts the next position accordingly
//		switch (dir) {
//		case "up":
//			nextPosition[1]--;
//			break;
//		case "down":
//			nextPosition[1]++;
//			break;
//		case "left":
//			nextPosition[0]--;
//			break;
//		case "right":
//			nextPosition[0]++;
//			break;
//		}
//		
//		// checks if the next position is within the bounds of the map
//		if ((nextPosition[0] < cells.length) && (nextPosition[0] > -1) && 
//				(nextPosition[1] < cells[1].length) && (nextPosition[1] > -1)) {
//			
//			// checks if the next position is not an empty cell
//			if (cells[nextPosition[0]][nextPosition[1]] != null) {
//				// changes the users current position to where they were moving
//				currentCell = nextPosition;
//			}
//		}
//	}
    
    public void resetFile(File myFile) throws FileNotFoundException {
		// creates new PrintWriter PW to save the file
		PrintWriter PW = new PrintWriter(myFile);
		// adds the size of the map to the file
		PW.println((cells.length - 1) + "," + (cells[0].length - 1) + ",");
		// adds the current position of the player to the file
		PW.println(currentCell[0] + "," + currentCell[1] + ",");
		
		// prints out a colon to let the program know the user had no items when loading
		PW.println(":");
		
		// prints out a colon to let the program know the user had no clues when loading
		PW.println(":");
		
		// writes the number of room attempts to the file
		PW.println(0);
		
		// iterates through the map and saves the cells to the file
		for (int r = 0; r < cells.length; r++) {
			
			for (int c = 0; c < cells[r].length; c++) {
				
				if (cells[r][c] != null) {
					System.out.println("Calling save cells");
					resetCells(PW, r, c);
				}
			}
		}
		
		// closes the PrintWriters stream
		PW.close();
		
		load(myFile);
    }
    
    public void resetCells(PrintWriter PW, int r, int c) {
		// adds the position of the current cell on the map to the file
		PW.println(r + "," + c + ",");
		Cell beingSaved = cells[r][c];
		
		// gets the cells puzzle
		PuzzleModule cellPuzzle = beingSaved.getPuzzle();
		/*
		writes the various components of the current cells puzzle to the file,
		those being its name, description, puzzle type, attempts, and solution
		*/
		PW.println(cellPuzzle.getName());
		PW.println(cellPuzzle.getDesc());
		PW.println(cellPuzzle.getPuzzleType());
		// sets attempts to 0 automatically as the room is being reset
		PW.println(0);
		PW.println(cellPuzzle.getSolution());
		// sets puzzle and cell to being unsolved as room is being reset
		PW.println(false);
		
		/*
		writes the various components of the current cell to the file,
		those being its name, description, solve message, and item
		*/
		PW.println(beingSaved.getName());
		PW.println(beingSaved.getDesc());
		PW.println(beingSaved.getSolveMessage());
		PW.println(beingSaved.givesItem());
		
		// gets the cells item or clue
		Item cellItem = beingSaved.getReward();
		// writes the articles name to the file
		PW.println(cellItem.getName());
		// writes the articles description to the file
		PW.println(cellItem.getDesc());
		
		// checks if the article being saved is an item or a clue
		if (beingSaved.givesItem()) {
			// writes the items durability to the file
			PW.println(cellItem.canBreak());
		}
		
    }

	/**
	 * The load method will take in a file and load the game state saved within the file
	 */
	public void load(File myFile) throws FileNotFoundException {
		// initializes new scanner fsc (or file scanner) to read the file
		Scanner fsc = new Scanner(myFile);
		// initializes new variable currentLine to be able to store the lines being read by the scanner
		String currentLine = fsc.nextLine();
		// initializes the dimensions of the cells from the file
		cells = new Cell[Integer.parseInt((currentLine.split(","))[0]) + 1][Integer.parseInt((currentLine.split(","))[1]) + 1];

		// reads next line
		currentLine = fsc.nextLine();
		// sets the current position of the user to the one last saved in the file
		currentCell[0] = Integer.parseInt((currentLine.split(","))[0]);
		currentCell[1] = Integer.parseInt((currentLine.split(","))[1]);
		// reads next line
		currentLine = fsc.nextLine();

		// resets the users items and clues if they are potentially loading another
		// escape room or just in general to prevent errors or extra items
		items = new ArrayList<Item>();
		clues = new ArrayList<Article>();

		// checks if the user last saved with any items in their inventory
		if (!(currentLine.equals(":"))) {
			// Since the user has items to be loaded, the program fetches the
			// descriptions and breakability of the items from the file
			String descLine = fsc.nextLine();
			String canBreakLine = fsc.nextLine();
			// initializes the users item inventory
			initializeItems(currentLine, descLine, canBreakLine);
		}

		// reads next line
		currentLine = fsc.nextLine();
		System.out.println(currentLine);

		// checks if the user last saved with any items in their inventory
		if (!(currentLine.equals(":"))) {
			// Since the user had clues to be loaded, the program fetches the
			// descriptions of the clues from the file
			String descLine = fsc.nextLine();
			// initializes the users clue inventory
			initializeClues(currentLine, descLine);
		}

		// fetches the total number of puzzle attempts for the room
		attempts = Integer.parseInt(fsc.nextLine());
		System.out.println(attempts);
		// iterates through the rest of the file while there are rooms to load
		while (fsc.hasNext()) {
			initializeCell(fsc);
		}

		fsc.close();	
	}
	
	/**
	 * The save method will take in a file name save the current gamestate of that file
	 */
	public void save(File myFile) throws FileNotFoundException {
		// creates new PrintWriter PW to save the file
		PrintWriter PW = new PrintWriter(myFile);
		// adds the size of the map to the file
		PW.println((cells.length - 1) + "," + (cells[0].length - 1) + ",");
		// adds the current position of the player to the file
		PW.println(currentCell[0] + "," + currentCell[1] + ",");
		
		// checks if the user has at least 1 or more items to save
		if (items.size() > 0) {
			saveItems(PW);
		}
		// if the user had no items to save
		else {
			// prints out a colon to let the program know the user had no items when loading
			PW.println(":");
		}
		
		// checks if the user has at least 1 or more clues to save
		if (clues.size() > 0) {
			saveClues(PW);
		}
		else {
			// prints out a colon to let the program know the user had no clues when loading
			PW.println(":");
		}
		
		// writes the number of room attempts to the file
		PW.println(attempts);
		
		// iterates through the map and saves the cells to the file
		for (int r = 0; r < cells.length; r++) {
			
			for (int c = 0; c < cells[r].length; c++) {
				
				if (cells[r][c] != null) {
					System.out.println("Calling save cells");
					saveCells(PW, r, c);
				}
			}
		}
		
		// closes the PrintWriters stream
		PW.close();
	}
	
	/**
	 * The saveCells method takes in the printwriter, the row of the current cell being saved, 
	 * and the column, in order to properly save the cell to the file
	 */
	private void saveCells(PrintWriter PW, int r, int c) {
		// adds the position of the current cell on the map to the file
		PW.println(r + "," + c + ",");
		Cell beingSaved = cells[r][c];
		
		// gets the cells puzzle
		PuzzleModule cellPuzzle = beingSaved.getPuzzle();
		/*
		writes the various components of the current cells puzzle to the file,
		those being its name, description, puzzle type, attempts, and solution
		*/
		PW.println(cellPuzzle.getName());
		PW.println(cellPuzzle.getDesc());
		PW.println(cellPuzzle.getPuzzleType());
		PW.println(cellPuzzle.getAttempts());
		PW.println(cellPuzzle.getSolution());
		PW.println(cellPuzzle.isSolved());
		
		/*
		writes the various components of the current cell to the file,
		those being its name, description, solve message, and item
		*/
		PW.println(beingSaved.getName());
		PW.println(beingSaved.getDesc());
		PW.println(beingSaved.getSolveMessage());
		PW.println(beingSaved.givesItem());
		
		// gets the cells item or clue
		Item cellItem = beingSaved.getReward();
		// writes the articles name to the file
		PW.println(cellItem.getName());
		// writes the articles description to the file
		PW.println(cellItem.getDesc());
		
		// checks if the article being saved is an item or a clue
		if (beingSaved.givesItem()) {
			// writes the items durability to the file
			PW.println(cellItem.canBreak());
		}
		
	}
	
	/**
	 * the saveItems method is made for the explicit purpose of saving the items the
	 * player has currently attained to the file for the game state
	 */
	private void saveItems(PrintWriter PW) {
		/*
		initializes int saveCount to be used as a counter 
		to determine if the names, descriptions, or breakability of items
		are being saved next
		*/
		//int saveCount = 0;
		
		for (int i = 0; i < items.size(); i++) {
			PW.print((items.get(i)).getName() + ":");
		}
		PW.println();
		
		for (int i = 0; i < items.size(); i++) {
			PW.print((items.get(i)).getDesc() + ":");
		}
		PW.println();
		
		for (int i = 0; i < items.size(); i++) {
			PW.print((items.get(i)).canBreak() + ":");
		}
		PW.println();
		
	}
	
	/**
	 * the saveClues method is made for the explicit purpose of saving the clues the
	 * player has currently attained to the file for the game state
	 */
	private void saveClues(PrintWriter PW) {
		
		for (int i = 0; i < clues.size(); i++) {
			PW.print((clues.get(i)).getName() + ":");
		}
		PW.println();
		
		for (int i = 0; i < clues.size(); i++) {
			PW.print((clues.get(i)).getDesc() + ":");
		}
		PW.println();
		
	}

	private void initializeCell(Scanner fsc) {
		// gets the position of the cell in the 2d array from the file
		String[] cellPosition = (fsc.nextLine()).split(",");
		System.out.println("Cell position is " + cellPosition[0] + "," + cellPosition[1]);
		// gets the name of the puzzle in the cell
		String puzzleName = fsc.nextLine();
		// gets the description of the puzzle in the cell
		String puzzleDesc = fsc.nextLine();
		// gets the puzzle type of the puzzle from the file
		String puzzleType = fsc.nextLine();
		// gets the number of attempts of the puzzle in the file
		int puzzleAttempts = Integer.parseInt(fsc.nextLine());
		// gets the solution of the puzzle from the file
		String solution = fsc.nextLine();
		// gets if the cell has been solved or not
		boolean puzzleSolved = Boolean.parseBoolean(fsc.nextLine());
		// gets the cell name from the file
		String cellName = fsc.nextLine();
		// gets the cell description from the file
		String cellDesc = fsc.nextLine();
		// gets the solve message from the file
		String solveMessage = fsc.nextLine();
		// gets if the room gives an item or a key from the file
		boolean cellGivesItem = Boolean.parseBoolean(fsc.nextLine());
		// gets the name of the item and or clue from the file
		String articleName = fsc.nextLine();
		// gets the description of the item and or clue from the file
		String articleDesc = fsc.nextLine();
		// gets if the item is breakable or not from the file
		Boolean itemBreaks;

		// checks if the item given is an item or a clue
		if (cellGivesItem) {
			// fetches the breakable status from the file for the item
			itemBreaks = Boolean.parseBoolean(fsc.nextLine());
		}
		else {
			// sets itemBreaks to null as this isn't an item and is a clue
			itemBreaks = false;
		}
		
		PuzzleModule cellPuzzle = new Riddle(puzzleName, puzzleDesc, puzzleType, solution, puzzleAttempts, puzzleSolved);
		
		if (puzzleType.equalsIgnoreCase("combination")) {
			cellPuzzle = new Combination(puzzleName, puzzleDesc, puzzleType, solution, puzzleAttempts, puzzleSolved);

		}
		else if (puzzleType.equalsIgnoreCase("obstacle")) {
			cellPuzzle = new Obstacle(puzzleName, puzzleDesc, puzzleType, solution, puzzleAttempts, puzzleSolved);
		}
		

		// creates an object for the cell's puzzle
		System.out.println(cellPuzzle.getSolution());
		// creates a sample item for the cell's reward
		Item cellReward = new Item(articleName, articleDesc, itemBreaks);
		// adds the completed cell to the cells 2d array
		cells[Integer.parseInt(cellPosition[0])][Integer.parseInt(cellPosition[1])] = new Cell(cellName, cellDesc, solveMessage, puzzleSolved, cellGivesItem, cellReward, cellPuzzle);
		System.out.println(cells[Integer.parseInt(cellPosition[0])][Integer.parseInt(cellPosition[1])]);
	}

	/**
	 * This method will take in 3 strings to use for initializing the items
	 * the user has from the loaded session.
	 */
	private void initializeItems(String nameLine, String descLine, String canBreakLine) {

		// splits the nameLine to get a string array of names
		String[] names = nameLine.split(":");
		String[] descriptions = descLine.split(":");
		String[] canBreaks = canBreakLine.split(":");

		// iterates for the number of items to initialize
		for (int i = 0; i < names.length; i++) {
			// adds each item the user had saved to the items ArrayList one by one
			items.add(new Item(names[i], descriptions[i], Boolean.parseBoolean(canBreaks[i])));
		}
	}

	/**
	 * This method will take in 2 strings to use for initializing the clues
	 * the user has from the loaded session.
	 */
	private void initializeClues(String nameLine, String descLine) {

		String[] names = nameLine.split(":");
		String[] descriptions = descLine.split(":");

		// iterates for the number of items to initialize
		for (int i = 0; i < names.length; i++) {
			// adds each item the user had saved to the items ArrayList one by one
			clues.add(new Article(names[i], descriptions[i]));
		}
	}
	
	public ArrayList<Item> getItems() { return items; }
	
	public ArrayList<Article> getClues() { return clues; }
	
	public int[] getCurrentCell() { return currentCell; }

	public void removeItem(String itemName) {
		
		for (int i = 0; i < items.size(); i++) {
			Item currentItem = items.get(i);
			
			if (currentItem.getName().equals(itemName)) {
				
				if (currentItem.canBreak()) {
					items.remove(i);
				}
				
				break;
			}
		}
	}

}
