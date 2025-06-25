package com.example.memorygame;

import java.util.*;

/**
 * Generiert Symbole für das Memory-Spiel.
 */
public class SymbolGenerator {

    /**
     * Generiert eine Liste von eindeutigen Symbolen.
     * @param count Anzahl der benötigten Symbole.
     * @return Array mit Symbolen.
     */
    public static String[] generateSymbols(int count) {
        List<String> symbolPool = createSymbolPool();

        if (symbolPool.size() < count) {
            throw new IllegalStateException("Not enough unique symbols available for grid size.");
        }

        Collections.shuffle(symbolPool);
        String[] selectedSymbols = new String[count];
        for (int i = 0; i < count; i++) {
            selectedSymbols[i] = symbolPool.get(i);
        }

        return selectedSymbols;
    }

    /**
     * Erstellt den Symbolpool.
     * @return Liste mit Symbolen.
     */
    private static List<String> createSymbolPool() {
        List<String> pool = new ArrayList<>();

        for (char c = 'A'; c <= 'Z'; c++) {
            pool.add(String.valueOf(c));
        }

        for (char c = 'a'; c <= 'z'; c++) {
            pool.add(String.valueOf(c));
        }

        for (int i = 0; i < 100; i++) {
            pool.add(String.valueOf(i));
        }

        String[] specialSymbols = {"♠", "♣", "♥", "♦", "★", "☆", "♪", "♫", "☀", "☽", "☂", "☃"};
        pool.addAll(Arrays.asList(specialSymbols));

        return pool;
    }
}