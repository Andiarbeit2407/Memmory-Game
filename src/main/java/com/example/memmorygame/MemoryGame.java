package com.example.memmorygame;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.*;
import java.util.*;

public class MemoryGame extends Application {

    private Stage primaryStage;
    private Scene gameScene;
    private Scene startScene;

    // Memory-Spiel Attribute
    private Button[][] buttons = new Button[4][4];
    private String[][] werte = new String[4][4];
    private boolean[][] aufgedeckt = new boolean[4][4];
    private List<Button> geklickteButtons = new ArrayList<>();
    private int paare = 0;
    private Label scoreLabel;
    private int versuche = 0;
    private String[] symbole = {"A", "B", "C", "D", "E", "F", "G", "H"};
    private String currentPlayerName = "";
    private String currentDifficulty = "Mittel";
    private double pauseTime = 1.0;

    // Musik-Manager Attribute
    private ObservableList<Song> playlist = FXCollections.observableArrayList();
    private MediaPlayer musicPlayer;
    private ListView<Song> songListView;
    private Slider volumeSlider;
    private Label currentSongLabel;
    private ProgressBar progressBar;
    private boolean isShuffleMode = false;
    private boolean isRepeatMode = false;
    private int currentSongIndex = 0;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Memory Game & Musik Manager");

        startScene = new Scene(createStartPage(), 600, 800);

        TabPane tabPane = new TabPane();
        Tab gameTab = new Tab("Memory Spiel");
        gameTab.setClosable(false);
        gameTab.setContent(createGamePane());

        Tab musicTab = new Tab("Musik Manager");
        musicTab.setClosable(false);
        musicTab.setContent(createMusicPane());

        tabPane.getTabs().addAll(gameTab, musicTab);
        gameScene = new Scene(tabPane, 600, 800);

