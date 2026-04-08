package com.coen390.team6;

public final class BatteryEstimator {

    private static final int MAX_BATTERY_HOURS = 12;
    private static final int TOTAL_BARS = 5;

    private BatteryEstimator() {
    }

    public static String getBatteryBars(long driveTimeMinutes) {
        double hoursUsed = driveTimeMinutes / 60.0;
        double remaining = Math.max(0, MAX_BATTERY_HOURS - hoursUsed);
        int filledBars = (int) Math.ceil((remaining / MAX_BATTERY_HOURS) * TOTAL_BARS);
        filledBars = Math.max(0, Math.min(TOTAL_BARS, filledBars));

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filledBars; i++) {
            sb.append("█");
        }
        for (int i = filledBars; i < TOTAL_BARS; i++) {
            sb.append("░");
        }
        return sb.toString();
    }

    public static int getBatteryPercent(long driveTimeMinutes) {
        double hoursUsed = driveTimeMinutes / 60.0;
        double remaining = Math.max(0, MAX_BATTERY_HOURS - hoursUsed);
        return (int) ((remaining / MAX_BATTERY_HOURS) * 100);
    }

    public static String getBatteryColor(long driveTimeMinutes) {
        int percent = getBatteryPercent(driveTimeMinutes);
        if (percent > 60) {
            return "#4ADE80";
        }
        if (percent > 30) {
            return "#FBBF24";
        }
        return "#EF4444";
    }

    public static int getFilledBars(long driveTimeMinutes) {
        double hoursUsed = driveTimeMinutes / 60.0;
        double remaining = Math.max(0, MAX_BATTERY_HOURS - hoursUsed);
        int bars = (int) Math.ceil((remaining / MAX_BATTERY_HOURS) * TOTAL_BARS);
        return Math.max(0, Math.min(TOTAL_BARS, bars));
    }
}
