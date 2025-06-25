package com.example.memmorygame;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;

public class MemoryGame extends Application {

    private Stage primaryStage;
    private Scene gameScene;
    private Scene startScene;
    private Scene musicScene;

    // Game components
    private GameController gameController;
    private MusicManager musicManager;
    private StartPageController startPageController;
    private HighscoreManager highscoreManager;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Initialize managers and controllers
        this.musicManager = new MusicManager();
        this.musicManager.setMainApp(this); // Set the main app reference
        this.highscoreManager = new HighscoreManager();
        this.gameController = new GameController(this, musicManager, highscoreManager);
        this.startPageController = new StartPageController(this, musicManager, highscoreManager);

        // Create scenes
        createStartScene();
        createGameScene();

        // Set up window close behavior
        setupWindowCloseHandler();

        // Show start scene
        primaryStage.setScene(startScene);
        primaryStage.setTitle("Memory Game");
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (musicManager != null) {
            musicManager.cleanup();
        }
        if (gameController != null) {
            gameController.cleanup();
        }
        System.out.println("Application terminated. Resources cleaned up.");
    }

    private void createStartScene() {
        startScene = new Scene(startPageController.createStartPage(), 800, 600);
        // Apply dark mode styling
        startScene.getRoot().setStyle("-fx-background-color: #2b2b2b;");

        // Add keyboard shortcuts for start scene
        startScene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                startPageController.handleStartGame();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                startPageController.handleExit();
                event.consume();
            }
        });
    }

    private void createGameScene() {
        gameScene = new Scene(gameController.createGamePane(), 800, 600);
        gameScene.getRoot().setStyle("-fx-background-color: #2b2b2b;");

        // Add keyboard shortcuts for game scene
        gameScene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                gameController.showPauseMenu();
                event.consume();
            } else if (event.getCode() == KeyCode.F5) {
                gameController.startNewGame();
                event.consume();
            }
        });
    }

    private void setupWindowCloseHandler() {
        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            if (DialogUtils.showConfirmationDialog("Application Exit",
                    "Do you really want to exit the application?")) {
                if (musicManager != null) {
                    musicManager.cleanup();
                }
                if (gameController != null) {
                    gameController.cleanup();
                }
                primaryStage.close();
            }
        });
    }

    // Scene switching methods
    public void switchToGameView(String playerName, String difficulty) {
        gameController.initializeGame(playerName, difficulty);
        primaryStage.setScene(gameScene);
        primaryStage.setTitle("Memory Game - " + playerName + " (" + difficulty + ")");
    }

    public void switchToStartView() {
        primaryStage.setScene(startScene);
        primaryStage.setTitle("Memory Game");
    }

    public void showMusicManager() {
        // Always create a new scene to ensure the back button works
        musicScene = new Scene(musicManager.createMusicPane(), 800, 600);
        musicScene.getRoot().setStyle("-fx-background-color: #2b2b2b;");

        // Add keyboard shortcuts for music scene
        musicScene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SPACE) {
                musicManager.togglePlayPause();
                event.consume();
            } else if (event.getCode() == KeyCode.RIGHT) {
                musicManager.playNext();
                event.consume();
            } else if (event.getCode() == KeyCode.LEFT) {
                musicManager.playPrevious();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                switchToStartView();
                event.consume();
            }
        });

        primaryStage.setScene(musicScene);
        primaryStage.setTitle("Memory Game - Music Manager");
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}