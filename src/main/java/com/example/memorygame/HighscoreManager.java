package com.example.memorygame;

import java.io.*;
import java.util.*;

/**
 * Verwaltet die Highscores für das Memory-Spiel.
 */
public class HighscoreManager {

    private static final String HIGHSCORE_FILE = "highscores.txt";
    private List<HighscoreEntry> highscores;

    /**
     * Konstruktor. Lädt Highscores aus Datei.
     */
    public HighscoreManager() {
        this.highscores = new ArrayList<>();
        loadHighscores();
    }

    /**
     * Speichert einen neuen Score und hält die Top 5 pro Schwierigkeitsgrad.
     * @param playerName Spielername
     * @param difficulty Schwierigkeitsgrad
     * @param timeInSeconds Zeit in Sekunden
     * @param attempts Versuche
     */
    public void saveScore(String playerName, String difficulty, long timeInSeconds, int attempts) {
        HighscoreEntry entry = new HighscoreEntry(playerName, difficulty, timeInSeconds, attempts);
        highscores.add(entry);

        // Sort by time (ascending) and keep only top 10 per difficulty
        Collections.sort(highscores);

        // Keep only top 5 per difficulty
        Map<String, List<HighscoreEntry>> byDifficulty = new HashMap<>();
        for (HighscoreEntry e : highscores) {
            byDifficulty.computeIfAbsent(e.getDifficulty(), k -> new ArrayList<>()).add(e);
        }

        highscores.clear();
        for (List<HighscoreEntry> entries : byDifficulty.values()) {
            highscores.addAll(entries.subList(0, Math.min(5, entries.size())));
        }

        Collections.sort(highscores);
        saveHighscores();
    }

    /**
     * Gibt die Highscores formatiert als String zurück.
     * @return Formatierter Highscore-Text.
     */
    public String getFormattedHighscores() {
        if (highscores.isEmpty()) {
            return "No high scores yet!";
        }

        StringBuilder sb = new StringBuilder();
        String currentDifficulty = "";

        for (HighscoreEntry entry : highscores) {
            if (!entry.getDifficulty().equals(currentDifficulty)) {
                if (!currentDifficulty.isEmpty()) {
                    sb.append("\n");
                }
                currentDifficulty = entry.getDifficulty();
                sb.append(currentDifficulty).append(":\n");
            }

            sb.append(String.format("  %s - %s (%d attempts)\n",
                    entry.getPlayerName(),
                    formatTime(entry.getTimeInSeconds()),
                    entry.getAttempts()));
        }

        return sb.toString();
    }

    /**
     * Formatiert die Zeit als mm:ss.
     * @param seconds Sekunden
     * @return Formatierte Zeit
     */
    private String formatTime(long seconds) {
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    /**
     * Lädt Highscores aus Datei.
     */
    private void loadHighscores() {
        try (BufferedReader reader = new BufferedReader(new FileReader(HIGHSCORE_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    String name = parts[0];
                    String difficulty = parts[1];
                    long time = Long.parseLong(parts[2]);
                    int attempts = Integer.parseInt(parts[3]);
                    highscores.add(new HighscoreEntry(name, difficulty, time, attempts));
                }
            }
            Collections.sort(highscores);
        } catch (IOException e) {
            // Datei existiert noch nicht
        }
    }

    /**
     * Speichert Highscores in Datei.
     */
    private void saveHighscores() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(HIGHSCORE_FILE))) {
            for (HighscoreEntry entry : highscores) {
                writer.println(entry.getPlayerName() + "," +
                        entry.getDifficulty() + "," +
                        entry.getTimeInSeconds() + "," +
                        entry.getAttempts());
            }
        } catch (IOException e) {
            System.err.println("Could not save high scores: " + e.getMessage());
        }
    }

    /**
     * Repräsentiert einen Highscore-Eintrag.
     */
    private static class HighscoreEntry implements Comparable<HighscoreEntry> {
        private String playerName;
        private String difficulty;
        private long timeInSeconds;
        private int attempts;

        /**
         * Konstruktor.
         */
        public HighscoreEntry(String playerName, String difficulty, long timeInSeconds, int attempts) {
            this.playerName = playerName;
            this.difficulty = difficulty;
            this.timeInSeconds = timeInSeconds;
            this.attempts = attempts;
        }

        @Override
        public int compareTo(HighscoreEntry other) {
            // First sort by difficulty (Einfach, Mittel, Schwer)
            int difficultyComparison = getDifficultyOrder(this.difficulty) - getDifficultyOrder(other.difficulty);
            if (difficultyComparison != 0) {
                return difficultyComparison;
            }

            // Then by time (ascending)
            return Long.compare(this.timeInSeconds, other.timeInSeconds);
        }

        private int getDifficultyOrder(String difficulty) {
            switch (difficulty) {
                case "Einfach": return 1;
                case "Mittel": return 2;
                case "Schwer": return 3;
                default: return 4;
            }
        }

        // Getters
        public String getPlayerName() { return playerName; }
        public String getDifficulty() { return difficulty; }
        public long getTimeInSeconds() { return timeInSeconds; }
        public int getAttempts() { return attempts; }
    }
}