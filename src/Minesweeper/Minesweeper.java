package Minesweeper;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Random;


/**
 * This is a custom Java implementation of Microsoft's Minesweeper Game.  It
 * has the capabilities of supporting three different difficulty levels, and
 * functions just as Minesweeper is expected to:
 * <ul>
 * <li>Reveals all blank spaces if no mines are present immediately nearby</li>
 * <li>Has the ability to flag and un-flag mines on right click</li>
 * <li>Includes a timer, for the user's reference</li>
 * <li>Game ends when all but the mines are revealed, or a mine is clicked, though
 * a new game can be started at any time.</li>
 * </ul>
 * 
 * @author rodenbe4
 *
 */
public class Minesweeper {

	// Needed to keep Eclipse happy, though it will not be used anywhere.
	@SuppressWarnings("unused")
	private static final long serialVersionUID = 1L;

	// Interactive components
	private JFrame mainFrame, selectionFrame;
	private JPanel gridPanel, topPanel, selectionPanel, subPanel;
	private JLabel minesLabel, timerLabel, selectionLabel;
	private JButton newGame, select, cancel;
	private JButton[][] buttons;
	private JMenuBar menuBar;
	private JMenu menu;
	private JMenuItem newGameMenuItem, exitMenuItem, difficultyMenuItem;
	private JRadioButton easy, medium, hard;
	private ButtonGroup radioGroup;
		
	// Arrays to keep track of each mine's current status
	private boolean[][] mines;
	private boolean[][] isClicked;
	private boolean[][] isFlagged;
	
	// Constant variables for setting the difficulty
	private final int[] BUTTON_COLS = {9, 16, 30};
	private final int[] BUTTON_ROWS = {9, 16, 16};
	private final int[] NUM_MINES = {10, 40, 99};
		
	// The difficulty-dependent variables
	private int cols;
	private int rows;
	private int totalMines;
	private int minesFlagged = 0;
	private int safeSpaces = 0;
	private Difficulty difficulty = Difficulty.EASY;
	
	// Keeping track of the status of the game
	private boolean gameStarted = false;
	private boolean lostGame = false;
	private boolean difficultyChanged = false;

	// Variables for the timer
	private long minutes = 0;
	private long seconds = 0;
		
	// Images needed
	ImageIcon smiley = new ImageIcon(this.getClass().getResource("/resources/smileys.png"));
	ImageIcon winner = new ImageIcon(this.getClass().getResource("/resources/smiley_win.png"));
	ImageIcon loser = new ImageIcon(this.getClass().getResource("/resources/smiley_lose.png"));
	ImageIcon mine = new ImageIcon(this.getClass().getResource("/resources/mine.png"));
	ImageIcon error = new ImageIcon(this.getClass().getResource("/resources/mine_bad.png"));
	ImageIcon flag = new ImageIcon(this.getClass().getResource("/resources/flag.png"));	
	
	// Colors
	Color[] colors = {null, Color.BLUE, new Color(0,128,0), Color.RED, 
			new Color(0,0,160), new Color(128,0,128), Color.BLACK, Color.DARK_GRAY};
	
	/**
	 * Instantiate the Minesweeper game
	 */
	public Minesweeper() {
		makeGUI();
	}
	
