package com.example.memorygame;

import java.util.*;

public class SymbolGenerator {

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

    private static List<String> createSymbolPool() {
        List<String> pool = new ArrayList<>();

        // Add letters A-Z
        for (char c = 'A'; c <= 'Z'; c++) {
            pool.add(String.valueOf(c));
        }

        // Add letters a-z
        for (char c = 'a'; c <= 'z'; c++) {
            pool.add(String.valueOf(c));
        }

        // Add numbers 0-99
        for (int i = 0; i < 100; i++) {
            pool.add(String.valueOf(i));
        }

        // Add some special symbols
        String[] specialSymbols = {"♠", "♣", "♥", "♦", "★", "☆", "♪", "♫", "☀", "☽", "☂", "☃"};
        pool.addAll(Arrays.asList(specialSymbols));

        return pool;
    }
}