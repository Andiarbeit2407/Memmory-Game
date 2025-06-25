package com.example.memmorygame;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

public class GameController {

    private MemoryGame mainApp;
    private MusicManager musicManager;
    private HighscoreManager highscoreManager;

    // Game state
    private GameModel gameModel;
    private GameBoard gameBoard;
    private GameTimer gameTimer;

    // UI components
    private BorderPane gameLayout;
    private GridPane gameGrid;
    private HBox controls;
    private Label scoreLabel;
    private Label timeLabel;

    // Current game settings
    private String currentPlayerName = "";
    private String currentDifficulty = "Mittel";

    public GameController(MemoryGame mainApp, MusicManager musicManager, HighscoreManager highscoreManager) {
        this.mainApp = mainApp;
        this.musicManager = musicManager;
        this.highscoreManager = highscoreManager;
        this.gameModel = new GameModel();
        this.gameTimer = new GameTimer(this::updateTimeDisplay);
    }

    public VBox createGamePane() {
        gameLayout = new BorderPane();
        gameLayout.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Initialize empty grid
        gameGrid = new GridPane();
        gameGrid.setAlignment(Pos.CENTER);
        gameLayout.setCenter(gameGrid);

        // Create UI labels
        scoreLabel = new Label("Attempts: 0");
        scoreLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        timeLabel = new Label("Time: 00:00");
        timeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Create control buttons
        Button newGameButton = new Button("New Game");
        newGameButton.setOnAction(e -> startNewGame());

        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction(e -> showPauseMenu());

        Button backToStartButton = new Button("Main Menu");
        backToStartButton.setOnAction(e -> {
            if (DialogUtils.showConfirmationDialog("Main Menu",
                    "Do you want to end the current game and return to the main menu?")) {
                gameTimer.stop();
                mainApp.switchToStartView();
            }
        });

        controls = new HBox(20, scoreLabel, timeLabel, newGameButton, pauseButton, backToStartButton);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(20));
        gameLayout.setBottom(controls);

        VBox gameRootVBox = new VBox();
        gameRootVBox.getChildren().add(gameLayout);
        VBox.setVgrow(gameLayout, Priority.ALWAYS);

        return gameRootVBox;
    }

    public void initializeGame(String playerName, String difficulty) {
        this.currentPlayerName = playerName;
        this.currentDifficulty = difficulty;

        // Configure game settings based on difficulty
        GameSettings settings = GameSettings.fromDifficulty(difficulty);
        gameModel.initialize(settings);

        // Create game board
        gameBoard = new GameBoard(settings.getGridSize(), settings.getButtonSize(), this::onButtonClicked);
        buildGameGrid();

        // Reset UI
        updateScoreDisplay();
        updateTimeDisplay();

        // Start timer
        gameTimer.start();
    }

    public void startNewGame() {
        gameTimer.stop();
        GameSettings settings = GameSettings.fromDifficulty(currentDifficulty);
        gameModel.initialize(settings);
        gameBoard.reset(gameModel.getSymbols());
        updateScoreDisplay();
        gameTimer.start();
    }

    public void showPauseMenu() {
        gameTimer.pause();

        Alert pauseAlert = new Alert(Alert.AlertType.CONFIRMATION);
        pauseAlert.setTitle("Game Paused");
        pauseAlert.setHeaderText("The game is paused");
        pauseAlert.setContentText("What would you like to do?");

        ButtonType resumeButton = new ButtonType("Continue");
        ButtonType newGameButton = new ButtonType("New Game");
        ButtonType mainMenuButton = new ButtonType("Main Menu");

        pauseAlert.getButtonTypes().setAll(resumeButton, newGameButton, mainMenuButton);

        pauseAlert.showAndWait().ifPresentOrElse(result -> {
            if (result == resumeButton) {
                gameTimer.resume();
            } else if (result == newGameButton) {
                startNewGame();
            } else if (result == mainMenuButton) {
                gameTimer.stop();
                mainApp.switchToStartView();
            }
        }, () -> gameTimer.resume());
    }

    private void buildGameGrid() {
        gameGrid = new GridPane();
        gameGrid.setAlignment(Pos.CENTER);
        gameGrid.setHgap(10);
        gameGrid.setVgap(10);

        Button[][] buttons = gameBoard.getButtons();
        int gridSize = gameModel.getSettings().getGridSize();

        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                gameGrid.add(buttons[i][j], j, i);
            }
        }

        gameLayout.setCenter(gameGrid);
    }

    private void onButtonClicked(int row, int col) {
        if (!gameModel.canClick(row, col)) {
            return;
        }

        gameBoard.revealButton(row, col, gameModel.getSymbol(row, col));
        gameModel.addClickedPosition(row, col);

        if (gameModel.getClickedCount() == 2) {
            handleTwoButtonsClicked();
        }
    }

    private void handleTwoButtonsClicked() {
        gameBoard.disableAllButtons();
        gameModel.incrementAttempts();
        updateScoreDisplay();

        PauseTransition pause = new PauseTransition(Duration.seconds(gameModel.getSettings().getPauseTime()));
        pause.setOnFinished(event -> {
            int[] pos1 = gameModel.getClickedPosition(0);
            int[] pos2 = gameModel.getClickedPosition(1);

            if (gameModel.isMatch(pos1[0], pos1[1], pos2[0], pos2[1])) {
                // Match found
                gameBoard.markAsFound(pos1[0], pos1[1]);
                gameBoard.markAsFound(pos2[0], pos2[1]);
                gameModel.markAsRevealed(pos1[0], pos1[1]);
                gameModel.markAsRevealed(pos2[0], pos2[1]);
                gameModel.incrementPairs();

                if (gameModel.isGameWon()) {
                    handleGameWon();
                    return;
                }
            } else {
                // No match
                gameBoard.hideButton(pos1[0], pos1[1]);
                gameBoard.hideButton(pos2[0], pos2[1]);
            }

            gameModel.clearClickedPositions();
            gameBoard.enableUnrevealedButtons(gameModel.getRevealedPositions());
        });

        pause.play();
    }

    private void handleGameWon() {
        gameTimer.stop();
        gameBoard.disableAllButtons();

        Platform.runLater(() -> {
            highscoreManager.saveScore(currentPlayerName, currentDifficulty,
                    gameTimer.getElapsedTimeInSeconds(), gameModel.getAttempts());

            Alert winAlert = new Alert(Alert.AlertType.CONFIRMATION);
            winAlert.setTitle("Game Finished!");
            winAlert.setHeaderText("Congratulations! You found all pairs!");
            winAlert.setContentText("Time: " + gameTimer.getFormattedTime() +
                    "\nAttempts: " + gameModel.getAttempts() +
                    "\n\nWould you like to start a new game or return to the main menu?");

            ButtonType newGameButton = new ButtonType("New Game");
            ButtonType mainMenuButton = new ButtonType("Main Menu", ButtonBar.ButtonData.CANCEL_CLOSE);

            winAlert.getButtonTypes().setAll(newGameButton, mainMenuButton);

            winAlert.showAndWait().ifPresentOrElse(result -> {
                if (result == newGameButton) {
                    startNewGame();
                } else {
                    mainApp.switchToStartView();
                }
            }, () -> mainApp.switchToStartView());
        });
    }

    private void updateScoreDisplay() {
        scoreLabel.setText("Attempts: " + gameModel.getAttempts());
    }

    private void updateTimeDisplay() {
        timeLabel.setText("Time: " + gameTimer.getFormattedTime());
    }

    public void cleanup() {
        if (gameTimer != null) {
            gameTimer.stop();
        }
    }
}