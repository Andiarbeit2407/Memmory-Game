package com.example.memmorygame;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MusicManager extends Application {

    private List<String> playlist = new ArrayList<>();
    private MediaPlayer mediaPlayer;
    private ListView<String> songListView;

    @Override
    public void start(Stage primaryStage) {
        try {
            primaryStage.setTitle("Musik Manager");

            VBox root = new VBox(10);
            root.setPadding(new Insets(10));

            // Steuerelemente erstellen
            Button addButton = new Button("Musik hinzufügen");
            Button playButton = new Button("Abspielen");
            Button stopButton = new Button("Stop");
            songListView = new ListView<>();

            // Musik hinzufügen
            addButton.setOnAction(e -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("MP3 Dateien", "*.mp3")
                );
                File selectedFile = fileChooser.showOpenDialog(primaryStage);

                if (selectedFile != null) {
                    playlist.add(selectedFile.getPath());
                    songListView.getItems().add(selectedFile.getName());
                }
            });

            // Musik abspielen
            playButton.setOnAction(e -> {
                String selectedSong = songListView.getSelectionModel().getSelectedItem();
                if (selectedSong != null) {
                    int index = songListView.getSelectionModel().getSelectedIndex();
                    playMusic(playlist.get(index)); // Fehlerbehandelte Methode
                } else {
                    showErrorDialog("Keine Auswahl", "Bitte wählen Sie einen Song aus der Liste aus.");
                }
            });

            // Musik stoppen
            stopButton.setOnAction(e -> {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                }
            });

            root.getChildren().addAll(addButton, songListView, playButton, stopButton);

            Scene scene = new Scene(root, 400, 500);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            showErrorDialog("Fehler", "Ein kritischer Fehler ist aufgetreten: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void playMusic(String path) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
            }

            Media media = new Media(new File(path).toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.play();
        } catch (Exception e) {
            showErrorDialog("Abspielfehler", "Der ausgewählte Titel konnte nicht abgespielt werden. Bitte überprüfen Sie die Datei: " + path);
            e.printStackTrace();
        }
    }

    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}