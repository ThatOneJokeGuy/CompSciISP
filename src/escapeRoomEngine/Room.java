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
	private ArrayList<Item> items;
	// the clues the player currently has
	private ArrayList<Article> clues;
	// the number of attempts the user has made on the puzzles in the escape room overall
	private int attempts = 0;
	// the current item the user is looking at in their inventory
	private int currentItem;

	/**
	 * The locomote method will allow the user to travel between rooms after pressing
	 * the relevant buttons in the GUI and giving a direction of movement to this method.
	 * The user will be able to move, up, down, left, and right on the map, given that a specific cell
	 * isn't a blank space on the map or out of bounds of the map.
	 */
	public void locomote(String dir) {
		// initializes nextPosition as the currentCell position
		int[] nextPosition = currentCell;
		
		// this switch statement examines the input to determine which
		// direction the user will be moving, and adjusts the next position accordingly
		switch (dir) {
		case "up":
			nextPosition[1]--;
			break;
		case "down":
			nextPosition[1]++;
			break;
		case "left":
			nextPosition[0]--;
			break;
		case "right":
			nextPosition[0]++;
			break;
		}
		
		// checks if the next position is within the bounds of the map
		if ((nextPosition[0] < cells.length) && (nextPosition[0] > -1) && 
				(nextPosition[1] < cells[1].length) && (nextPosition[1] > -1)) {
			
			// checks if the next position is not an empty cell
			if (cells[nextPosition[0]][nextPosition[1]] != null) {
				// changes the users current position to where they were moving
				currentCell = nextPosition;
			}
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
		cells = new Cell[Integer.parseInt((currentLine.split(","))[0])][Integer.parseInt((currentLine.split(","))[1])];

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
		PW.println(cells.length + "," + cells[0].length + ",");
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
		
		// initializes variables r and c for use of iterating through the cells
		int r = 0;
		int c = 0;
		
		// iterates through the map and saves the cells to the file
		while ((r < cells.length) && (c < cells[r].length)) {
			
			// if the current cell is not empty
			if (cells[r][c] != null) {
				// saves the cell to the file
				saveCells(PW, r, c);
			}
			
			// adds 1 to the column counter
			c++;
			
			// if the column of the next iteration would be out of bounds for the 2d array
			if ((c > cells[r].length)) {
				// adds 1 to row counter
				r++;
				// sets the column counter to 0
				c = 0;
			}
		}
		
		// closes the PrintWriters stream
		PW.close();
	}
	
	/**
	 * The saveCells method takes in the printwriter, the row of the current cell being saved, 
	 * and the column, in order to properly save the cell to the file
	 */
	public void saveCells(PrintWriter PW, int r, int c) {
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
	public void saveItems(PrintWriter PW) {
		/*
		initializes int saveCount to be used as a counter 
		to determine if the names, descriptions, or breakability of items
		are being saved next
		*/
		int saveCount = 0;
		
		// iterates through the users items and saves their names, descriptions,
		// and whether they can break or not to the file
		for (int i = 0; i < items.size(); i++) {
			
			// checks if its currently saving the names of items
			if (saveCount == 0) {
				PW.print((items.get(i)).getName() + "|");
			}
			// checks if its currently saving the descriptions of items
			else if (saveCount == 1) {
				PW.print((items.get(i)).getDesc() + "|");
			}
			// goes here if its currently saving the durability of items
			else {
				PW.print((items.get(i)).canBreak() + "|");
			}
			
			// checks if it needs to update the counter after saving all the names or descriptions
			if ((saveCount == items.size()) && (saveCount < 2)) {
				i = 0;
				saveCount++;
				PW.println();
			}
		}
		
		PW.println();
	}
	
	/**
	 * the saveClues method is made for the explicit purpose of saving the clues the
	 * player has currently attained to the file for the game state
	 */
	public void saveClues(PrintWriter PW) {
		/*
		initializes int saveCount to be used as a counter 
		to determine if the names or descriptions of clues are being saved next
		*/
		int saveCount = 0;
		
		// iterates through the users clues and saves their names and descriptions to the file
		for (int i = 0; i < items.size(); i++) {
			
			// checks if its currently saving the names of clues
			if (saveCount == 0) {
				PW.print((clues.get(i)).getName() + "|");
			}
			// checks if its currently saving the descriptions of clues
			else {
				PW.print((clues.get(i)).getDesc() + "|");
			}
			
			// checks if it needs to update the counter after saving all the names
			if ((saveCount == clues.size()) && (saveCount < 1)) {
				i = 0;
				saveCount++;
				PW.println();
			}
		}
		
		PW.println();
	}

	public void initializeCell(Scanner fsc) {
		// gets the position of the cell in the 2d array from the file
		String[] cellPosition = (fsc.nextLine()).split(",");
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
			itemBreaks = null;
		}

		// creates an object for the cell's puzzle
		PuzzleModule cellPuzzle = new PuzzleModule(puzzleName, puzzleDesc, puzzleType, solution, puzzleAttempts, puzzleSolved);
		// creates a sample item for the cell's reward
		Item cellReward = new Item(articleName, articleDesc, itemBreaks);
		// adds the completed cell to the cells 2d array
		cells[Integer.parseInt(cellPosition[0])][Integer.parseInt(cellPosition[1])] = new Cell(cellName, cellDesc, solveMessage, puzzleSolved, cellGivesItem, cellReward, cellPuzzle);
	}

	/**
	 * This method will take in 3 strings to use for initializing the items
	 * the user has from the loaded session.
	 */
	public void initializeItems(String nameLine, String descLine, String canBreakLine) {

		// splits the nameLine to get a string array of names
		String[] names = nameLine.split("|");
		String[] descriptions = descLine.split("|");
		String[] canBreaks = canBreakLine.split("|");

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
	public void initializeClues(String nameLine, String descLine) {

		String[] names = nameLine.split("|");
		String[] descriptions = descLine.split("|");

		// iterates for the number of items to initialize
		for (int i = 0; i < names.length; i++) {
			// adds each item the user had saved to the items ArrayList one by one
			clues.add(new Article(names[i], descriptions[i]);
		}
	}

}
