package com.example.memorygame;

import java.util.*;

/**
 * Modelliert den Zustand des Memory-Spiels.
 */
public class GameModel {

    private GameSettings settings;
    private String[][] gameGrid;
    private boolean[][] revealedPositions;
    private List<int[]> clickedPositions;
    private int pairs;
    private int attempts;

    /**
     * Konstruktor.
     */
    public GameModel() {
        this.clickedPositions = new ArrayList<>();
    }

    /**
     * Initialisiert das Spielmodell mit den gegebenen Einstellungen.
     * @param settings Spieleinstellungen
     */
    public void initialize(GameSettings settings) {
        this.settings = settings;
        int gridSize = settings.getGridSize();

        this.gameGrid = new String[gridSize][gridSize];
        this.revealedPositions = new boolean[gridSize][gridSize];
        this.clickedPositions.clear();
        this.pairs = 0;
        this.attempts = 0;

        generateGameSymbols();
    }

    /**
     * Generiert und verteilt die Symbole auf dem Spielfeld.
     */
    private void generateGameSymbols() {
        int gridSize = settings.getGridSize();
        int totalPairs = (gridSize * gridSize) / 2;

        String[] symbols = SymbolGenerator.generateSymbols(totalPairs);

        List<String> gameSymbols = new ArrayList<>();
        for (String symbol : symbols) {
            gameSymbols.add(symbol);
            gameSymbols.add(symbol);
        }
        Collections.shuffle(gameSymbols);

        int index = 0;
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                gameGrid[i][j] = gameSymbols.get(index++);
                revealedPositions[i][j] = false;
            }
        }
    }

    /**
     * Prüft, ob ein Feld angeklickt werden kann.
     * @param row Zeile
     * @param col Spalte
     * @return true, wenn klickbar
     */
    public boolean canClick(int row, int col) {
        return !revealedPositions[row][col] && clickedPositions.size() < 2 &&
                !isAlreadyClicked(row, col);
    }

    private boolean isAlreadyClicked(int row, int col) {
        return clickedPositions.stream()
                .anyMatch(pos -> pos[0] == row && pos[1] == col);
    }

    /**
     * Fügt eine angeklickte Position hinzu.
     * @param row Zeile
     * @param col Spalte
     */
    public void addClickedPosition(int row, int col) {
        if (clickedPositions.size() < 2) {
            clickedPositions.add(new int[]{row, col});
        }
    }

    /**
     * Leert die Liste der angeklickten Positionen.
     */
    public void clearClickedPositions() {
        clickedPositions.clear();
    }

    /**
     * Prüft, ob zwei Felder ein Paar bilden.
     * @return true, wenn gleich
     */
    public boolean isMatch(int row1, int col1, int row2, int col2) {
        return gameGrid[row1][col1].equals(gameGrid[row2][col2]);
    }

    /**
     * Markiert ein Feld als aufgedeckt.
     * @param row Zeile
     * @param col Spalte
     */
    public void markAsRevealed(int row, int col) {
        revealedPositions[row][col] = true;
    }

    /**
     * Erhöht die Anzahl gefundener Paare.
     */
    public void incrementPairs() {
        pairs++;
    }

    /**
     * Erhöht die Anzahl der Versuche.
     */
    public void incrementAttempts() {
        attempts++;
    }

    /**
     * Prüft, ob das Spiel gewonnen ist.
     * @return true, wenn alle Paare gefunden
     */
    public boolean isGameWon() {
        int totalPairs = (settings.getGridSize() * settings.getGridSize()) / 2;
        return pairs == totalPairs;
    }

    public GameSettings getSettings() { return settings; }
    public String getSymbol(int row, int col) { return gameGrid[row][col]; }
    public String[][] getSymbols() { return gameGrid; }
    public boolean[][] getRevealedPositions() { return revealedPositions; }
    public int getClickedCount() { return clickedPositions.size(); }
    public int[] getClickedPosition(int index) { return clickedPositions.get(index); }
    public int getPairs() { return pairs; }
    public int getAttempts() { return attempts; }
}