	/**
	 * Create the GUI
	 */
	private void makeGUI() {
		// Set up the initial JFrame
		mainFrame = new JFrame();
		mainFrame.setTitle("Minesweeper");
		mainFrame.setBounds(0, 0, 600, 700);
		mainFrame.setLocationRelativeTo(null);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		mainFrame.setLayout(new BorderLayout());
		
		// Assess the default difficulty and generate the initial buttons to the grid
		assessDifficulty();
		generateButtons();
		
		// Set up the top panel bar (mines remaining, new game button, and timer go here)
		topPanel = new JPanel();
		topPanel.setLayout(new GridLayout(1,3));
		
		// Create label used to indicate number mines left to be flagged
		minesLabel = new JLabel();
		minesLabel.setFont(new Font("Arial", Font.BOLD, 40));
		minesLabel.setHorizontalAlignment(JLabel.LEFT);
		topPanel.add(minesLabel);
		
		// Create the new game button
		newGame = new JButton();
		newGame.setBackground(Color.WHITE);
		newGame.setPreferredSize(new Dimension(45, 45));
		newGame.setHorizontalAlignment(JLabel.CENTER);
		topPanel.add(newGame);
		
		// Create the label used to show the timer
		timerLabel = new JLabel();
		timerLabel.setFont(new Font("Arial", Font.BOLD, 40));
		timerLabel.setHorizontalAlignment(JLabel.RIGHT);
		topPanel.add(timerLabel);
		
		// Initialize the other variables and add the appropriate listener to the new game button
		newGame();
		newGame.addActionListener((ActionEvent e)->newGame());
		
		//  Create the menu bar
		menuBar = new JMenuBar();
		menu = new JMenu("File");
		menu.setFont(new Font("Arial", Font.BOLD, 16));
		
		// Add the new game menu option
		newGameMenuItem = new JMenuItem("New Game",KeyEvent.VK_T);
		newGameMenuItem.setFont(new Font("Arial", Font.PLAIN, 16));
		newGameMenuItem.addActionListener(newGameListener);
		menu.add(newGameMenuItem);
		
		// Add the difficulty menu option 
		menu.addSeparator();
		difficultyMenuItem = new JMenuItem("Difficulty",KeyEvent.VK_T);
		difficultyMenuItem.setFont(new Font("Arial", Font.PLAIN, 16));
		difficultyMenuItem.addActionListener((ActionEvent event)->selectionFrame.setVisible(true));
		menu.add(difficultyMenuItem);
		
		// Add the exit game menu option
		menu.addSeparator();
		exitMenuItem = new JMenuItem("Exit",KeyEvent.VK_T);
		exitMenuItem.setFont(new Font("Arial", Font.PLAIN, 16));
		exitMenuItem.addActionListener(exitListener);
		menu.add(exitMenuItem);
		
		
		// Add the components to the frame and set visibility
		menuBar.add(menu);
		mainFrame.setJMenuBar(menuBar);
		
		mainFrame.add(topPanel, BorderLayout.NORTH);
		mainFrame.add(gridPanel, BorderLayout.CENTER);
		mainFrame.pack();
		mainFrame.setVisible(true);
		
		// Now set up the additional frame for the difficulty selection
		selectionFrame = new JFrame();
		selectionFrame.setTitle("Difficulty");
		selectionFrame.setLocationRelativeTo(null);
		selectionFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		selectionFrame.setLayout(new BorderLayout());
		
		selectionPanel = new JPanel();
		selectionPanel.setLayout(new BoxLayout(selectionPanel, BoxLayout.Y_AXIS));
		radioGroup = new ButtonGroup();
		
		easy = new JRadioButton("Easy", true);
		easy.setFont(new Font("Arial", Font.PLAIN, 16));
		medium = new JRadioButton("Intermediate", false);
		medium.setFont(new Font("Arial", Font.PLAIN, 16));
		hard = new JRadioButton("Hard", false);
		hard.setFont(new Font("Arial", Font.PLAIN, 16));
		radioGroup.add(easy);
		radioGroup.add(medium);
		radioGroup.add(hard);
		
		selectionLabel = new JLabel("Please select the difficulty you'd like to play:");
		selectionLabel.setFont(new Font("Arial", Font.PLAIN, 16));
		selectionFrame.add(selectionLabel, BorderLayout.NORTH);
		
		selectionPanel.add(easy);
		selectionPanel.add(medium);
		selectionPanel.add(hard);
		
		subPanel = new JPanel();
		subPanel.setLayout(new GridLayout(1,2));
		
		select = new JButton("OK");
		select.addActionListener(difficultyListener);
		select.setFont(new Font("Arial", Font.BOLD, 16));
		subPanel.add(select);
		
		cancel = new JButton("Cancel");
		cancel.addActionListener((ActionEvent event)->selectionFrame.setVisible(false));
		cancel.setFont(new Font("Arial", Font.BOLD, 16));
		subPanel.add(cancel);
		
		selectionFrame.add(subPanel, BorderLayout.SOUTH);
		selectionPanel.setAlignmentY(JLabel.LEFT);
		
		selectionFrame.add(selectionPanel, BorderLayout.CENTER);
		selectionFrame.pack();
	}
	
