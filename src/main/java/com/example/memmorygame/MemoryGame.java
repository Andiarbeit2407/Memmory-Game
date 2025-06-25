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
import javafx.beans.value.ChangeListener; // Diese Importe sind möglicherweise nicht mehr direkt im Code verwendet, aber schaden nicht
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.*;
import java.util.*;
import javafx.scene.input.KeyCode; // Für KeyCode (Shortcuts)

public class MemoryGame extends Application {

    private Stage primaryStage;
    private Scene gameScene;
    private Scene startScene;
    private Scene musicScene; // Hinzugefügt, um die Musik-Szene als Instanzvariable zu halten

    // Memory-Spiel Attribute
    private Button[][] buttons; // Größe wird dynamisch festgelegt
    private String[][] werte;   // Größe wird dynamisch festgelegt
    private boolean[][] aufgedeckt; // Größe wird dynamisch festgelegt
    private List<Button> geklickteButtons = new ArrayList<>();
    private int paare = 0;
    private Label scoreLabel;
    private int versuche = 0;
    private int gridSize = 4; // Standardmäßig 4x4
    private double calculatedButtonSize; // Neue Instanzvariable für die Button-Größe
    private String currentPlayerName = "";
    private String currentDifficulty = "Mittel";
    private double pauseTime = 1.0;

    // Layout-Elemente für das Spielpaneel, damit sie dynamisch aktualisiert werden können
    private BorderPane gameLayout;
    private GridPane gameGrid;
    private HBox controls; // Hier als Instanzvariable deklariert

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

    // Referenz auf den Musik-Root, um ToggleButtons zugänglich zu machen, ohne die Logik zu ändern
    private VBox musicRoot;

    // Konstante für den Playlist-Dateinamen
    private static final String PLAYLIST_FILE_NAME = "playlist.ser";


    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Startseite erstellen
        VBox startPageRoot = createStartPage();
        startScene = new Scene(startPageRoot, 800, 600); // Standardgröße für Startseite

        // Spiel-Panel erstellen (initialisiert nur das Layout, nicht die Buttons)
        VBox gamePaneRoot = createGamePane();
        gameScene = new Scene(gamePaneRoot); // Keine feste Größe mehr hier, wird dynamisch angepasst

        // Wiedergabeliste laden (automatisch beim Start)
        loadPlaylist();

        // --- Dark Mode CSS anwenden ---
        startScene.getStylesheets().add(getClass().getResource("/dark-mode.css").toExternalForm());
        // gameScene und musicScene Stylesheets werden gesetzt, wenn die Szenen aktiviert werden
        // da sie dynamisch erstellt oder neu zugewiesen werden können.

        primaryStage.setScene(startScene);
        primaryStage.setTitle("Memory Game");

        // NEUE FUNKTIONALITÄT: Event-Handler für den Schließen-Knopf des Fensters (X-Button)
        primaryStage.setOnCloseRequest(event -> {
            // Verhindert das sofortige Schließen des Fensters
            event.consume();

            if (showConfirmationDialog("Anwendung beenden", "Möchten Sie die Anwendung wirklich beenden? Sie müssen ein CAPTCHA lösen.")) {
                if (showCaptchaDialog()) {
                    // CAPTCHA korrekt gelöst
                    // Musik stoppen und Ressourcen freigeben
                    if (musicPlayer != null) {
                        musicPlayer.stop();
                        musicPlayer.dispose();
                    }
                    savePlaylist(); // Playlist speichern beim Beenden
                    primaryStage.close(); // Fenster schließen
                } else {
                    // CAPTCHA falsch
                    showError("CAPTCHA Fehler", "Das CAPTCHA war falsch. Die Anwendung bleibt geöffnet.");
                }
            } else {
                // Benutzer hat "Abbrechen" im Bestätigungsdialog gewählt
                System.out.println("Schließen abgebrochen.");
            }
        });

        // NEU: Shortcuts für die Startseite (startScene)
        startScene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                // Auslösen der Start-Button-Aktion
                // Direkter Zugriff auf die Komponenten der Startseite
                TextField nameInput = (TextField) ((VBox) startScene.getRoot()).getChildren().get(1);
                ComboBox<String> difficultyComboBox = (ComboBox<String>) ((VBox) startScene.getRoot()).getChildren().get(2);

