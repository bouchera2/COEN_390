package com.coen390.team6;

import android.content.Context;
import android.graphics.Color;

public final class DriverFatigueStatus {

    public enum Level {
        LOW,
        MEDIUM,
        HIGH,
        WAITING
    }

    private final Level level;
    private final String label;
    private final String emoji;
    private final String scoreText;
    private final int accentColor;

    private DriverFatigueStatus(Level level, String label, String emoji, String scoreText, int accentColor) {
        this.level = level;
        this.label = label;
        this.emoji = emoji;
        this.scoreText = scoreText;
        this.accentColor = accentColor;
    }

    public static DriverFatigueStatus from(Context context) {
        boolean isConnected = BleSensorPreferences.isConnected(context);
        boolean fingerDetected = BleSensorPreferences.isFingerDetected(context);

        if (!isConnected || !fingerDetected) {
            return waiting();
        }

        if (BleSensorPreferences.hasSuddenMovement(context)) {
            return high();
        }

        String driverState = BleSensorPreferences.getDriverState(context);
        if ("DROWSY".equalsIgnoreCase(driverState)) {
            return high();
        }
        if ("STRESSED".equalsIgnoreCase(driverState)) {
            return medium();
        }
        if ("NORMAL".equalsIgnoreCase(driverState)) {
            return low();
        }

        return waiting();
    }

    public Level getLevel() {
        return level;
    }

    public String getLabel() {
        return label;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getScoreText() {
        return scoreText;
    }

    public int getAccentColor() {
        return accentColor;
    }

    private static DriverFatigueStatus low() {
        return new DriverFatigueStatus(Level.LOW, "Low", "🙂", "92/100", Color.parseColor("#22C55E"));
    }

    private static DriverFatigueStatus medium() {
        return new DriverFatigueStatus(Level.MEDIUM, "Moderate", "😐", "64/100", Color.parseColor("#F59E0B"));
    }

    private static DriverFatigueStatus high() {
        return new DriverFatigueStatus(Level.HIGH, "High", "☹", "28/100", Color.parseColor("#EF4444"));
    }

    private static DriverFatigueStatus waiting() {
        return new DriverFatigueStatus(Level.WAITING, "Waiting", "•", "--/100", Color.parseColor("#94A3B8"));
    }
}