	/**
	 * Determines the difficulty that the game is currently set at, and sets the
	 * number of rows, columns, and mines accordingly.
	 */
	private void assessDifficulty() {
		if (difficulty == Difficulty.EASY) {
			rows = BUTTON_ROWS[0];
			cols = BUTTON_COLS[0];
			totalMines = NUM_MINES[0];
		} else if (difficulty == Difficulty.MEDIUM) {
			rows = BUTTON_ROWS[1];
			cols = BUTTON_COLS[1];
			totalMines = NUM_MINES[1];
		} else {
			rows = BUTTON_ROWS[2];
			cols = BUTTON_COLS[2];
			totalMines = NUM_MINES[2];
		}
	}
	
	/**
	 * Creates the buttons on the game board, depending on the difficulty selected.
	 */
	private void generateButtons() {
		// Remove the grid panel if it has previously been instantiated
		if (gridPanel != null) mainFrame.remove(gridPanel);
		
		// Recreate the panel with the new buttons, then add it back to the main frame
		gridPanel = new JPanel();
		buttons = new JButton[rows][cols];
		gridPanel.setLayout(new GridLayout(rows,cols));
		mainFrame.add(gridPanel, BorderLayout.CENTER);
		
		// generate all of the buttons
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				buttons[i][j] = new JButton();
				buttons[i][j].setPreferredSize(new Dimension(42, 42));
				buttons[i][j].setFont(new Font("Arial", Font.BOLD, 14));
				gridPanel.add(buttons[i][j]);
			}
		}
		mainFrame.pack();
	}
	
	/**
	 * Adds the appropriate number mines to the games based on random generations 
	 * of a row and column number.  Two mines cannot exist that have the same 
	 * row and column, nor is the very first click of the game allowed to be a mine.
	 * 
	 * @param row  The row number corresponding to the first click
	 * @param col  The column number corresponding to the first click
	 */
	private void generateMines(int row, int col) {
		int count = 0;
		Random rnd = new Random();
		while (count < totalMines) {
			int r = rnd.nextInt(rows);
			int c = rnd.nextInt(cols);
			if (!mines[r][c] && buttons[r][c] != buttons[row][col]) {
				mines[r][c] = true;
				++count;
			}
		}
		timer.start();
		//printMines();
	}
	
	/**
	 * Sets the New Game icon to the default and sets up the game anew.
	 */
	private void newGame() {
		// Reset the timer
		timer.stop();
		minutes = 0;
		seconds = 0;
		
		// Check the difficulty and generate the appropriate buttons
		assessDifficulty();
		if (difficultyChanged) {
			generateButtons();
			difficultyChanged = false;
		}
		safeSpaces = cols * rows - totalMines;
		minesFlagged = 0;
		
		// Reset the buttons and labels
		newGame.setIcon(smiley);
		minesLabel.setText(Integer.toString(totalMines));
		timerLabel.setText("0:00");
		
		gameStarted = false;
		lostGame = false;
		mines = new boolean[rows][cols];
		isClicked = new boolean[rows][cols];
		isFlagged = new boolean[rows][cols];

		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				buttons[i][j].addMouseListener(mouseListener);
				buttons[i][j].setBackground(null);
				buttons[i][j].setText(null);
				buttons[i][j].setIcon(null);
			}
		}
	}
	
	/**
	 *   For debugging purposes only.  Prints to the console where each mine is located at.
	 */
	void printMines() {
		System.out.println("WHERE DA MINES AT!?");
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (mines[i][j]) {
					System.out.println(i + ", " + j);
				}
			}
		}
	}
	
	/**
	 * Set the color of the text of a button.
	 * 
	 * @param row The row of the button being changed.
	 * @param col The column of the button being changed.
	 * @param count The number of mines that are located nearby.
	 */
	private void setColor(int row, int col, int count) {
		buttons[row][col].setForeground(colors[count]);
	}
	
	// This is the original event listener I created before adding the ability to flag the mines
	ActionListener buttonListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent event) {
			int row = 0;
			int col = 0;
			Object src = event.getSource();
			for (row = 0; row < rows; ++row) {
				for (col = 0; col < cols; ++col) {
					if (buttons[row][col] == src) click(row,col);
				}
			}		
		}
	};
	
	
	// Timer for the game, updated every second.
	Timer timer = new Timer(1000, new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			++seconds;
			if (seconds < 10) {
				timerLabel.setText(minutes + ":0" + seconds);
			} else if (seconds == 60) {
				++minutes;
				seconds = 0;
				timerLabel.setText(minutes + ":0" + seconds);
			} else {
				timerLabel.setText(minutes + ":" + seconds);
			}
		}
		
	});
	
	// The listener to be added to each button click
	MouseListener mouseListener = new MouseAdapter() {
		public void mouseClicked(MouseEvent event) {
			int row = 0;
			int col = 0;
			Object src = event.getSource();
			for (row = 0; row < rows; ++row) {
				for (col = 0; col < cols; ++col) {
					if (buttons[row][col] == src) {
						if (SwingUtilities.isRightMouseButton(event)) {
							flagMine(row,col);
						} else if (SwingUtilities.isLeftMouseButton(event)) {
							click(row,col);
						}
					}
				}
			}
			
		}
	};
	
	// Listener that allows for the selection of a different difficulty
	ActionListener difficultyListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent event) {
			Difficulty newDifficulty;
			
			if (easy.isSelected()) {
				newDifficulty = Difficulty.EASY;
			} else if (medium.isSelected()) {
				newDifficulty = Difficulty.MEDIUM;
			} else {
				newDifficulty = Difficulty.HARD;
			}
			
			if (newDifficulty != difficulty) {
				if (gameStarted && !lostGame && !checkWinner()) {
					int selection = JOptionPane.showConfirmDialog(null,
							"Are you sure you want to change the difficulty? This will end your current game.",
							"Warning",JOptionPane.YES_NO_OPTION);
					if (selection == 0) {
						difficultyChanged = true;
						difficulty = newDifficulty;
						newGame();
						selectionFrame.setVisible(false);
					}
				} else {
					difficultyChanged = true;
					difficulty = newDifficulty;
					newGame();
					selectionFrame.setVisible(false);
				}
			} else {
				selectionFrame.setVisible(false);
			}
		}
		
	};
	
	// Listener that is executed whenever the New Game button is selected from the File menu
	ActionListener newGameListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent event) {
			if (gameStarted && !lostGame && !checkWinner()) {
				int selection = JOptionPane.showConfirmDialog(null,
						"Are you sure you want to start a new game?","Warning",JOptionPane.YES_NO_OPTION);
				if (selection == 0) {
					newGame();
				}
			}
		}
	};
	
	// Listener that is executed whenever the Exit button is selected from the File menu
	ActionListener exitListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent event) {
			int selection = JOptionPane.showConfirmDialog(null,
					"Are you sure you want to quit?","Warning",JOptionPane.YES_NO_OPTION);
			if (selection == 0) {
				System.exit(0);
			}
		}
	};
	
	/**
	 * Systematically removes all actions from the buttons.  Triggered by either
	 * a game win or game loss.  If the game has been lost, all of the locations
	 * of the mines are displayed to the user, unless they were flagged correctly
	 * by the user.
	 */
	private void endGame() {
		timer.stop();
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				buttons[i][j].removeMouseListener(mouseListener);
				if (mines[i][j] && lostGame && !isFlagged[i][j]) {
					buttons[i][j].setIcon(mine);
				} else if (!mines[i][j] && lostGame && isFlagged[i][j]) {
					buttons[i][j].setIcon(error);
				}
			}
		}
	}
	
	/**
	 * Adds or removes a flag from a button.  If no game is currently
	 * being played, or the space has already been clicked on, nothing
	 * happens.  Alternatively, if the number of flags set matches the
	 * number of mines in the game, no new flag can be set until another
	 * one is removed.
	 * 
	 * @param row The row location of where the flag should go.
	 * @param col The column location of where the flag should go.
	 */
	private void flagMine(int row, int col) {
		if (!gameStarted) return;
		if (isClicked[row][col]) return;
		
		if (isFlagged[row][col]) {
			buttons[row][col].setIcon(null);
			--minesFlagged;
		} else { 
			if (minesFlagged == totalMines) return;
			buttons[row][col].setIcon(flag);
			++minesFlagged;
		}
		
		isFlagged[row][col] = !isFlagged[row][col];	
		minesLabel.setText(Integer.toString(totalMines - minesFlagged));
	}
	 
	/**
	 * <p>Performs a series of actions upon click of a button.  These actions may
	 * result in the loss of a game, a simple unveiling of a space, or a game
	 * win.
	 * <br>
	 * If the game has been reset and we're making the first move of the game,
	 * the mines are generated before we can continue with our first move.</p>
	 * 
	 * @param row The row of the clicked location in the grid.
	 * @param col The column of the clicked location in the grid.
	 */
	private void click(int row, int col) {
		if (!gameStarted) {
			generateMines(row, col);
			gameStarted = true;
		}
		
		if (isFlagged[row][col]) return;

		if (mines[row][col]) {
			loseGame(row, col);
		} else {
			reveal(row, col);
		}
	}
	
	/**
	 * Checks whether a given row and column location of a button is within the 
	 * bounds of the grid.
	 * 
	 * @param row The row we're checking.
	 * @param col The column we're checking.
	 * @return    Whether the row and column correspond to a button in the grid.
	 */
	private boolean withinBounds(int row, int col) {
		if (row < 0 || row >= rows) return false;
		if (col < 0 || col >= cols) return false;
		return true;
	}
	
	/**
	 * Checks how many mines are surrounding a particular location in the grid.
	 * 
	 * @param row The row of the location we're checking.
	 * @param col The column of the location we're checking.
	 * @return    The number of mines surrounding the current location.
	 */
	private int checkSurrounding(int row, int col) {
		int count = 0;
		
		for (int i = row - 1; i <= row + 1; ++i) {
			for (int j = col - 1; j <= col + 1; ++j) {
				if (withinBounds(i, j)) {
					if (mines[i][j]) ++count;
				}
			}
		}
		return count;
	}
	
	
	/**
	 * Unveil the space that was clicked.  This could either be a blank space, in 
	 * which case all surrounding blank spaces are revealed, or a single space
	 * reveal that indicates how many mines are nearby.
	 * 
	 * @param row The row of the location we're looking to reveal.
	 * @param col The column of the location we're looking to reveal.
	 */
	private void reveal(int row, int col) {
		if (!withinBounds(row, col)) return;
		if (isClicked[row][col]) return;
		if (mines[row][col]) return;
		if (isFlagged[row][col]) return;
		
		int count = checkSurrounding(row,col);
		
		--safeSpaces;
		setColor(row,col,count);
		buttons[row][col].setBackground(Color.WHITE);
		isClicked[row][col] = true;
		
		if (count > 0) {
			buttons[row][col].setText(count + "");
		} else {
			for (int i = row - 1; i <= row + 1; ++i) {
				for (int j = col - 1; j <= col + 1; ++j) {
					reveal(i,j);
				}
			}
		}
		
		if (checkWinner()) {
			winGame();
		}
	}
	
	/**
	 * Simple method to check and see if the game has been won.
	 * 
	 * @return Whether the user won the game or not.
	 */
	private boolean checkWinner() {
		if (safeSpaces == 0) return true;
		return false;
	}
	
	/**
	 * Displays information regarding a game win to the user, and removes
	 * all actions from the game buttons.
	 */
	private void winGame() {
		newGame.setIcon(winner);
		endGame();
		JOptionPane.showMessageDialog(null, "Congratulations, you win!");
	}
	
	/**
	 * Displays information regarding a game loss to the user, and removes
	 * all actions from the game buttons.  The mine that was clicked is 
	 * highlighted in red.
	 * 
	 * @param row The row location of the clicked mine.
	 * @param col The column location of the clicked mine.
	 */
	private void loseGame(int row, int col) {
		lostGame = true;
		newGame.setIcon(loser);
		endGame();
		buttons[row][col].setBackground(Color.RED);
		JOptionPane.showMessageDialog(null, "Game over!");
	}
	
	/**
	 * Run the program.
	 */
	public static void main(String[] args) {
		new Minesweeper();
	}
}

/**
 * A simple Enumeration for keeping track of the difficulty.
 *
 */
enum Difficulty {
	EASY, MEDIUM, HARD
}

