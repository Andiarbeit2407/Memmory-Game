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

    // Musik-Manager Attribute (Unverändert gelassen)
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


    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Startseite erstellen
        VBox startPageRoot = createStartPage();
        startScene = new Scene(startPageRoot, 800, 600); // Standardgröße für Startseite

        // Spiel-Panel erstellen (initialisiert nur das Layout, nicht die Buttons)
        VBox gamePaneRoot = createGamePane();
        gameScene = new Scene(gamePaneRoot); // Keine feste Größe mehr hier, wird dynamisch angepasst

        // Wiedergabeliste laden
        // Die Playlist sollte hier geladen werden, bevor die primaryStage.setScene() aufgerufen wird,
        // falls die Musik beim Start automatisch spielen soll oder der Musikmanager direkt aufgerufen wird.
        loadPlaylist();

        // --- Dark Mode CSS anwenden ---
        // Stellen Sie sicher, dass dark-mode.css im Ressourcenordner (z.B. src/main/resources) liegt.
        startScene.getStylesheets().add(getClass().getResource("/dark-mode.css").toExternalForm());
        gameScene.getStylesheets().add(getClass().getResource("/dark-mode.css").toExternalForm());

        primaryStage.setScene(startScene);
        primaryStage.setTitle("Memory Game");

        // NEUE FUNKTIONALITÄT: Event-Handler für den Schließen-Knopf des Fensters
        primaryStage.setOnCloseRequest(event -> {
            // Verhindert das sofortige Schließen des Fensters
            event.consume();

            // Optional: Zusätzlicher Bestätigungsdialog vor dem CAPTCHA
            if (showConfirmationDialog("Anwendung beenden", "Möchten Sie die Anwendung wirklich beenden? Sie müssen ein CAPTCHA lösen.")) {
                if (showCaptchaDialog()) {
                    // CAPTCHA korrekt gelöst und Bestätigung gegeben
                    // Musik stoppen, wenn die Anwendung geschlossen wird
                    if (musicPlayer != null) {
                        musicPlayer.stop();
                        musicPlayer.dispose(); // Wichtig: Ressourcen freigeben
                    }
                    primaryStage.close(); // Fenster schließen
                    // Optional: System.exit(0); für sauberen Exit, wenn nicht alle Threads selbst beendet werden
                    // (Oft nicht nötig bei sauberer JavaFX Applikation)
                } else {
                    // CAPTCHA falsch
                    showError("CAPTCHA Fehler", "Das CAPTCHA war falsch. Die Anwendung bleibt geöffnet.");
                }
            } else {
                // Benutzer hat "Abbrechen" im Bestätigungsdialog gewählt
                System.out.println("Schließen abgebrochen."); // Nur für Debugging
            }
        });

        primaryStage.show();
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
            // ANGEPASSTE LOGIK FÜR DEN BEENDEN-BUTTON
            if (showConfirmationDialog("Anwendung beenden", "Möchtest du die Anwendung wirklich beenden? Du musst ein CAPTCHA lösen.")) {
                if (showCaptchaDialog()) {
                    // CAPTCHA korrekt gelöst
                    // Musik stoppen, wenn die Anwendung geschlossen wird
                    if (musicPlayer != null) {
                        musicPlayer.stop();
                        musicPlayer.dispose();
                    }
                    primaryStage.close(); // Anwendung schließen
                } else {
                    // CAPTCHA falsch
                    showError("CAPTCHA Fehler", "Das CAPTCHA war falsch. Die Anwendung bleibt geöffnet.");
                }
            }
            // Wenn der Benutzer den ersten Bestätigungsdialog abbricht, passiert nichts.
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
        musicRoot = new VBox(10); // Speichere Referenz im Instanzfeld
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

        // Toggle Buttons als Instanzvariablen (oder final in Methode) für einfacheren Zugriff
        // Ich belasse die ursprüngliche, weniger robuste Methode bei, wie vom Benutzer gewünscht.
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
        Scene musicScene = new Scene(createMusicPane(), 800, 600);
        musicScene.getStylesheets().add(getClass().getResource("/dark-mode.css").toExternalForm()); // Dark Mode auch für den Music Manager
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
        // Zusätzliche Sonderzeichen oder Unicode-Symbole könnten hier hinzugefügt werden, falls benötigt

        if (pool.size() < count) {
            // Sollte mit der aktuellen Pool-Größe und max. 8x8 Gitter (32 Paare) nicht passieren.
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
                // Schriftgröße wird bereits in buildGameGrid gesetzt
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
            for (int i = 0; i < gridSize; i++) { // Loop über aktuelle gridSize
                for (int j = 0; j < gridSize; j++) { // Loop über aktuelle gridSize
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
                for (int i = 0; i < gridSize; i++) { // Loop über aktuelle gridSize
                    for (int j = 0; j < gridSize; j++) { // Loop über aktuelle gridSize
                        if (!aufgedeckt[i][j]) {
                            buttons[i][j].setDisable(false);
                        }
                    }
                }

                // Gewinnbedingung an die Gittergröße anpassen
                if (paare == (gridSize * gridSize) / 2) {
                    if (showCaptchaDialog()) { // CAPTCHA vor dem Speichern der Highscores
                        showHighscores();
                        // Optional: Direkt ein neues Spiel starten oder zurück zur Startseite
                        if (showConfirmationDialog("Spiel beendet!", "Herzlichen Glückwunsch! Du hast alle Paare gefunden!\n" +
                                "Möchtest du ein neues Spiel starten?")) {
                            neuesSpiel();
                        } else {
                            primaryStage.setScene(startScene);
                        }
                        savePlayerData(currentPlayerName, currentDifficulty); // Highscore speichern
                    } else {
                        showError("CAPTCHA Fehler", "Das CAPTCHA war falsch. Highscore wird nicht gespeichert.");
                        // Spiel zurücksetzen oder beenden
                        neuesSpiel();
                    }
                }
            });
            pause.play();
        }
    }

    private int[] findeButtonPosition(Button btn) {
        for (int i = 0; i < gridSize; i++) { // Loop über aktuelle gridSize
            for (int j = 0; j < gridSize; j++) { // Loop über aktuelle gridSize
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
        // Stellen Sie sicher, dass alle Buttons wieder aktiviert sind, falls sie durch den CAPTCHA-Dialog deaktiviert wurden
        for (int i = 0; i < gridSize; i++) { // Loop über aktuelle gridSize
            for (int j = 0; j < gridSize; j++) { // Loop über aktuelle gridSize
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
        final double CONTROLS_HEIGHT_ESTIMATE = 80.0; // Geschätzte Höhe der unteren Kontrollleiste (inkl. Padding)
        final double SCENE_PADDING_VERTICAL = 40.0; // Z.B. 20px oben + 20px unten um das Gitter
        final double SCENE_PADDING_HORIZONTAL = 40.0; // Z.B. 20px links + 20px rechts um das Gitter

        // Berechnung der benötigten Szenengröße
        double requiredGridWidth = (gridSize * calculatedButtonSize) + ((gridSize - 1) * H_GAP);
        double requiredGridHeight = (gridSize * calculatedButtonSize) + ((gridSize - 1) * V_GAP);

        double newWidth = requiredGridWidth + SCENE_PADDING_HORIZONTAL;
        double newHeight = requiredGridHeight + CONTROLS_HEIGHT_ESTIMATE + SCENE_PADDING_VERTICAL;

        // Mindest- und Maximalgrößen für das Fenster, um extreme Größen zu vermeiden
        if (newWidth < 500) newWidth = 500;
        if (newHeight < 400) newHeight = 400;
        if (newWidth > 1000) newWidth = 1000;
        if (newHeight > 900) newHeight = 900;


        // primaryStage anpassen
        primaryStage.setWidth(newWidth);
        primaryStage.setHeight(newHeight);
        primaryStage.centerOnScreen(); // Fenster zentrieren

        buildGameGrid(); // Erstellt das Gitter und die Buttons mit der neuen gridSize und calculatedButtonSize
        spielInitialisieren(); // Initialisiert das Spiel mit den neuen Einstellungen
        primaryStage.setScene(gameScene); // Setzt die Szene, die sich nun an die Fenstergröße anpassen wird
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
        // Optional: Highscores sortieren, z.B. nach Versuchen aufsteigend
        highscores.stream()
                .map(line -> {
                    String[] parts = line.split(";");
                    if (parts.length == 3) {
                        try {
                            return new AbstractMap.SimpleEntry<>(Integer.parseInt(parts[2]), parts[0] + " (" + parts[1] + "): " + parts[2] + " Versuche");
                        } catch (NumberFormatException e) {
                            return new AbstractMap.SimpleEntry<>(Integer.MAX_VALUE, line); // Ungültige Einträge ans Ende
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
        alert.getDialogPane().setExpanded(true); // Direkt erweitern

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
            }
        }
        // Wenn "Fortsetzen" gewählt wird oder der Dialog geschlossen wird, passiert nichts.
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
            savePlaylist();
        }
    }

    private void removeSelectedSong() {
        Song selectedSong = songListView.getSelectionModel().getSelectedItem();
        if (selectedSong != null) {
            if (showConfirmationDialog("Song entfernen", "Möchten Sie '" + selectedSong.getName() + "' wirklich aus der Wiedergabeliste entfernen?")) {
                if (musicPlayer != null && selectedSong.getPath().equals(playlist.get(currentSongIndex).getPath())) {
                    stopMusic(); // Aktuell spielenden Song stoppen, wenn er entfernt wird
                }
                playlist.remove(selectedSong);
                savePlaylist();
                // Index anpassen, falls der entfernte Song vor dem aktuellen Song war
                if (currentSongIndex >= playlist.size() && !playlist.isEmpty()) {
                    currentSongIndex = playlist.size() - 1;
                } else if (playlist.isEmpty()) {
                    currentSongIndex = 0;
                    currentSongLabel.setText("Aktueller Song: Keiner");
                    progressBar.setProgress(0);
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
    }

    private void toggleShuffle() {
        isShuffleMode = !isShuffleMode;
        // Ich belasse den ursprünglichen Zugriff auf die ToggleButtons, wie er im Code war.
        ToggleButton shuffleButton = (ToggleButton) ((HBox) ((VBox) songListView.getParent()).getChildren().get(5)).getChildren().get(0);
        shuffleButton.setText("Zufall (" + (isShuffleMode ? "An" : "Aus") + ")");
        if (isShuffleMode) {
            shuffleButton.setStyle("-fx-background-color: #4CAF50;"); // Grün für An
        } else {
            shuffleButton.setStyle(null); // Standardfarbe
        }
    }

    private void toggleRepeat() {
        isRepeatMode = !isRepeatMode;
        // Ich belasse den ursprünglichen Zugriff auf die ToggleButtons, wie er im Code war.
        ToggleButton repeatButton = (ToggleButton) ((HBox) ((VBox) songListView.getParent()).getChildren().get(5)).getChildren().get(1);
        repeatButton.setText("Wiederholen (" + (isRepeatMode ? "An" : "Aus") + ")");
        if (isRepeatMode) {
            repeatButton.setStyle("-fx-background-color: #2196F3;"); // Blau für An
        } else {
            repeatButton.setStyle(null); // Standardfarbe
        }
    }

    private void savePlaylist() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("playlist.ser"))) {
            oos.writeObject(new ArrayList<>(playlist)); // Serialize the list of songs
        } catch (IOException e) {
            showError("Fehler", "Playlist konnte nicht gespeichert werden: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadPlaylist() {
        File file = new File("playlist.ser");
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                List<Song> loadedSongs = (List<Song>) ois.readObject();
                playlist.setAll(loadedSongs); // Replace existing playlist with loaded songs
            } catch (IOException | ClassNotFoundException e) {
                showError("Fehler", "Playlist konnte nicht geladen werden: " + e.getMessage());
                e.printStackTrace();
            }
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