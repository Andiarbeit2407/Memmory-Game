package com.example.memorygame;

import javafx.scene.control.Button;

public class GameBoard {

    private Button[][] buttons;
    private int gridSize;
    private double buttonSize;
    private ButtonClickHandler clickHandler;

    @FunctionalInterface
    public interface ButtonClickHandler {
        void onClick(int row, int col);
    }

    public GameBoard(int gridSize, double buttonSize, ButtonClickHandler clickHandler) {
        this.gridSize = gridSize;
        this.buttonSize = buttonSize;
        this.clickHandler = clickHandler;
        createButtons();
    }

    private void createButtons() {
        buttons = new Button[gridSize][gridSize];

        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                Button btn = new Button();
                btn.setMinSize(buttonSize, buttonSize);
                btn.setMaxSize(buttonSize, buttonSize);

                // Adjust font size based on grid size
                int fontSize = getFontSize();
                btn.setStyle("-fx-font-size: " + fontSize + "px; -fx-background-color: #4a4a4a; -fx-text-fill: white;");

                final int row = i;
                final int col = j;
                btn.setOnAction(e -> clickHandler.onClick(row, col));

                buttons[i][j] = btn;
            }
        }
    }

    private int getFontSize() {
        if (gridSize == 4) return 24;
        else if (gridSize == 6) return 18;
        else return 14; // for gridSize == 8
    }

    public void reset(String[][] symbols) {
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                buttons[i][j].setText("");
                buttons[i][j].setDisable(false);
                buttons[i][j].setStyle("-fx-font-size: " + getFontSize() + "px; -fx-background-color: #4a4a4a; -fx-text-fill: white;");
            }
        }
    }

    public void revealButton(int row, int col, String symbol) {
        buttons[row][col].setText(symbol);
        buttons[row][col].setStyle("-fx-font-size: " + getFontSize() + "px; -fx-background-color: #6a6a6a; -fx-text-fill: white;");
    }

    public void hideButton(int row, int col) {
        buttons[row][col].setText("");
        buttons[row][col].setStyle("-fx-font-size: " + getFontSize() + "px; -fx-background-color: #4a4a4a; -fx-text-fill: white;");
    }

    public void markAsFound(int row, int col) {
        buttons[row][col].setDisable(true);
        buttons[row][col].setStyle("-fx-font-size: " + getFontSize() + "px; -fx-background-color: #2d5a3d; -fx-text-fill: #90ee90;");
    }

    public void disableAllButtons() {
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                buttons[i][j].setDisable(true);
            }
        }
    }

    public void enableUnrevealedButtons(boolean[][] revealedPositions) {
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                if (!revealedPositions[i][j]) {
                    buttons[i][j].setDisable(false);
                }
            }
        }
    }

    public Button[][] getButtons() {
        return buttons;
    }
}