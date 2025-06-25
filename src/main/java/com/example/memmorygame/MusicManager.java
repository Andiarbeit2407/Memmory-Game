package com.example.memmorygame;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class MusicManager {

    private MemoryGame mainApp;
    private MediaPlayer currentPlayer;
    private List<String> playlist;
    private int currentSongIndex;
    private boolean isPlaying;

    // UI components
    private Label currentSongLabel;
    private Button playPauseButton;
    private Slider volumeSlider;

    public MusicManager() {
        this.playlist = new ArrayList<>();
        this.currentSongIndex = 0;
        this.isPlaying = false;
        ensureMusicDirectoryExists();
        loadMusicFromDirectory();
    }

    private void ensureMusicDirectoryExists() {
        File musicDir = new File("music");
        if (!musicDir.exists()) {
            musicDir.mkdirs();
        }
    }

    private void loadMusicFromDirectory() {
        playlist.clear();
        File musicDir = new File("music");
        File[] files = musicDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".mp3") || lower.endsWith(".wav");
        });
        if (files != null && files.length > 0) {
            for (File file : files) {
                playlist.add(file.getPath());
            }
        }
        if (playlist.isEmpty()) {
            playlist.add("No music files found");
        }
        currentSongIndex = 0;
        updateCurrentSongLabel();
    }

    public void setMainApp(MemoryGame mainApp) {
        this.mainApp = mainApp;
    }

    public VBox createMusicPane() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(50));
        root.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Music Manager");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        currentSongLabel = new Label(getCurrentSongName());
        currentSongLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #ffffff;");

        // Control buttons
        Button previousButton = new Button("Previous");
        previousButton.setOnAction(e -> playPrevious());

        playPauseButton = new Button(isPlaying ? "Pause" : "Play");
        playPauseButton.setOnAction(e -> togglePlayPause());

        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction(e -> pause());

        Button nextButton = new Button("Next");
        nextButton.setOnAction(e -> playNext());

        HBox controlsBox = new HBox(10, previousButton, playPauseButton, pauseButton, nextButton);
        controlsBox.setAlignment(Pos.CENTER);

        // Volume control
        Label volumeLabel = new Label("Volume:");
        volumeLabel.setStyle("-fx-text-fill: #ffffff;");

        volumeSlider = new Slider(0, 100, 50);
        volumeSlider.setShowTickMarks(true);
        volumeSlider.setShowTickLabels(true);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> setVolume(newVal.doubleValue()));

        VBox volumeBox = new VBox(5, volumeLabel, volumeSlider);
        volumeBox.setAlignment(Pos.CENTER);

        // Musik hinzufügen Button
        Button addMusicButton = new Button("Musik hinzufügen");
        addMusicButton.setStyle("-fx-font-size: 16px; -fx-min-width: 150px;");
        addMusicButton.setOnAction(e -> handleAddMusic());

        // Back button
        Button backButton = new Button("Back to Main Menu");
        backButton.setStyle("-fx-font-size: 16px; -fx-min-width: 150px;");
        backButton.setOnAction(e -> mainApp.switchToStartView());

        root.getChildren().addAll(titleLabel, currentSongLabel, controlsBox, volumeBox, addMusicButton, backButton);

        return root;
    }

    private void handleAddMusic() {
        Window window = mainApp.getPrimaryStage();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Audio-Dateien auswählen");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Audio Dateien (*.mp3, *.wav)", "*.mp3", "*.wav"),
            new FileChooser.ExtensionFilter("MP3 Dateien", "*.mp3"),
            new FileChooser.ExtensionFilter("WAV Dateien", "*.wav")
        );
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(window);

        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            File musicDir = new File("music");
            int added = 0;
            for (File src : selectedFiles) {
                File dest = new File(musicDir, src.getName());
                try {
                    if (!dest.exists()) {
                        Files.copy(src.toPath(), dest.toPath());
                        added++;
                    }
                } catch (IOException ex) {
                    DialogUtils.showError("Fehler beim Hinzufügen", "Datei konnte nicht kopiert werden: " + src.getName());
                }
            }
            if (added > 0) {
                DialogUtils.showInformation("Musik hinzugefügt", added + " Datei(en) wurden hinzugefügt.");
                loadMusicFromDirectory();
            } else {
                DialogUtils.showInformation("Keine neuen Dateien", "Es wurden keine neuen Musikdateien hinzugefügt.");
            }
        }
    }

    public void togglePlayPause() {
        if (playlist.get(0).equals("No music files found")) {
            DialogUtils.showInformation("No Music", "No music files found in the music directory.");
            return;
        }

        if (isPlaying) {
            pause();
        } else {
            play();
        }
    }

    private void play() {
        try {
            if (currentPlayer != null) {
                currentPlayer.stop();
            }

            String currentSong = playlist.get(currentSongIndex);
            File file = new File(currentSong);
            Media media = new Media(file.toURI().toString());
            currentPlayer = new MediaPlayer(media);

            currentPlayer.setVolume(volumeSlider.getValue() / 100.0);
            currentPlayer.setOnEndOfMedia(this::playNext);
            currentPlayer.play();

            isPlaying = true;
            playPauseButton.setText("Pause");
        } catch (Exception e) {
            DialogUtils.showError("Music Error", "Could not play music file: " + e.getMessage());
        }
    }

    private void pause() {
        if (currentPlayer != null) {
            currentPlayer.pause();
        }
        isPlaying = false;
        playPauseButton.setText("Play");
    }

    public void playNext() {
        if (playlist.size() > 1) {
            currentSongIndex = (currentSongIndex + 1) % playlist.size();
            updateCurrentSongLabel();
            if (isPlaying) {
                play();
            }
        }
    }

    public void playPrevious() {
        if (playlist.size() > 1) {
            currentSongIndex = (currentSongIndex - 1 + playlist.size()) % playlist.size();
            updateCurrentSongLabel();
            if (isPlaying) {
                play();
            }
        }
    }

    private void setVolume(double volume) {
        if (currentPlayer != null) {
            currentPlayer.setVolume(volume / 100.0);
        }
    }

    private String getCurrentSongName() {
        if (playlist.isEmpty()) {
            return "No songs available";
        }

        String fullPath = playlist.get(currentSongIndex);
        if (fullPath.equals("No music files found")) {
            return fullPath;
        }

        // Extract filename from path
        return new File(fullPath).getName();
    }

    private void updateCurrentSongLabel() {
        if (currentSongLabel != null) {
            currentSongLabel.setText(getCurrentSongName());
        }
    }

    public void cleanup() {
        if (currentPlayer != null) {
            currentPlayer.stop();
            currentPlayer.dispose();
        }
    }
}