package com.example.memmorygame;

import java.util.*;

public class GameModel {

    private GameSettings settings;
    private String[][] gameGrid;
    private boolean[][] revealedPositions;
    private List<int[]> clickedPositions;
    private int pairs;
    private int attempts;

    public GameModel() {
        this.clickedPositions = new ArrayList<>();
    }

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

    private void generateGameSymbols() {
        int gridSize = settings.getGridSize();
        int totalPairs = (gridSize * gridSize) / 2;

        // Generate unique symbols
        String[] symbols = SymbolGenerator.generateSymbols(totalPairs);

        // Create pairs and shuffle
        List<String> gameSymbols = new ArrayList<>();
        for (String symbol : symbols) {
            gameSymbols.add(symbol);
            gameSymbols.add(symbol);
        }
        Collections.shuffle(gameSymbols);

        // Fill grid
        int index = 0;
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                gameGrid[i][j] = gameSymbols.get(index++);
                revealedPositions[i][j] = false;
            }
        }
    }

    public boolean canClick(int row, int col) {
        return !revealedPositions[row][col] && clickedPositions.size() < 2 &&
                !isAlreadyClicked(row, col);
    }

    private boolean isAlreadyClicked(int row, int col) {
        return clickedPositions.stream()
                .anyMatch(pos -> pos[0] == row && pos[1] == col);
    }

    public void addClickedPosition(int row, int col) {
        if (clickedPositions.size() < 2) {
            clickedPositions.add(new int[]{row, col});
        }
    }

    public void clearClickedPositions() {
        clickedPositions.clear();
    }

    public boolean isMatch(int row1, int col1, int row2, int col2) {
        return gameGrid[row1][col1].equals(gameGrid[row2][col2]);
    }

    public void markAsRevealed(int row, int col) {
        revealedPositions[row][col] = true;
    }

    public void incrementPairs() {
        pairs++;
    }

    public void incrementAttempts() {
        attempts++;
    }

    public boolean isGameWon() {
        int totalPairs = (settings.getGridSize() * settings.getGridSize()) / 2;
        return pairs == totalPairs;
    }

    // Getters
    public GameSettings getSettings() { return settings; }
    public String getSymbol(int row, int col) { return gameGrid[row][col]; }
    public String[][] getSymbols() { return gameGrid; }
    public boolean[][] getRevealedPositions() { return revealedPositions; }
    public int getClickedCount() { return clickedPositions.size(); }
    public int[] getClickedPosition(int index) { return clickedPositions.get(index); }
    public int getPairs() { return pairs; }
    public int getAttempts() { return attempts; }
}