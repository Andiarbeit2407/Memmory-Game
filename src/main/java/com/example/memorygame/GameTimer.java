package com.example.memorygame;

import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

public class GameTimer {

    private Timeline timeline;
    private long startTime;
    private long endTime;
    private boolean isActive;
    private Runnable updateCallback;

    public GameTimer(Runnable updateCallback) {
        this.updateCallback = updateCallback;
    }

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

    public void stop() {
        isActive = false;
        if (timeline != null) {
            timeline.stop();
        }
        endTime = System.currentTimeMillis();
    }

    public void pause() {
        if (timeline != null && isActive) {
            timeline.pause();
        }
    }

    public void resume() {
        if (timeline != null && isActive) {
            timeline.play();
        }
    }

    public long getElapsedTimeInSeconds() {
        if (endTime > startTime) {
            return (endTime - startTime) / 1000;
        } else if (isActive) {
            return (System.currentTimeMillis() - startTime) / 1000;
        }
        return 0;
    }

    public String getFormattedTime() {
        long seconds = getElapsedTimeInSeconds();
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    public boolean isActive() {
        return isActive;
    }
}