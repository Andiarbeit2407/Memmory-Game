package com.example.memorygame;

/**
 * Enthält die Einstellungen für das Memory-Spiel.
 */
public class GameSettings {

    private int gridSize;
    private double buttonSize;
    private double pauseTime;

    /**
     * Konstruktor.
     * @param gridSize Größe des Spielfelds
     * @param buttonSize Größe der Buttons
     * @param pauseTime Pausenzeit nach einem Zug
     */
    public GameSettings(int gridSize, double buttonSize, double pauseTime) {
        this.gridSize = gridSize;
        this.buttonSize = buttonSize;
        this.pauseTime = pauseTime;
    }

    /**
     * Erstellt Einstellungen basierend auf dem Schwierigkeitsgrad.
     * @param difficulty Schwierigkeitsgrad
     * @return GameSettings-Objekt
     */
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

    public int getGridSize() { return gridSize; }
    public double getButtonSize() { return buttonSize; }
    public double getPauseTime() { return pauseTime; }

    public void setGridSize(int gridSize) { this.gridSize = gridSize; }
    public void setButtonSize(double buttonSize) { this.buttonSize = buttonSize; }
    public void setPauseTime(double pauseTime) { this.pauseTime = pauseTime; }
}