package escapeRoomEngine;

/*
 * Names: Braydon Castle, Anoosh B., Colin
 * Date: 2026-01-15
 * Description: This class is the main engine for the escape room game. 
 * It handles the user interface, game logic, and interactions between the player and the game world.
 */

import javax.swing.*;
import javax.swing.border.Border;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

// Main UI + controller for the escape room application.
// Handles building the Swing interface, relaying user actions to the Room model,
// and updating the view when the model changes.
public class Engine extends JFrame implements ActionListener {

    // Core game model: the Room being edited/played and whether it's loaded
    private Room escapeRoom = new Room();
    private boolean roomLoaded;

    // Top area: label showing escape room name
    private JPanel ERNamePanel = new JPanel();
    private JLabel ERNameLabel = new JLabel("unloaded");

    // Layout panels for the main window
    private JPanel bottomPanel = new JPanel(); // not used yet
    private JPanel leftPanel = new JPanel();   // contains the grid and cell info
    private JPanel rightPanel = new JPanel();  // contains controls, inventory, puzzles

    // Grid of buttons representing room cells (left side)
    private JPanel buttonGridPanel = new JPanel();
    private JButton[][] buttonGrid = new JButton[10][10];
    private Dimension buttonGridDimension = new Dimension(25, 25);

//  private GridBagLayout gbl = new GridBagLayout();
//  private GridBagConstraints gbc = new GridBagConstraints();
   
    // Right-side controls (tabs, save/load)
    private JButton puzzleButton = new JButton("Puzzle Menu");
    private JButton inventoryButton = new JButton("Inventory");
    private JPanel puzzleItemPanel = new JPanel();

    // Cell information display under the grid
    private JPanel cellInformation = new JPanel();
    private JLabel cellName = new JLabel("No cell loaded");
    private JLabel cellDesc = new JLabel("Description: ");

    // Panel that holds either the Inventory UI or Puzzle UI (uses CardLayout)
    private JPanel selectedBox = new JPanel();

    // Save / Load controls
    private JLabel space = new JLabel("            ");
    private JButton saveButton = new JButton("Save");
    private JButton loadButton = new JButton("Load");
    private JTextField fileNameTF = new JTextField("File Name Goes Here", 14); // compact width
    private JPanel saveLoadPanel = new JPanel();
    private JPanel topSaveLoadPanel = new JPanel();
    private JPanel bottomSaveLoadPanel = new JPanel();

    private JLabel fileNameL = new JLabel("File Name: ");

    // CardLayout to switch between Inventory and Puzzle views inside selectedBox
    private CardLayout cardLayout = new CardLayout();
    private JPanel cardPanel = new JPanel(cardLayout);

    // Inventory UI components
    private JComboBox<String> itemBox = new JComboBox<>();
    private JLabel itemInfo = new JLabel(" ");
    private JComboBox<String> clueBox = new JComboBox<>();
    private JLabel clueInfo = new JLabel(" ");

    // Puzzle UI components
    private String[] numList = {"1","2","3","4","5","6","7","8","9","0"};
    private JLabel puzzleTitle = new JLabel("Puzzle: ");
    private JLabel puzzleDesc = new JLabel(" ");
    private JComboBox<String> comboLockNum1 = new JComboBox<String>(numList);
    private JComboBox<String> comboLockNum2 = new JComboBox<String>(numList);
    private JComboBox<String> comboLockNum3 = new JComboBox<String>(numList);
    private JComboBox<String> comboLockNum4 = new JComboBox<String>(numList);
    private JTextField answerTF = new JTextField(); // user types puzzle answers here
    private JButton submitAnswerButton = new JButton("Submit");
    private JLabel puzzleResult = new JLabel(" ");

