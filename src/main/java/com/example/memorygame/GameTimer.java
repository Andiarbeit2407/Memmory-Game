package com.example.memorygame;

import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

/**
 * Timer für das Memory-Spiel.
 */
public class GameTimer {

    private Timeline timeline;
    private long startTime;
    private long endTime;
    private boolean isActive;
    private Runnable updateCallback;

    /**
     * Konstruktor.
     * @param updateCallback Callback für UI-Updates.
     */
    public GameTimer(Runnable updateCallback) {
        this.updateCallback = updateCallback;
    }

    /**
     * Startet den Timer.
     */
    public void start() {
        startTime = System.currentTimeMillis();
        isActive = true;

        if (timeline != null) {
            timeline.stop();
        }

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (updateCallback != null) {
                updateCallback.run();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    /**
     * Stoppt den Timer.
     */
    public void stop() {
        isActive = false;
        if (timeline != null) {
            timeline.stop();
        }
        endTime = System.currentTimeMillis();
    }

    /**
     * Pausiert den Timer.
     */
    public void pause() {
        if (timeline != null && isActive) {
            timeline.pause();
        }
    }

    /**
     * Setzt den Timer fort.
     */
    public void resume() {
        if (timeline != null && isActive) {
            timeline.play();
        }
    }

    /**
     * Gibt die vergangene Zeit in Sekunden zurück.
     * @return Sekunden
     */
    public long getElapsedTimeInSeconds() {
        if (endTime > startTime) {
            return (endTime - startTime) / 1000;
        } else if (isActive) {
            return (System.currentTimeMillis() - startTime) / 1000;
        }
        return 0;
    }

    /**
     * Gibt die vergangene Zeit formatiert als mm:ss zurück.
     * @return Formatierte Zeit
     */
    public String getFormattedTime() {
        long seconds = getElapsedTimeInSeconds();
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    /**
     * Gibt zurück, ob der Timer aktiv ist.
     * @return true, wenn aktiv
     */
    public boolean isActive() {
        return isActive;
    }
}