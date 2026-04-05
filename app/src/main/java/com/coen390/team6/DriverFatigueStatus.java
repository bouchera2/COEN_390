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
    private final String dashboardTitle;
    private final String dashboardDescription;
    private final int riskPercent;
    private final int accentColor;

    private DriverFatigueStatus(
            Level level,
            String label,
            String emoji,
            String scoreText,
            String dashboardTitle,
            String dashboardDescription,
            int riskPercent,
            int accentColor
    ) {
        this.level = level;
        this.label = label;
        this.emoji = emoji;
        this.scoreText = scoreText;
        this.dashboardTitle = dashboardTitle;
        this.dashboardDescription = dashboardDescription;
        this.riskPercent = riskPercent;
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

    public String getDashboardTitle() {
        return dashboardTitle;
    }

    public String getDashboardDescription() {
        return dashboardDescription;
    }

    public int getRiskPercent() {
        return riskPercent;
    }

    private static DriverFatigueStatus low() {
        return new DriverFatigueStatus(
                Level.LOW,
                "Low",
                "🙂",
                "92/100",
                "Fatigue Risk: Low",
                "Optimal condition for driving.",
                15,
                Color.parseColor("#22C55E")
        );
    }

    private static DriverFatigueStatus medium() {
        return new DriverFatigueStatus(
                Level.MEDIUM,
                "Moderate",
                "😐",
                "64/100",
                "Fatigue Risk: Moderate",
                "Monitor the driver and plan a break soon.",
                55,
                Color.parseColor("#F59E0B")
        );
    }

    private static DriverFatigueStatus high() {
        return new DriverFatigueStatus(
                Level.HIGH,
                "High",
                "☹",
                "28/100",
                "Fatigue Risk: High",
                "Critical state detected. Stop driving and rest.",
                85,
                Color.parseColor("#EF4444")
        );
    }

    private static DriverFatigueStatus waiting() {
        return new DriverFatigueStatus(
                Level.WAITING,
                "Waiting",
                "•",
                "--/100",
                "Fatigue Risk: Waiting",
                "Waiting for enough sensor data to estimate risk.",
                0,
                Color.parseColor("#94A3B8")
        );
    }
}