                currentPlayerName = nameInput.getText().trim();
                if (currentPlayerName.isEmpty()) {
                    showError("Fehler", "Bitte gib einen Spielernamen ein.");
                } else {
                    currentDifficulty = difficultyComboBox.getValue();
                    switchToGameView(currentPlayerName, currentDifficulty);
                }
                event.consume(); // Event konsumieren
            } else if (event.getCode() == KeyCode.ESCAPE) {
                // Auslösen der Exit-Button-Aktion (inkl. CAPTCHA)
                // Die Logik ist bereits im exitButton.setOnAction hinterlegt und wird hier direkt dupliziert
                // um Konsistenz zu gewährleisten, wenn der User ESC drückt.
                if (showConfirmationDialog("Anwendung beenden", "Möchten Sie die Anwendung wirklich beenden? Sie müssen ein CAPTCHA lösen.")) {
                    if (showCaptchaDialog()) {
                        if (musicPlayer != null) {
                            musicPlayer.stop();
                            musicPlayer.dispose();
                        }
                        savePlaylist(); // Playlist speichern beim Beenden
                        primaryStage.close();
                    } else {
                        showError("CAPTCHA Fehler", "Das CAPTCHA war falsch. Die Anwendung bleibt geöffnet.");
                    }
                }
                event.consume();
            }
        });

        primaryStage.show();
    }

    // Die stop() Methode wird automatisch von JavaFX aufgerufen, wenn die Anwendung beendet wird (auch nach primaryStage.close()).
    // Hier können wir nochmals eine Sicherung für das Speichern der Playlist einbauen, falls der Benutzer den Dialog ablehnt oder der Exit nicht über die UI erfolgt.
    @Override
    public void stop() throws Exception {
        super.stop();
        // Hier sollte die Playlist NUR gespeichert werden, wenn nicht bereits durch den CloseRequest-Handler geschehen.
        // Die aktuelle Logik im CloseRequest-Handler ist ausreichend.
        // Diese stop-Methode dient mehr als letzter Aufräumschritt.
        if (musicPlayer != null) {
            musicPlayer.dispose(); // Sicherstellen, dass der Player disposed wird
        }
        System.out.println("Anwendung wird beendet. Ressourcen freigegeben.");
    }


    private VBox createStartPage() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(50));
        root.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Memory Spiel");
        titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold;");

        TextField nameInput = new TextField();
        nameInput.setPromptText("Gib deinen Spielernamen ein");
        nameInput.setMaxWidth(300);

        ComboBox<String> difficultyComboBox = new ComboBox<>();
        difficultyComboBox.getItems().addAll("Einfach", "Mittel", "Schwer");
        difficultyComboBox.setValue("Mittel"); // Standardwert

        Button startButton = new Button("Spiel starten");
        startButton.setOnAction(e -> {
            currentPlayerName = nameInput.getText().trim();
            if (currentPlayerName.isEmpty()) {
                showError("Fehler", "Bitte gib einen Spielernamen ein.");
            } else {
                currentDifficulty = difficultyComboBox.getValue();
                switchToGameView(currentPlayerName, currentDifficulty);
            }
        });

        Button highscoreButton = new Button("Highscores anzeigen");
        highscoreButton.setOnAction(e -> showHighscores());

        Button exitButton = new Button("Beenden");
        exitButton.setOnAction(e -> {
            // ANGEPASSTE LOGIK FÜR DEN BEENDEN-BUTTON (mit CAPTCHA)
            if (showConfirmationDialog("Anwendung beenden", "Möchtest du die Anwendung wirklich beenden? Du musst ein CAPTCHA lösen.")) {
                if (showCaptchaDialog()) {
                    // CAPTCHA korrekt gelöst
                    if (musicPlayer != null) {
                        musicPlayer.stop();
                        musicPlayer.dispose();
                    }
                    savePlaylist(); // Playlist speichern beim Beenden
                    primaryStage.close(); // Anwendung schließen
                } else {
                    // CAPTCHA falsch
                    showError("CAPTCHA Fehler", "Das CAPTCHA war falsch. Die Anwendung bleibt geöffnet.");
                }
            }
        });

        // Musik Manager Button
        Button musicManagerButton = new Button("Musik Manager");
        musicManagerButton.setOnAction(e -> showMusicManager());

        root.getChildren().addAll(titleLabel, nameInput, difficultyComboBox, startButton, highscoreButton, musicManagerButton, exitButton);
        return root;
    }

    private VBox createGamePane() {
        gameLayout = new BorderPane();
        gameLayout.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE); // BorderPane den gesamten Platz einnehmen lassen

        // Initialisiere ein leeres Grid. Es wird später durch buildGameGrid() ersetzt.
        gameGrid = new GridPane();
        gameGrid.setAlignment(Pos.CENTER);
        gameLayout.setCenter(gameGrid);

        scoreLabel = new Label("Versuche: 0");
        scoreLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button neuesSpielButton = new Button("Neues Spiel");
        neuesSpielButton.setOnAction(e -> neuesSpiel());

        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction(e -> showPauseMenu());

        Button backToStartButton = new Button("Zurück zur Startseite");
        backToStartButton.setOnAction(e -> {
            if (showConfirmationDialog("Zurück zur Startseite", "Möchtest du das aktuelle Spiel beenden und zur Startseite zurückkehren?")) {
                primaryStage.setScene(startScene);
                stopMusic(); // Musik stoppen, wenn man zur Startseite zurückkehrt
            }
        });

        controls = new HBox(20, scoreLabel, neuesSpielButton, pauseButton, backToStartButton); // Jetzt Zuweisung zur Instanzvariable
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(20));
        gameLayout.setBottom(controls);

        VBox gameRootVBox = new VBox();
        gameRootVBox.getChildren().add(gameLayout);
        VBox.setVgrow(gameLayout, Priority.ALWAYS); // Erlaubt dem BorderPane, vertikal zu wachsen

        return gameRootVBox;
    }

    /**
     * Erstellt das Spielgitter und die Buttons basierend auf der aktuellen gridSize und calculatedButtonSize.
     */
    private void buildGameGrid() {
        gameGrid = new GridPane();
        gameGrid.setAlignment(Pos.CENTER);
        gameGrid.setHgap(10); // Horizontaler Abstand
        gameGrid.setVgap(10); // Vertikaler Abstand

        buttons = new Button[gridSize][gridSize]; // Initialisiere das buttons-Array mit der korrekten Größe

        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                Button btn = new Button();
                // Button-Größe basierend auf calculatedButtonSize setzen
                btn.setMinSize(calculatedButtonSize, calculatedButtonSize);
                btn.setMaxSize(calculatedButtonSize, calculatedButtonSize); // Fixe Größe

                // Schriftgröße anpassen: größere Gitter -> kleinere Schrift
                int fontSize;
                if (gridSize == 4) fontSize = 24;
                else if (gridSize == 6) fontSize = 18;
                else fontSize = 14; // for gridSize == 8
                btn.setStyle("-fx-font-size: " + fontSize + "px;");

                final int row = i;
                final int col = j;
                btn.setOnAction(e -> buttonGeklickt(row, col));
                buttons[i][j] = btn;
                gameGrid.add(btn, j, i);
            }
        }
        gameLayout.setCenter(gameGrid); // Ersetze das alte Gitter durch das neu erstellte
    }

    private VBox createMusicPane() {
        musicRoot = new VBox(10);
        musicRoot.setPadding(new Insets(20));
        musicRoot.setAlignment(Pos.TOP_CENTER);

        Label musicTitle = new Label("Musik Manager");
        musicTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        songListView = new ListView<>(playlist);
        songListView.setPrefHeight(200);

        currentSongLabel = new Label("Aktueller Song: Keiner");
        currentSongLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);

        volumeSlider = new Slider(0, 1, 0.5); // Min, Max, Initial
        volumeSlider.setBlockIncrement(0.1);
        volumeSlider.setPrefWidth(200);
        volumeSlider.setShowTickLabels(true);
        volumeSlider.setShowTickMarks(true);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (musicPlayer != null) {
                musicPlayer.setVolume(newVal.doubleValue());
            }
        });

        Button addMusicButton = new Button("Musik hinzufügen");
        addMusicButton.setOnAction(e -> addMusic());

        Button removeSongButton = new Button("Ausgewählten Song entfernen");
        removeSongButton.setOnAction(e -> removeSelectedSong());

        Button playButton = new Button("Play");
        playButton.setOnAction(e -> playSelectedSong());

        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction(e -> pauseMusic());

        Button stopButton = new Button("Stop");
        stopButton.setOnAction(e -> stopMusic());

        Button previousButton = new Button("Vorheriger");
        previousButton.setOnAction(e -> playPreviousSong());

        Button nextButton = new Button("Nächster");
        nextButton.setOnAction(e -> playNextSong());

        ToggleButton shuffleButton = new ToggleButton("Zufall (Aus)");
        shuffleButton.setOnAction(e -> toggleShuffle());

        ToggleButton repeatButton = new ToggleButton("Wiederholen (Aus)");
        repeatButton.setOnAction(e -> toggleRepeat());


        Button backButton = new Button("Zurück");
        backButton.setOnAction(e -> primaryStage.setScene(startScene));

        HBox topControls = new HBox(10, addMusicButton, removeSongButton);
        topControls.setAlignment(Pos.CENTER);

        HBox playbackControls = new HBox(10, previousButton, playButton, pauseButton, stopButton, nextButton);
        playbackControls.setAlignment(Pos.CENTER);

        HBox modeControls = new HBox(10, shuffleButton, repeatButton);
        modeControls.setAlignment(Pos.CENTER);

        VBox volumeControl = new VBox(5, new Label("Lautstärke:"), volumeSlider);
        volumeControl.setAlignment(Pos.CENTER);

        musicRoot.getChildren().addAll(musicTitle, currentSongLabel, progressBar, songListView, volumeControl, topControls, playbackControls, modeControls, backButton);
        return musicRoot;
    }

    private void showMusicManager() {
        musicScene = new Scene(createMusicPane(), 800, 600); // Zuweisung zur Instanzvariable
        musicScene.getStylesheets().add(getClass().getResource("/dark-mode.css").toExternalForm()); // Dark Mode auch für den Music Manager

        // NEU: Shortcuts für die Musik-Manager-Szene (musicScene)
        musicScene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SPACE) { // Leertaste für Play/Pause
                if (musicPlayer != null && musicPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    pauseMusic();
                } else {
                    playSelectedSong(); // Versucht, den aktuell ausgewählten/nächsten Song zu spielen
                }
                event.consume();
            } else if (event.getCode() == KeyCode.RIGHT) { // Pfeil Rechts für Nächster Song
                playNextSong();
                event.consume();
            } else if (event.getCode() == KeyCode.LEFT) { // Pfeil Links für Vorheriger Song
                playPreviousSong();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) { // Escape für Zurück
                primaryStage.setScene(startScene);
                event.consume();
            }
        });

        primaryStage.setScene(musicScene);
    }

    /**
     * Generiert eine Liste von Symbolen, die für das Spielgitter verwendet werden können.
     * @param count Die Anzahl der benötigten einzigartigen Symbole.
     * @return Ein Array von Strings mit den generierten Symbolen.
     */
    private String[] generateGameSymbols(int count) {
        List<String> pool = new ArrayList<>();
        // Buchstaben A-Z
        for (char c = 'A'; c <= 'Z'; c++) {
            pool.add(String.valueOf(c));
        }
        // Buchstaben a-z
        for (char c = 'a'; c <= 'z'; c++) {
            pool.add(String.valueOf(c));
        }
        // Zahlen 0-99 (als Strings)
        for (int i = 0; i < 100; i++) {
            pool.add(String.valueOf(i));
        }

        if (pool.size() < count) {
            throw new IllegalStateException("Nicht genügend einzigartige Symbole für die Gittergröße verfügbar.");
        }

        Collections.shuffle(pool); // Symbole mischen
        String[] selectedSymbols = new String[count];
        for (int i = 0; i < count; i++) {
            selectedSymbols[i] = pool.get(i);
        }
        return selectedSymbols;
    }

    private void spielInitialisieren() {
        // Arrays basierend auf der aktuellen gridSize initialisieren
        werte = new String[gridSize][gridSize];
        aufgedeckt = new boolean[gridSize][gridSize];

        // Symbole für die aktuelle Gittergröße generieren
        String[] currentSymbols = generateGameSymbols((gridSize * gridSize) / 2);

        // Symbole mischen
        List<String> tempSymbole = new ArrayList<>();
        for (String s : currentSymbols) {
            tempSymbole.add(s);
            tempSymbole.add(s); // Jedes Symbol zweimal hinzufügen
        }
        Collections.shuffle(tempSymbole);

        // Werte den Buttons zuweisen und Zustand zurücksetzen
        int k = 0;
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                buttons[i][j].setText("");
                buttons[i][j].setDisable(false);
                werte[i][j] = tempSymbole.get(k++);
                aufgedeckt[i][j] = false;
            }
        }
        paare = 0;
        versuche = 0;
        scoreLabel.setText("Versuche: 0");
        geklickteButtons.clear();
    }

    private void buttonGeklickt(int row, int col) {
        Button geklickterButton = buttons[row][col];

        // Nur unaufgedeckte und nicht bereits geklickte Buttons bearbeiten
        if (aufgedeckt[row][col] || geklickterButton.getText().length() > 0) {
            return;
        }

        geklickterButton.setText(werte[row][col]);
        geklickteButtons.add(geklickterButton);

        if (geklickteButtons.size() == 2) {
            // Alle Buttons während des Vergleichs deaktivieren
            for (int i = 0; i < gridSize; i++) {
                for (int j = 0; j < gridSize; j++) {
                    buttons[i][j].setDisable(true);
                }
            }

            versuche++;
            scoreUpdate();

            PauseTransition pause = new PauseTransition(Duration.seconds(pauseTime));
            pause.setOnFinished(event -> {
                Button ersterButton = geklickteButtons.get(0);
                Button zweiterButton = geklickteButtons.get(1);

                int[] pos1 = findeButtonPosition(ersterButton);
                int[] pos2 = findeButtonPosition(zweiterButton);

                if (werte[pos1[0]][pos1[1]].equals(werte[pos2[0]][pos2[1]])) {
                    // Paare gefunden
                    ersterButton.setDisable(true);
                    zweiterButton.setDisable(true);
                    aufgedeckt[pos1[0]][pos1[1]] = true;
                    aufgedeckt[pos2[0]][pos2[1]] = true;
                    paare++;
                } else {
                    // Keine Paare
                    ersterButton.setText("");
                    zweiterButton.setText("");
                }

                geklickteButtons.clear();

                // Buttons wieder aktivieren, die noch nicht aufgedeckt sind
                for (int i = 0; i < gridSize; i++) {
                    for (int j = 0; j < gridSize; j++) {
                        if (!aufgedeckt[i][j]) {
                            buttons[i][j].setDisable(false);
                        }
                    }
                }

                // Gewinnbedingung an die Gittergröße anpassen
                if (paare == (gridSize * gridSize) / 2) {
                    if (showCaptchaDialog()) { // CAPTCHA vor dem Speichern der Highscores
                        showHighscores();
                        if (showConfirmationDialog("Spiel beendet!", "Herzlichen Glückwunsch! Du hast alle Paare gefunden!\n" +
                                "Möchtest du ein neues Spiel starten?")) {
                            neuesSpiel();
                        } else {
                            primaryStage.setScene(startScene);
                        }
                        savePlayerData(currentPlayerName, currentDifficulty); // Highscore speichern
                    } else {
                        showError("CAPTCHA Fehler", "Das CAPTCHA war falsch. Highscore wird nicht gespeichert.");
                        neuesSpiel();
                    }
                }
            });
            pause.play();
        }
    }

    private int[] findeButtonPosition(Button btn) {
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                if (buttons[i][j] == btn) {
                    return new int[]{i, j};
                }
            }
        }
        return null;
    }

    private void scoreUpdate() {
        scoreLabel.setText("Versuche: " + versuche);
    }

    private void neuesSpiel() {
        spielInitialisieren();
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                buttons[i][j].setDisable(false);
            }
        }
    }

    private void switchToGameView(String playerName, String difficulty) {
        currentPlayerName = playerName;
        currentDifficulty = difficulty;

        adjustDifficulty(currentDifficulty); // Setzt gridSize und calculatedButtonSize

        // Konstanten für Abstände/Padding
        final double H_GAP = 10;
        final double V_GAP = 10;
        final double CONTROLS_HEIGHT_ESTIMATE = 80.0;
        final double SCENE_PADDING_VERTICAL = 40.0;
        final double SCENE_PADDING_HORIZONTAL = 40.0;

        // Berechnung der benötigten Szenengröße
        double requiredGridWidth = (gridSize * calculatedButtonSize) + ((gridSize - 1) * H_GAP);
        double requiredGridHeight = (gridSize * calculatedButtonSize) + ((gridSize - 1) * V_GAP);

        double newWidth = requiredGridWidth + SCENE_PADDING_HORIZONTAL;
        double newHeight = requiredGridHeight + CONTROLS_HEIGHT_ESTIMATE + SCENE_PADDING_VERTICAL;

        if (newWidth < 500) newWidth = 500;
        if (newHeight < 400) newHeight = 400;
        if (newWidth > 1000) newWidth = 1000;
        if (newHeight > 900) newHeight = 900;


        primaryStage.setWidth(newWidth);
        primaryStage.setHeight(newHeight);
        primaryStage.centerOnScreen();

        buildGameGrid(); // Erstellt das Gitter und die Buttons mit der neuen gridSize und calculatedButtonSize
        spielInitialisieren(); // Initialisiert das Spiel mit den neuen Einstellungen
        primaryStage.setScene(gameScene); // Setzt die Szene, die sich nun an die Fenstergröße anpassen wird

        // NEU: Shortcuts für die Spielszene (gameScene)
        gameScene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                showPauseMenu();
                event.consume();
            } else if (event.getCode() == KeyCode.N) {
                neuesSpiel();
                event.consume();
            }
        });
        gameScene.getStylesheets().add(getClass().getResource("/dark-mode.css").toExternalForm());
    }

    private void adjustDifficulty(String difficulty) {
        switch (difficulty) {
            case "Einfach":
                pauseTime = 2.0;
                gridSize = 4; // 4x4 Gitter
                calculatedButtonSize = 120.0; // Große Buttons
                break;
            case "Mittel":
                pauseTime = 1.0;
                gridSize = 6; // 6x6 Gitter
                calculatedButtonSize = 80.0; // Mittelgroße Buttons
                break;
            case "Schwer":
                pauseTime = 0.5;
                gridSize = 8; // 8x8 Gitter
                calculatedButtonSize = 60.0; // Kleine Buttons
                break;
            default:
                pauseTime = 1.0;
                gridSize = 4; // Standardwert
                calculatedButtonSize = 120.0;
        }
    }

    private void savePlayerData(String playerName, String difficulty) {
        String filename = "highscores.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true))) { // true für Append-Modus
            writer.write(playerName + ";" + difficulty + ";" + versuche + "\n");
        } catch (IOException e) {
            showError("Fehler beim Speichern", "Highscore konnte nicht gespeichert werden: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showHighscores() {
        String filename = "highscores.txt";
        List<String> highscores = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                highscores.add(line);
            }
        } catch (IOException e) {
            showError("Fehler beim Laden", "Highscores konnten nicht geladen werden: " + e.getMessage());
            highscores.add("Noch keine Highscores vorhanden.");
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Highscores");
        alert.setHeaderText("Beste Spielergebnisse:");

        StringBuilder sb = new StringBuilder();
        highscores.stream()
                .map(line -> {
                    String[] parts = line.split(";");
                    if (parts.length == 3) {
                        try {
                            return new AbstractMap.SimpleEntry<>(Integer.parseInt(parts[2]), parts[0] + " (" + parts[1] + "): " + parts[2] + " Versuche");
                        } catch (NumberFormatException e) {
                            return new AbstractMap.SimpleEntry<>(Integer.MAX_VALUE, line);
                        }
                    }
                    return new AbstractMap.SimpleEntry<>(Integer.MAX_VALUE, line);
                })
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .forEach(entry -> sb.append(entry.getValue()).append("\n"));


        TextArea textArea = new TextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(textArea, 0, 0);

        alert.getDialogPane().setExpandableContent(expContent);
        alert.getDialogPane().setExpanded(true);

        alert.showAndWait();
    }

    private void showPauseMenu() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Spiel pausiert");
        alert.setHeaderText("Das Spiel ist pausiert.");
        alert.setContentText("Möchtest du das Spiel fortsetzen oder beenden?");

        ButtonType resumeButton = new ButtonType("Fortsetzen");
        ButtonType exitButton = new ButtonType("Beenden", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(resumeButton, exitButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == exitButton) {
            if (showConfirmationDialog("Beenden", "Möchtest du das Spiel wirklich beenden?")) {
                primaryStage.setScene(startScene); // Zurück zur Startseite
                stopMusic(); // Musik stoppen, wenn man zur Startseite zurückkehrt
            }
        }
    }

    private boolean showConfirmationDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Zeigt einen CAPTCHA-Dialog an und gibt true zurück, wenn der Benutzer das CAPTCHA korrekt eingibt.
     *
     * @return true, wenn CAPTCHA korrekt eingegeben wurde, sonst false.
     */
    private boolean showCaptchaDialog() {
        String captchaText = generateCaptcha();
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("CAPTCHA-Verifizierung");
        dialog.setHeaderText("Bitte geben Sie den folgenden Text ein, um fortzufahren:");
        dialog.setContentText("CAPTCHA: " + captchaText);

        Optional<String> result = dialog.showAndWait();
        return result.isPresent() && result.get().equalsIgnoreCase(captchaText);
    }

    /**
     * Generiert einen zufälligen alphanumerischen CAPTCHA-String.
     *
     * @return Der generierte CAPTCHA-String.
     */
    private String generateCaptcha() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder captcha = new StringBuilder();
        Random rnd = new Random();
        for (int i = 0; i < 6; i++) {
            captcha.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return captcha.toString();
    }

    private void addMusic() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Audio-Dateien", "*.mp3", "*.wav", "*.aac")
        );
        File selectedFile = fileChooser.showOpenDialog(primaryStage);

        if (selectedFile != null) {
            Song newSong = new Song(selectedFile.getName(), selectedFile.toURI().toString());
            playlist.add(newSong);
            savePlaylist(); // Playlist speichern, nachdem ein Song hinzugefügt wurde
        }
    }

    private void removeSelectedSong() {
        Song selectedSong = songListView.getSelectionModel().getSelectedItem();
        if (selectedSong != null) {
            if (showConfirmationDialog("Song entfernen", "Möchten Sie '" + selectedSong.getName() + "' wirklich aus der Wiedergabeliste entfernen?")) {
                // Prüfen, ob der zu entfernende Song der aktuell spielende ist
                if (musicPlayer != null && currentSongIndex < playlist.size() && selectedSong.getPath().equals(playlist.get(currentSongIndex).getPath())) {
                    stopMusic(); // Aktuell spielenden Song stoppen, wenn er entfernt wird
                }
                playlist.remove(selectedSong);
                savePlaylist(); // Playlist speichern, nachdem ein Song entfernt wurde
                // Index anpassen, falls der entfernte Song vor dem aktuellen Song war
                if (currentSongIndex >= playlist.size() && !playlist.isEmpty()) {
                    currentSongIndex = playlist.size() - 1;
                } else if (playlist.isEmpty()) {
                    currentSongIndex = 0;
                    currentSongLabel.setText("Aktueller Song: Keiner");
                    progressBar.setProgress(0);
                } else if (currentSongIndex > songListView.getSelectionModel().getSelectedIndex()) {
                    currentSongIndex--; // Wenn ein Song vor dem aktuellen Index entfernt wird
                }
            }
        } else {
            showError("Keine Auswahl", "Bitte wählen Sie einen Song zum Entfernen aus.");
        }
    }

    private void playSelectedSong() {
        Song selectedSong = songListView.getSelectionModel().getSelectedItem();
        if (selectedSong != null) {
            playSong(selectedSong);
            currentSongIndex = playlist.indexOf(selectedSong); // Index des ausgewählten Songs setzen
        } else if (!playlist.isEmpty()) {
            playSong(playlist.get(currentSongIndex)); // Falls nichts ausgewählt ist, aktuellen Song spielen
        } else {
            showError("Wiedergabefehler", "Kein Song zum Abspielen ausgewählt oder in der Wiedergabeliste.");
        }
    }

    private void playSong(Song song) {
        if (musicPlayer != null) {
            musicPlayer.stop();
            musicPlayer.dispose(); // Ressourcen freigeben
        }

        try {
            Media media = new Media(song.getPath());
            musicPlayer = new MediaPlayer(media);
            musicPlayer.setVolume(volumeSlider.getValue()); // Lautstärke vom Slider übernehmen
            currentSongLabel.setText("Aktueller Song: " + song.getName());

            setupMediaPlayerListeners(song);
            musicPlayer.play();
        } catch (Exception e) {
            showError("Wiedergabefehler", "Der ausgewählte Titel konnte nicht abgespielt werden: " + song.getName() + "\nFehler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void pauseMusic() {
        if (musicPlayer != null) {
            musicPlayer.pause();
        }
    }

    private void stopMusic() {
        if (musicPlayer != null) {
            musicPlayer.stop();
            currentSongLabel.setText("Aktueller Song: Keiner");
            progressBar.setProgress(0);
        }
    }

    private void playPreviousSong() {
        if (playlist.isEmpty()) return;

        if (currentSongIndex > 0) {
            currentSongIndex--;
        } else {
            currentSongIndex = playlist.size() - 1; // Zum Ende der Liste springen
        }
        playSong(playlist.get(currentSongIndex));
        songListView.getSelectionModel().select(currentSongIndex);
    }

    private void playNextSong() {
        if (playlist.isEmpty()) return;

        if (isShuffleMode) {
            Random rand = new Random();
            int newIndex;
            do {
                newIndex = rand.nextInt(playlist.size());
            } while (playlist.size() > 1 && newIndex == currentSongIndex); // Vermeide den gleichen Song bei Shuffle, wenn mehr als 1 Song
            currentSongIndex = newIndex;
        } else {
            if (currentSongIndex < playlist.size() - 1) {
                currentSongIndex++;
            } else if (isRepeatMode) {
                currentSongIndex = 0; // Anfang der Liste, wenn Repeat an ist
            } else {
                stopMusic(); // Am Ende der Playlist stoppen, wenn kein Repeat
                return;
            }
        }
        playSong(playlist.get(currentSongIndex));
        songListView.getSelectionModel().select(currentSongIndex);
    }

    private void setupMediaPlayerListeners(Song song) {
        musicPlayer.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
            if (musicPlayer.getTotalDuration() != null && !musicPlayer.getTotalDuration().isUnknown()) {
                progressBar.setProgress(newVal.toMillis() / musicPlayer.getTotalDuration().toMillis());
            }
        });

        musicPlayer.setOnEndOfMedia(() -> {
            if (isRepeatMode) {
                playSong(song); // Aktuellen Song wiederholen
            } else {
                playNextSong(); // Nächsten Song spielen oder stoppen
            }
        });

        musicPlayer.setOnError(() -> {
            System.err.println("MediaPlayer Fehler für " + song.getName() + ": " + musicPlayer.getError());
            showError("Wiedergabefehler", "Beim Abspielen von " + song.getName() + " ist ein Fehler aufgetreten: " + musicPlayer.getError());
            // Optional: Zum nächsten Song springen oder den problematischen Song entfernen
            playNextSong();
        });
    }

    private void toggleShuffle() {
        isShuffleMode = !isShuffleMode;
        // Zugriff auf den Button über musicRoot und Children-Liste, da es keine direkte Referenz außerhalb der createMusicPane gibt
        // Diese Art des Zugriffs ist anfällig für Änderungen in der Layout-Struktur.
        // Besser wäre es, die ToggleButtons als Instanzvariablen zu deklarieren.
        // Für diese Ausgabe belasse ich es beim ursprünglichen Ansatz.
        ToggleButton shuffleButton = (ToggleButton) ((HBox) musicRoot.getChildren().get(6)).getChildren().get(0); // Annahme: playbackControls ist Index 6, shuffle ist erstes Kind
        shuffleButton.setText("Zufall (" + (isShuffleMode ? "An" : "Aus") + ")");
        if (isShuffleMode) {
            shuffleButton.setStyle("-fx-background-color: #4CAF50;"); // Grün für An
        } else {
            shuffleButton.setStyle(null); // Standardfarbe
        }
    }

    private void toggleRepeat() {
        isRepeatMode = !isRepeatMode;
        // Ähnlicher Zugriff wie bei shuffleButton
        ToggleButton repeatButton = (ToggleButton) ((HBox) musicRoot.getChildren().get(6)).getChildren().get(1); // Annahme: playbackControls ist Index 6, repeat ist zweites Kind
        repeatButton.setText("Wiederholen (" + (isRepeatMode ? "An" : "Aus") + ")");
        if (isRepeatMode) {
            repeatButton.setStyle("-fx-background-color: #2196F3;"); // Blau für An
        } else {
            repeatButton.setStyle(null); // Standardfarbe
        }
    }

    // Automatische Speichern/Laden Methoden (ohne FileChooser)
    private void savePlaylist() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(PLAYLIST_FILE_NAME))) {
            oos.writeObject(new ArrayList<>(playlist));
            System.out.println("Playlist wurde erfolgreich unter " + new File(PLAYLIST_FILE_NAME).getAbsolutePath() + " gespeichert.");
        } catch (IOException e) {
            showError("Fehler", "Playlist konnte nicht gespeichert werden: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadPlaylist() {
        File file = new File(PLAYLIST_FILE_NAME);
        System.out.println("Versuche, Playlist zu laden von: " + file.getAbsolutePath());
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                List<Song> loadedSongs = (List<Song>) ois.readObject();
                playlist.setAll(loadedSongs);
                currentSongIndex = 0; // Setze den Index auf den ersten Song beim Laden
                System.out.println("Playlist wurde erfolgreich von " + PLAYLIST_FILE_NAME + " geladen! (" + playlist.size() + " Songs gefunden.)");
            } catch (IOException | ClassNotFoundException e) {
                showError("Fehler", "Playlist konnte nicht geladen werden: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Keine Playlist-Datei '" + PLAYLIST_FILE_NAME + "' gefunden. Starte mit leerer Playlist.");
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
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

        public String getName() {
            return name;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}