    // Constructor: build the UI and wire listeners (no game loaded yet)
    public Engine() {
        roomLoaded = false;

        // The window changes sizes but the Layout stays the same
        setLayout(new BorderLayout());

        // Top: name panel
        ERNamePanel.add(ERNameLabel);
        ERNamePanel.setPreferredSize(new Dimension(800, 40));
        add(ERNamePanel, BorderLayout.NORTH);

        // Left: grid and cell info
        leftPanel.setLayout(new BorderLayout());
        buttonGridPanel.setLayout(new GridLayout(10, 10));
        initializeGrid(); // create 10x10 buttons and add listeners
        leftPanel.add(buttonGridPanel, BorderLayout.CENTER);

        cellInformation.setLayout(new GridLayout(1, 2));
        cellInformation.add(cellName);
        cellInformation.add(cellDesc);
        cellInformation.setPreferredSize(new Dimension(1, 40));
        leftPanel.add(cellInformation, BorderLayout.SOUTH);

        // Right: top buttons (puzzle/inventory), middle card, bottom save/load
        rightPanel.setLayout(new BorderLayout());

        puzzleItemPanel.setLayout(new GridLayout(1, 2));
        puzzleItemPanel.add(puzzleButton);
        puzzleItemPanel.add(inventoryButton);
        puzzleItemPanel.setPreferredSize(new Dimension(260, 40));
        rightPanel.add(puzzleItemPanel, BorderLayout.NORTH);

        puzzleButton.addActionListener(this);
        inventoryButton.addActionListener(this);

        // Middle area for Inventory/Puzzle “tab"
        setupSelectedBoxTabs();
        rightPanel.add(selectedBox, BorderLayout.CENTER);

        // Save/Load area at bottom (so fileNameTF always visible)
        saveLoadPanel.setLayout(new GridLayout(2, 1));

        topSaveLoadPanel.add(saveButton);
        saveButton.addActionListener(this);
        topSaveLoadPanel.add(space);
        topSaveLoadPanel.add(loadButton);
        loadButton.addActionListener(this);

        bottomSaveLoadPanel.add(fileNameL);
        bottomSaveLoadPanel.add(fileNameTF);

        saveLoadPanel.add(topSaveLoadPanel);
        saveLoadPanel.add(bottomSaveLoadPanel);
        saveLoadPanel.setPreferredSize(new Dimension(260, 90));
        rightPanel.add(saveLoadPanel, BorderLayout.SOUTH);

        // Give the right side a fixed width so it doesn't get squeezed away
        rightPanel.setPreferredSize(new Dimension(280, 1));

        add(leftPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setSize(800, 500);
        setVisible(true);
    }

    // Simple main to open the window
    public static void main(String[] args) {
        new Engine();
    }

    // Create the 10x10 grid of JButtons and add ActionListeners
    public void initializeGrid() {

        // initialize each button slot so there is a clickable grid.
        for (int r = 0; r < buttonGrid.length; r++) {

            for (int c = 0; c < buttonGrid[r].length; c++) {
                buttonGrid[r][c] = new JButton();
                buttonGrid[r][c].setPreferredSize(buttonGridDimension);
                buttonGrid[r][c].addActionListener(this); // route clicks to actionPerformed
                buttonGridPanel.add(buttonGrid[r][c]);
            }
        }
    }

    // Update visual states of grid buttons and cell info based on model
    public void updateGrid() {
        int rows = escapeRoom.getMapHeight();
        int cols = escapeRoom.getMapLength();
        int[] currentPos = escapeRoom.getCurrentCell();

        // For each defined cell in the room, set color depending on state
        for (int r = 0; r < rows; r++) {

            for (int c = 0; c < cols; c++) {

                if (escapeRoom.getCell(r, c) == null) {
                    // empty / no cell -> default background
                    buttonGrid[r][c].setBackground(null);
                }
                else {
                    if ((escapeRoom.getCell(r, c)).isSolved()) {
                        // solved cells are green
                        buttonGrid[r][c].setBackground(Color.GREEN);
                    }
                    else {
                        // unsolved, present puzzle -> red
                        buttonGrid[r][c].setBackground(Color.RED);
                    }
                }
            }

            // Update info for the currently selected cell
            System.out.println(currentPos[0] + "," + currentPos[1]);
            System.out.println(escapeRoom.getCell(currentPos[0], currentPos[1]));
            cellName.setText((escapeRoom.getCell(currentPos[0], currentPos[1])).getName());

            if ((escapeRoom.getCell(currentPos[0], currentPos[1])).isSolved()) {
                cellDesc.setText((escapeRoom.getCell(currentPos[0], currentPos[1])).getSolveMessage());
            }
            else {
                cellDesc.setText((escapeRoom.getCell(currentPos[0], currentPos[1])).getDesc());
            }

            // Highlight the player's current position in yellow
            buttonGrid[currentPos[0]][currentPos[1]].setBackground(Color.YELLOW);
        }

        // Keep puzzle panel synced to current cell
        refreshPuzzlePanel();
    }

    // Central event handler for buttons and UI actions
    public void actionPerformed(ActionEvent e) {
        // initializes component as the component which triggered the action listener
        Object component = e.getSource();

        // Inventory shows in the selectedBox area
        if (component.equals(inventoryButton)) {
            refreshInventoryPanel();
            cardLayout.show(cardPanel, "INV");
            return;
        }

        // Puzzle menu shows in the selectedBox area
        if (component.equals(puzzleButton)) {
            refreshPuzzlePanel();
            cardLayout.show(cardPanel, "PUZ");
            return;
        }

        
        
        // Submit answer button inside puzzle panel
        if (component.equals(submitAnswerButton)) {
        	
        	int[] pos = escapeRoom.getCurrentCell();
            Cell cell = escapeRoom.getCell(pos[0], pos[1]);
        	
            if (roomLoaded) {
                // attempt to solve current cell's puzzle using typed answer
            	String attemptedAnswer = null;
            	if ((cell.getPuzzle()).getPuzzleType().equals("combination")) {
            		int num1 = comboLockNum1.getSelectedIndex() + 1;
            		int num2 = comboLockNum2.getSelectedIndex() + 1;
            		int num3 = comboLockNum3.getSelectedIndex() + 1;
            		int num4 = comboLockNum4.getSelectedIndex() + 1;
            		attemptedAnswer = Integer.toString(num1) + Integer.toString(num2) + Integer.toString(num3) + Integer.toString(num4);
            		System.out.println(attemptedAnswer);
            	}
            	else {
            		attemptedAnswer = answerTF.getText();
            	}
            	boolean solved = escapeRoom.solveCurrentCell(attemptedAnswer);
                
                if (solved) {
                    cell.setSolved(solved);
                    puzzleResult.setText(cell.getSolveMessage());
                    
                    if ((cell.getPuzzle()).getPuzzleType().equals("obstacle")) {
                    	escapeRoom.removeItem(answerTF.getText());
                    }
                }
                else {
                    puzzleResult.setText("Incorrect.");
                }

                updateGrid();
                refreshInventoryPanel(); // if reward was granted, update dropdowns
            }
            
            return;
        }

        // Load button: read room from src/<filename>.txt
        if (component.equals(loadButton)) {

            try {
                File myFile = new File("src/" + fileNameTF.getText() + ".txt");
                escapeRoom.load(myFile);

                ERNameLabel.setText(fileNameTF.getText());
                roomLoaded = true;

                updateGrid();

                // after loading, refresh and show puzzle view by default
                refreshInventoryPanel();
                refreshPuzzlePanel();
                cardLayout.show(cardPanel, "PUZ");
            }
            catch (FileNotFoundException ex) {
                System.out.println("File not found!");
                roomLoaded = false;
            }

        }
        else if (saveButton.equals(component) && (roomLoaded)) {

            // Save button: write room to src/<filename>.txt
            try {
                File myFile = new File("src/" + fileNameTF.getText() + ".txt");
                escapeRoom.save(myFile);
            }
            catch (FileNotFoundException ex) {
                System.out.println("Error! File cannot be saved!");
            }

        }
        else {

            // Grid button was clicked: attempt to move player there
            if (roomLoaded) {
                JButton buttonClicked = (JButton)component;

                for (int r = 0; r < buttonGrid.length; r++) {

                    for (int c = 0; c < buttonGrid[r].length; c++) {

                        if (buttonGrid[r][c].equals(buttonClicked)) {
                            boolean moved = escapeRoom.locomote(r, c);

                            if (moved) {
                                updateGrid();
                            }
                        }
                    }
                }
            }
        }

    }

    // Build the Inventory and Puzzle panels and wire their internal listeners
    private void setupSelectedBoxTabs() {
        selectedBox.setLayout(new BorderLayout());
        selectedBox.add(cardPanel, BorderLayout.CENTER);

        // -------- Inventory Panel --------
        JPanel invPanel = new JPanel(new GridLayout(0, 1, 6, 6));
        invPanel.add(new JLabel("Items:"));
        invPanel.add(itemBox);
        invPanel.add(itemInfo);

        invPanel.add(new JLabel("Clues:"));
        invPanel.add(clueBox);
        invPanel.add(clueInfo);

        // When an item is selected show its details
        itemBox.addActionListener(ev -> {
            int idx = itemBox.getSelectedIndex();
            ArrayList<Item> items = escapeRoom.getItems();
            if (idx >= 0 && idx < items.size()) {
                Item it = items.get(idx);
                itemInfo.setText(it.getName() + " - " + it.getDesc());
            } else {
                itemInfo.setText(" ");
            }
        });

        // When a clue is selected show its details
        clueBox.addActionListener(ev -> {
            int idx = clueBox.getSelectedIndex();
            ArrayList<Article> clues = escapeRoom.getClues();
            if (idx >= 0 && idx < clues.size()) {
                Article cl = clues.get(idx);
                clueInfo.setText(cl.getName() + " - " + cl.getDesc());
            } else {
                clueInfo.setText(" ");
            }
        });

        // Puzzle panel
        JPanel puzzlePanel = new JPanel(new GridLayout(0, 1, 6, 6));
        puzzlePanel.add(puzzleTitle);
        puzzlePanel.add(puzzleDesc);
        puzzlePanel.add(new JLabel("Answer:"));
        puzzlePanel.add(comboLockNum1);
        puzzlePanel.add(comboLockNum2);
        puzzlePanel.add(comboLockNum3);
        puzzlePanel.add(comboLockNum4);
        puzzlePanel.add(answerTF);
        puzzlePanel.add(submitAnswerButton);
        puzzlePanel.add(puzzleResult);

        submitAnswerButton.addActionListener(this);

        // Add panels to card layout
        cardPanel.add(invPanel, "INV");
        cardPanel.add(puzzlePanel, "PUZ");

        // Default view before load
        cardLayout.show(cardPanel, "PUZ");
    }

    // Rebuild inventory lists into the combo boxes
    private void refreshInventoryPanel() {
        // rebuild items list
        itemBox.removeAllItems();
        ArrayList<Item> items = escapeRoom.getItems();
        for (int i = 0; i < items.size(); i++) {
            itemBox.addItem(items.get(i).getName());
        }

        // rebuild clues list
        clueBox.removeAllItems();
        ArrayList<Article> clues = escapeRoom.getClues();
        for (int i = 0; i < clues.size(); i++) {
            clueBox.addItem(clues.get(i).getName());
        }

        // set display text
        if (items.size() > 0) {
            itemBox.setSelectedIndex(0);
        } else {
            itemInfo.setText("No items.");
        }

        if (clues.size() > 0) {
            clueBox.setSelectedIndex(0);
        } else {
            clueInfo.setText("No clues.");
        }
    }

    // Update the puzzle panel to show the puzzle for the current cell
    private void refreshPuzzlePanel() {
        if (!roomLoaded) return;

        comboLockNum1.setVisible(false);
    	comboLockNum2.setVisible(false);
    	comboLockNum3.setVisible(false);
    	comboLockNum4.setVisible(false);
        int[] pos = escapeRoom.getCurrentCell();
        Cell cell = escapeRoom.getCell(pos[0], pos[1]);
        
        if (escapeRoom.getCell(pos[0], pos[1]).getPuzzle().getPuzzleType().equals("combination")) {
        	comboLockNum1.setSelectedIndex(9);
        	comboLockNum2.setSelectedIndex(9);
        	comboLockNum3.setSelectedIndex(9);
        	comboLockNum4.setSelectedIndex(9);
        	comboLockNum1.setVisible(true);
        	comboLockNum2.setVisible(true);
        	comboLockNum3.setVisible(true);
        	comboLockNum4.setVisible(true);
        	answerTF.setVisible(false);
        }
        else {
        	answerTF.setVisible(true);
            answerTF.setText("");
        }
        submitAnswerButton.setVisible(true);

        if (cell == null) {
            puzzleTitle.setText("Puzzle: (none)");
            puzzleDesc.setText(" ");
            puzzleResult.setText(" ");
            return;
        }

        PuzzleModule pz = cell.getPuzzle();
        puzzleTitle.setText("Puzzle: " + pz.getName());
        // <html> for formatting multi-line descriptions
        puzzleDesc.setText("<html>" + pz.getDesc() + "</html>");

        // show the cell's solved message if already solved
        if (cell.isSolved()) {
            puzzleResult.setText(cell.getSolveMessage());
            comboLockNum1.setVisible(false);
            comboLockNum2.setVisible(false);
            comboLockNum3.setVisible(false);
            comboLockNum4.setVisible(false);
            answerTF.setVisible(false);
            submitAnswerButton.setVisible(false);
        } else {
            puzzleResult.setText(" ");
        }
    }
    
}