package com.example.memorygame;

public class GameSettings {

    private int gridSize;
    private double buttonSize;
    private double pauseTime;

    public GameSettings(int gridSize, double buttonSize, double pauseTime) {
        this.gridSize = gridSize;
        this.buttonSize = buttonSize;
        this.pauseTime = pauseTime;
    }

    public static GameSettings fromDifficulty(String difficulty) {
        switch (difficulty) {
            case "Einfach":
                return new GameSettings(4, 80, 2.0);
            case "Mittel":
                return new GameSettings(6, 70, 1.5);
            case "Schwer":
                return new GameSettings(8, 60, 1.0);
            default:
                return new GameSettings(4, 80, 2.0);
        }
    }

    // Getters
    public int getGridSize() { return gridSize; }
    public double getButtonSize() { return buttonSize; }
    public double getPauseTime() { return pauseTime; }

    // Setters
    public void setGridSize(int gridSize) { this.gridSize = gridSize; }
    public void setButtonSize(double buttonSize) { this.buttonSize = buttonSize; }
    public void setPauseTime(double pauseTime) { this.pauseTime = pauseTime; }
}