package com.example.memorygame;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;

/**
 * Controller für die Startseite des Spiels.
 */
public class StartPageController {

    private MemoryGame mainApp;
    private MusicManager musicManager;
    private HighscoreManager highscoreManager;

    private TextField nameInput;
    private ComboBox<String> difficultyComboBox;

    /**
     * Konstruktor.
     * @param mainApp Referenz auf die Hauptanwendung.
     * @param musicManager Musikmanager.
     * @param highscoreManager Highscoremanager.
     */
    public StartPageController(MemoryGame mainApp, MusicManager musicManager, HighscoreManager highscoreManager) {
        this.mainApp = mainApp;
        this.musicManager = musicManager;
        this.highscoreManager = highscoreManager;
    }

    /**
     * Erstellt die Startseite als VBox.
     * @return VBox mit UI-Elementen.
     */
    public VBox createStartPage() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(50));
        root.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Memory Game");
        titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Label nameLabel = new Label("Enter your name:");
        nameLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #ffffff;");

        nameInput = new TextField();
        nameInput.setPromptText("Your name");
        nameInput.setMaxWidth(200);
        nameInput.setStyle("-fx-font-size: 14px;");

        Label difficultyLabel = new Label("Select difficulty:");
        difficultyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #ffffff;");

        difficultyComboBox = new ComboBox<>();
        difficultyComboBox.getItems().addAll("Einfach", "Mittel", "Schwer");
        difficultyComboBox.setValue("Mittel");
        difficultyComboBox.setStyle("-fx-font-size: 14px;");

        Button startButton = new Button("Start Game");
        startButton.setStyle("-fx-font-size: 16px; -fx-min-width: 120px;");
        startButton.setOnAction(e -> handleStartGame());

        Button highscoreButton = new Button("High Scores");
        highscoreButton.setStyle("-fx-font-size: 16px; -fx-min-width: 120px;");
        highscoreButton.setOnAction(e -> showHighscores());

        Button musicButton = new Button("Music");
        musicButton.setStyle("-fx-font-size: 16px; -fx-min-width: 120px;");
        musicButton.setOnAction(e -> mainApp.showMusicManager());

        Button exitButton = new Button("Exit");
        exitButton.setStyle("-fx-font-size: 16px; -fx-min-width: 120px;");
        exitButton.setOnAction(e -> handleExit());

        HBox buttonBox = new HBox(10, startButton, highscoreButton, musicButton, exitButton);
        buttonBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(titleLabel, nameLabel, nameInput, difficultyLabel,
                difficultyComboBox, buttonBox);

        return root;
    }

    /**
     * Startet das Spiel mit den eingegebenen Einstellungen.
     */
    public void handleStartGame() {
        String playerName = nameInput.getText().trim();
        if (playerName.isEmpty()) {
            DialogUtils.showError("Input Error", "Please enter your name!");
            return;
        }

        String difficulty = difficultyComboBox.getValue();
        mainApp.switchToGameView(playerName, difficulty);
    }

    /**
     * Zeigt die Highscores an.
     */
    private void showHighscores() {
        Alert highscoreAlert = new Alert(Alert.AlertType.INFORMATION);
        highscoreAlert.setTitle("High Scores");
        highscoreAlert.setHeaderText("Best Times");

        String highscoreText = highscoreManager.getFormattedHighscores();
        highscoreAlert.setContentText(highscoreText);

        highscoreAlert.showAndWait();
    }

    /**
     * Beendet die Anwendung nach Captcha-Abfrage.
     */
    public void handleExit() {
        if (DialogUtils.showConfirmationDialog("Exit Application",
                "Do you really want to exit the game?")) {
            if (mainApp != null && mainApp.getClass().getSimpleName().equals("MemoryGame")) {
                try {
                    java.lang.reflect.Method captchaMethod = mainApp.getClass().getDeclaredMethod("showCaptchaDialog");
                    captchaMethod.setAccessible(true);
                    boolean captchaOk = (boolean) captchaMethod.invoke(mainApp);
                    if (captchaOk) {
                        System.exit(0);
                    }
                } catch (Exception e) {
                    DialogUtils.showError("Fehler", "Captcha konnte nicht angezeigt werden.");
                }
            } else {
                System.exit(0);
            }
        }
    }
}