        gameScene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case SPACE -> pauseMusic();
                case RIGHT -> playNextSong();
                case LEFT -> playPreviousSong();
                case ESCAPE -> showPauseMenu();
            }
        });

        // Hinzufügen des CAPTCHA-Dialogs vor dem Schließen
        primaryStage.setOnCloseRequest(event -> {
            // Zuerst fragen, ob der Benutzer wirklich beenden möchte
            if (showConfirmationDialog("Beenden", "Möchten Sie das Spiel wirklich beenden?")) {
                // Wenn ja, dann das CAPTCHA anzeigen
                if (!showCaptchaDialog()) {
                    // Wenn das CAPTCHA falsch ist oder abgebrochen wird, das Beenden abbrechen
                    event.consume();
                } else {
                    // Wenn CAPTCHA korrekt, Musik-Player disposten
                    if (musicPlayer != null) {
                        musicPlayer.dispose();
                    }
                    // Das System wird standardmäßig beendet, da event.consume() nicht aufgerufen wurde
                }
            } else {
                // Wenn der Benutzer den ersten Bestätigungsdialog abbricht, das Beenden abbrechen
                event.consume();
            }
        });

        primaryStage.setScene(startScene);
        primaryStage.show();

        loadPlaylist();
    }

    private VBox createStartPage() {
        VBox startRoot = new VBox(20);
        startRoot.setAlignment(Pos.CENTER);
        startRoot.setPadding(new Insets(40));
        startRoot.setStyle("-fx-background-color: #f0f0f0;");

        Label titleLabel = new Label("Memory & Musik");
        titleLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label subtitleLabel = new Label("Das ultimative Gedächtnisspiel mit Musikbegleitung");
        subtitleLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #34495e;");

        TextField playerNameField = new TextField();
        playerNameField.setPromptText("Dein Name");
        playerNameField.setMaxWidth(300);
        playerNameField.setStyle("-fx-font-size: 16px;");

        ComboBox<String> difficultyBox = new ComboBox<>();
        difficultyBox.getItems().addAll("Leicht", "Mittel", "Schwer");
        difficultyBox.setValue("Mittel");
        difficultyBox.setStyle("-fx-font-size: 16px;");

        Button startButton = new Button("Spiel starten");
        startButton.setStyle("""
            -fx-font-size: 18px;
            -fx-background-color: #2ecc71;
            -fx-text-fill: white;
            -fx-padding: 10 20 10 20;
            -fx-background-radius: 5;
            """);

        startButton.setOnMouseEntered(e ->
                startButton.setStyle("""
                -fx-font-size: 18px;
                -fx-background-color: #27ae60;
                -fx-text-fill: white;
                -fx-padding: 10 20 10 20;
                -fx-background-radius: 5;
                """)
        );

        startButton.setOnMouseExited(e ->
                startButton.setStyle("""
                -fx-font-size: 18px;
                -fx-background-color: #2ecc71;
                -fx-text-fill: white;
                -fx-padding: 10 20 10 20;
                -fx-background-radius: 5;
                """)
        );


        Button highscoreButton = new Button("Bestenliste");
        highscoreButton.setStyle("""
            -fx-font-size: 16px;
            -fx-background-color: #3498db;
            -fx-text-fill: white;
            -fx-padding: 8 15 8 15;
            -fx-background-radius: 5;
            """);


        VBox buttonsBox = new VBox(15);
        buttonsBox.setAlignment(Pos.CENTER);
        buttonsBox.getChildren().addAll(
                playerNameField,
                new Label("Schwierigkeitsgrad:"),
                difficultyBox,
                startButton,
                highscoreButton
        );

        Label creditsLabel = new Label("© 2025 Memory Game");
        creditsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        startRoot.getChildren().addAll(
                titleLabel,
                subtitleLabel,
                new Separator(),
                buttonsBox,
                creditsLabel
        );

        startButton.setOnAction(e -> {
            String playerName = playerNameField.getText().trim();
            if (playerName.isEmpty()) {
                showError("Fehler", "Bitte gib deinen Namen ein!");
                return;
            }
            switchToGameView(playerName, difficultyBox.getValue());
        });

        highscoreButton.setOnAction(e -> showHighscores());

        return startRoot;
    }

    private VBox createGamePane() {
        VBox gameRoot = new VBox(20);
        gameRoot.setAlignment(Pos.CENTER);
        gameRoot.setPadding(new Insets(20));

        scoreLabel = new Label("Versuche: 0 | Paare gefunden: 0/8");
        scoreLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(5);
        grid.setVgap(5);

        spielInitialisieren();

        for(int i = 0; i < 4; i++) {
            for(int j = 0; j < 4; j++) {
                Button btn = new Button("?");
                btn.setPrefSize(80, 80);
                btn.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

                final int row = i;
                final int col = j;
                btn.setOnAction(e -> buttonGeklickt(row, col));

                buttons[i][j] = btn;
                grid.add(btn, j, i);
            }
        }

        Button resetBtn = new Button("Neues Spiel");
        resetBtn.setStyle("-fx-font-size: 14px;");
        resetBtn.setOnAction(e -> neuesSpiel());

        gameRoot.getChildren().addAll(scoreLabel, grid, resetBtn);
        return gameRoot;
    }

    private VBox createMusicPane() {
        VBox musicRoot = new VBox(10);
        musicRoot.setPadding(new Insets(10));
        musicRoot.setAlignment(Pos.CENTER);

        songListView = new ListView<>();
        songListView.setPrefHeight(300);
        songListView.setItems(playlist);

        currentSongLabel = new Label("Kein Song ausgewählt");
        currentSongLabel.setStyle("-fx-font-size: 14px;");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(380);

        volumeSlider = new Slider(0, 1, 0.5);
        volumeSlider.setPrefWidth(200);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (musicPlayer != null) {
                musicPlayer.setVolume(newVal.doubleValue());
            }
        });

        Button addButton = new Button("Musik hinzufügen");
        Button removeButton = new Button("Entfernen");
        Button playButton = new Button("▶");
        Button pauseButton = new Button("⏸");
        Button stopButton = new Button("⏹");
        Button prevButton = new Button("⏮");
        Button nextButton = new Button("⏭");
        Button shuffleButton = new Button("🔀");
        Button repeatButton = new Button("🔁");

        HBox controlButtons = new HBox(10);
        controlButtons.setAlignment(Pos.CENTER);
        controlButtons.getChildren().addAll(
                prevButton, playButton, pauseButton, stopButton, nextButton,
                shuffleButton, repeatButton
        );

        HBox managementButtons = new HBox(10);
        managementButtons.setAlignment(Pos.CENTER);
        managementButtons.getChildren().addAll(addButton, removeButton);

        HBox volumeControl = new HBox(10);
        volumeControl.setAlignment(Pos.CENTER);
        volumeControl.getChildren().addAll(
                new Label("🔈"), volumeSlider, new Label("🔊")
        );

        addButton.setOnAction(e -> addMusic());
        removeButton.setOnAction(e -> removeSelectedSong());
        playButton.setOnAction(e -> playSelectedSong());
        pauseButton.setOnAction(e -> pauseMusic());
        stopButton.setOnAction(e -> stopMusic());
        prevButton.setOnAction(e -> playPreviousSong());
        nextButton.setOnAction(e -> playNextSong());
        shuffleButton.setOnAction(e -> toggleShuffle());
        repeatButton.setOnAction(e -> toggleRepeat());

        musicRoot.getChildren().addAll(
                new Label("Playlist:"),
                songListView,
                managementButtons,
                currentSongLabel,
                progressBar,
                volumeControl,
                controlButtons
        );

        return musicRoot;
    }

    private void spielInitialisieren() {
        aufgedeckt = new boolean[4][4];
        paare = 0;
        versuche = 0;
        geklickteButtons.clear();

        List<String> temp = new ArrayList<>();
        for(String symbol : symbole) {
            temp.add(symbol);
            temp.add(symbol);
        }

        Collections.shuffle(temp);

        int index = 0;
        for(int i = 0; i < 4; i++) {
            for(int j = 0; j < 4; j++) {
                werte[i][j] = temp.get(index);
                index++;
            }
        }

        scoreUpdate();
    }

    private void buttonGeklickt(int row, int col) {
        if(aufgedeckt[row][col] || geklickteButtons.size() >= 2) {
            return;
        }

        buttons[row][col].setText(werte[row][col]);
        buttons[row][col].setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-background-color: lightblue;");
        geklickteButtons.add(buttons[row][col]);

        if(geklickteButtons.size() == 2) {
            versuche++;

            int[] pos1 = findeButtonPosition(geklickteButtons.get(0));
            int[] pos2 = findeButtonPosition(geklickteButtons.get(1));

            String wert1 = werte[pos1[0]][pos1[1]];
            String wert2 = werte[pos2[0]][pos2[1]];

            if(wert1.equals(wert2)) {
                aufgedeckt[pos1[0]][pos1[1]] = true;
                aufgedeckt[pos2[0]][pos2[1]] = true;
                paare++;

                geklickteButtons.get(0).setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-background-color: lightgreen;");
                geklickteButtons.get(1).setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-background-color: lightgreen;");

                geklickteButtons.clear();
                scoreUpdate();

                if(paare == 8) {
                    scoreLabel.setText("GEWONNEN! 🎉 Versuche: " + versuche);
                }
            } else {
                PauseTransition pause = new PauseTransition(Duration.seconds(pauseTime));
                pause.setOnFinished(e -> {
                    geklickteButtons.get(0).setText("?");
                    geklickteButtons.get(0).setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
                    geklickteButtons.get(1).setText("?");
                    geklickteButtons.get(1).setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
                    geklickteButtons.clear();
                });
                pause.play();
                scoreUpdate();
            }
        }
    }

    private int[] findeButtonPosition(Button btn) {
        for(int i = 0; i < 4; i++) {
            for(int j = 0; j < 4; j++) {
                if(buttons[i][j] == btn) {
                    return new int[]{i, j};
                }
            }
        }
        return new int[]{0, 0};
    }

    private void scoreUpdate() {
        scoreLabel.setText("Versuche: " + versuche + " | Paare gefunden: " + paare + "/8");
    }

    private void neuesSpiel() {
        for(int i = 0; i < 4; i++) {
            for(int j = 0; j < 4; j++) {
                buttons[i][j].setText("?");
                buttons[i][j].setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
            }
        }
        spielInitialisieren();
    }

    private void switchToGameView(String playerName, String difficulty) {
        currentPlayerName = playerName;
        currentDifficulty = difficulty;
        adjustDifficulty(difficulty);
        savePlayerData(playerName, difficulty);
        primaryStage.setScene(gameScene);
        neuesSpiel();
    }

    private void adjustDifficulty(String difficulty) {
        switch (difficulty) {
            case "Leicht" -> pauseTime = 2.0;
            case "Mittel" -> pauseTime = 1.0;
            case "Schwer" -> pauseTime = 0.5;
        }
    }

    private void savePlayerData(String playerName, String difficulty) {
        try (FileWriter writer = new FileWriter("player_data.txt", true)) {
            writer.write(playerName + "," + difficulty + "," + new Date() + "\n");
        } catch (IOException e) {
            showError("Speicherfehler", "Spielerdaten konnten nicht gespeichert werden");
        }
    }

    private void showHighscores() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Bestenliste");
        alert.setHeaderText("Top Spieler");
        alert.setContentText("Funktion wird noch implementiert!");
        alert.showAndWait();
    }

    private void showPauseMenu() {
        if (musicPlayer != null) {
            musicPlayer.pause();
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Pause");
        alert.setHeaderText("Spiel pausiert");
        alert.setContentText("Möchten Sie weiterspielen?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (musicPlayer != null) {
                musicPlayer.play();
            }
        } else {
            primaryStage.setScene(startScene);
            if (musicPlayer != null) {
                musicPlayer.stop();
            }
        }
    }

    private boolean showConfirmationDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    /**
     * Zeigt einen CAPTCHA-Dialog an und gibt true zurück, wenn der Benutzer das CAPTCHA korrekt eingibt.
     *
     * @return true, wenn CAPTCHA korrekt eingegeben wurde, sonst false.
     */
    private boolean showCaptchaDialog() {
        String captchaText = generateCaptcha();

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Sicherheitsprüfung");
        dialog.setHeaderText("Um das Spiel zu beenden, löse bitte das CAPTCHA.");
        dialog.setContentText("Gib den folgenden Text ein:  " + captchaText);

        Optional<String> result = dialog.showAndWait();

        // Überprüfen, ob der Benutzer einen Wert eingegeben und OK geklickt hat
        if (result.isPresent()) {
            String enteredText = result.get().trim();
            if (enteredText.equals(captchaText)) {
                return true; // CAPTCHA korrekt
            } else {
                showError("Fehler", "CAPTCHA falsch! Bitte versuche es erneut.");
                return false; // CAPTCHA falsch
            }
        }
        return false; // Benutzer hat abgebrochen oder Dialog geschlossen
    }

    /**
     * Generiert einen zufälligen alphanumerischen CAPTCHA-String.
     *
     * @return Der generierte CAPTCHA-String.
     */
    private String generateCaptcha() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder captcha = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) { // CAPTCHA-Länge von 6 Zeichen
            captcha.append(chars.charAt(random.nextInt(chars.length())));
        }
        return captcha.toString();
    }


    private void addMusic() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("WAV Dateien", "*.wav")
        );
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);

        if (selectedFiles != null) {
            for (File file : selectedFiles) {
                playlist.add(new Song(file.getName(), file.getPath()));
            }
            savePlaylist();
        }
    }

    private void removeSelectedSong() {
        Song selectedSong = songListView.getSelectionModel().getSelectedItem();
        if (selectedSong != null) {
            playlist.remove(selectedSong);
            savePlaylist();
        }
    }

    private void playSelectedSong() {
        Song selectedSong = songListView.getSelectionModel().getSelectedItem();
        if (selectedSong != null) {
            currentSongIndex = playlist.indexOf(selectedSong);
            playSong(selectedSong);
        }
    }

    private void playSong(Song song) {
        if (song == null) {
            showError("Fehler", "Kein Song ausgewählt");
            return;
        }
        try {
            File songFile = new File(song.getPath());
            if (!songFile.exists()) {
                showError("Dateifehler", "Die Audiodatei wurde nicht gefunden: ");
                playlist.remove(song);
                savePlaylist();
                return;
            }
            if (musicPlayer != null) {
                musicPlayer.dispose();
            }

            Media media = new Media(songFile.toURI().toString());
            musicPlayer = new MediaPlayer(media);
            musicPlayer.setVolume(volumeSlider.getValue());

            setupMediaPlayerListeners(song);
            musicPlayer.play();
        } catch (Exception e) {
            showError("Fehler beim Abspielen", e.getMessage());
        }
    }

    private void pauseMusic() {
        if (musicPlayer != null) {
            if (musicPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                musicPlayer.pause();
            } else {
                musicPlayer.play();
            }
        }
    }

    private void stopMusic() {
        if (musicPlayer != null) {
            musicPlayer.stop();
            currentSongLabel.setText("Gestoppt");
        }
    }

    private void playPreviousSong() {
        if (playlist.isEmpty()) return;

        if (isShuffleMode) {
            currentSongIndex = new Random().nextInt(playlist.size());
        } else {
            currentSongIndex = (currentSongIndex - 1 + playlist.size()) % playlist.size();
        }

        playSong(playlist.get(currentSongIndex));
    }

    private void playNextSong() {
        if (playlist.isEmpty()) return;

        if (isShuffleMode) {
            currentSongIndex = new Random().nextInt(playlist.size());
        } else {
            currentSongIndex = (currentSongIndex + 1) % playlist.size();
        }

        playSong(playlist.get(currentSongIndex));
    }

    private void setupMediaPlayerListeners(Song song) {
        musicPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (musicPlayer.getTotalDuration() != null) {
                double progress = newTime.toSeconds() / musicPlayer.getTotalDuration().toSeconds();
                progressBar.setProgress(progress);
            }
        });

        musicPlayer.setOnEndOfMedia(() -> {
            if (isRepeatMode) {
                musicPlayer.seek(Duration.ZERO);
                musicPlayer.play();
            } else {
                playNextSong();
            }
        });

        currentSongLabel.setText("Spielt: " + song.toString());
    }

    private void toggleShuffle() {
        isShuffleMode = !isShuffleMode;
    }

    private void toggleRepeat() {
        isRepeatMode = !isRepeatMode;
    }

    private void savePlaylist() {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream("playlist.dat"))) {
            oos.writeObject(new ArrayList<>(playlist));
        } catch (IOException e) {
            showError("Speicherfehler", "Playlist konnte nicht gespeichert werden");
        }
    }

    @SuppressWarnings("unchecked")
    private void loadPlaylist() {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream("playlist.dat"))) {
            ArrayList<Song> loadedList = (ArrayList<Song>) ois.readObject();
            playlist.setAll(loadedList);
        } catch (IOException | ClassNotFoundException e) {
            // Ignoriere Fehler beim ersten Start (Datei existiert noch nicht)
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static class Song implements Serializable {
        private String name;
        private String path;

        public Song(String name, String path) {
            this.name = name;
            this.path = path;
        }

        @Override
        public String toString() {
            return name;
        }

        public String getPath() {
            return path;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}