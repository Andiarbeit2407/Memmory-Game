package com.example.memorygame;

import javafx.scene.control.Button;

/**
 * Stellt das Spielfeld für das Memory-Spiel dar.
 */
public class GameBoard {

    private Button[][] buttons;
    private int gridSize;
    private double buttonSize;
    private ButtonClickHandler clickHandler;

    /**
     * Funktionales Interface für Button-Klicks.
     */
    @FunctionalInterface
    public interface ButtonClickHandler {
        void onClick(int row, int col);
    }

    /**
     * Konstruktor.
     * @param gridSize Größe des Spielfelds
     * @param buttonSize Größe der Buttons
     * @param clickHandler Handler für Klicks
     */
    public GameBoard(int gridSize, double buttonSize, ButtonClickHandler clickHandler) {
        this.gridSize = gridSize;
        this.buttonSize = buttonSize;
        this.clickHandler = clickHandler;
        createButtons();
    }

    /**
     * Erstellt die Buttons für das Spielfeld.
     */
    private void createButtons() {
        buttons = new Button[gridSize][gridSize];

        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                Button btn = new Button();
                btn.setMinSize(buttonSize, buttonSize);
                btn.setMaxSize(buttonSize, buttonSize);

                int fontSize = getFontSize();
                btn.setStyle("-fx-font-size: " + fontSize + "px; -fx-background-color: #4a4a4a; -fx-text-fill: white;");

                final int row = i;
                final int col = j;
                btn.setOnAction(e -> clickHandler.onClick(row, col));

                buttons[i][j] = btn;
            }
        }
    }

    /**
     * Gibt die empfohlene Schriftgröße zurück.
     * @return Schriftgröße
     */
    private int getFontSize() {
        if (gridSize == 4) return 24;
        else if (gridSize == 6) return 18;
        else return 14;
    }

    /**
     * Setzt das Spielfeld zurück.
     * @param symbols Neue Symbole
     */
    public void reset(String[][] symbols) {
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                buttons[i][j].setText("");
                buttons[i][j].setDisable(false);
                buttons[i][j].setStyle("-fx-font-size: " + getFontSize() + "px; -fx-background-color: #4a4a4a; -fx-text-fill: white;");
            }
        }
    }

    /**
     * Zeigt das Symbol auf einem Button an.
     * @param row Zeile
     * @param col Spalte
     * @param symbol Symbol
     */
    public void revealButton(int row, int col, String symbol) {
        buttons[row][col].setText(symbol);
        buttons[row][col].setStyle("-fx-font-size: " + getFontSize() + "px; -fx-background-color: #6a6a6a; -fx-text-fill: white;");
    }

    /**
     * Versteckt das Symbol auf einem Button.
     * @param row Zeile
     * @param col Spalte
     */
    public void hideButton(int row, int col) {
        buttons[row][col].setText("");
        buttons[row][col].setStyle("-fx-font-size: " + getFontSize() + "px; -fx-background-color: #4a4a4a; -fx-text-fill: white;");
    }

    /**
     * Markiert einen Button als gefunden.
     * @param row Zeile
     * @param col Spalte
     */
    public void markAsFound(int row, int col) {
        buttons[row][col].setDisable(true);
        buttons[row][col].setStyle("-fx-font-size: " + getFontSize() + "px; -fx-background-color: #2d5a3d; -fx-text-fill: #90ee90;");
    }

    /**
     * Deaktiviert alle Buttons.
     */
    public void disableAllButtons() {
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                buttons[i][j].setDisable(true);
            }
        }
    }

    /**
     * Aktiviert alle nicht aufgedeckten Buttons.
     * @param revealedPositions Matrix der aufgedeckten Felder
     */
    public void enableUnrevealedButtons(boolean[][] revealedPositions) {
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                if (!revealedPositions[i][j]) {
                    buttons[i][j].setDisable(false);
                }
            }
        }
    }

    /**
     * Gibt das Button-Array zurück.
     * @return Buttons
     */
    public Button[][] getButtons() {
        return buttons;
    }
}