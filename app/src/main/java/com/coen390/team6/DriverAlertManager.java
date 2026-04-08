package com.coen390.team6;

import android.content.Context;

import org.json.JSONObject;

public final class DriverAlertManager {

    private static final int FATIGUE_ALERT_THRESHOLD = 85;
    private static final long ALERT_COOLDOWN_MS = 15000L;

    private DriverAlertManager() {
    }

    public static void evaluateAndNotify(Context context,
                                         int heartRate,
                                         int fatigueScore,
                                         String driverState) {
        String alertLevel = resolveAlertLevel(fatigueScore, driverState);
        JSONObject activeAlert = AlertHistoryPreferences.getActiveAlert(context);

        if (alertLevel == null) {
            if (activeAlert != null) {
                AlertHistoryPreferences.archiveActiveAlert(
                        context,
                        "Resolved",
                        "Alert condition cleared and monitoring returned to a safe state."
                );
                FatigueAlertNotifier.cancel(context);
            }
            return;
        }

        long driveMinutes = getDriveTimeMinutes(context);
        long hours = driveMinutes / 60L;
        long minutes = driveMinutes % 60L;
        String driveTime = String.format(java.util.Locale.US, "%dh %02dm", hours, minutes);
        long timestampMs = System.currentTimeMillis();

        if (activeAlert != null) {
            String activeLevel = activeAlert.optString("fatigueLevel");
            long activeTimestampMs = activeAlert.optLong("timestampMs", 0L);

            if (alertLevel.equals(activeLevel)
                    && (timestampMs - activeTimestampMs) < ALERT_COOLDOWN_MS) {
                return;
            }

            AlertHistoryPreferences.archiveActiveAlert(
                    context,
                    "Resolved",
                    "A newer alert event replaced the previous active alert."
            );
        }

        String alertTitle = "High Fatigue Risk";
        String alertMessage = "Critical fatigue detected. Immediate rest is recommended for driver safety.";

        if ("DROWSY".equals(alertLevel)) {
            alertTitle = "Driver Drowsiness Detected";
            alertMessage = "Drowsy state detected from the live thresholds. Immediate rest is recommended.";
        } else if ("STRESSED".equals(alertLevel)) {
            alertTitle = "Driver Stress Detected";
            alertMessage = "Stressed state detected from the live thresholds. Check the driver condition.";
        }

        AlertHistoryPreferences.saveActiveAlert(
                context,
                alertTitle,
                alertMessage,
                fatigueScore,
                heartRate,
                timestampMs,
                alertLevel
        );

        FatigueAlertNotifier.showFatigueAlert(
                context,
                heartRate,
                alertLevel,
                driveTime,
                driveMinutes
        );
    }

    private static String resolveAlertLevel(int fatigueScore, String driverState) {
        if ("DROWSY".equals(driverState)) {
            return "DROWSY";
        }
        if ("STRESSED".equals(driverState)) {
            return "STRESSED";
        }
        if (fatigueScore >= FATIGUE_ALERT_THRESHOLD) {
            return "HIGH";
        }
        return null;
    }

    private static long getDriveTimeMinutes(Context context) {
        if (!DrivingSessionPreferences.isActive(context)) {
            return 0L;
        }

        long startedAtMs = DrivingSessionPreferences.getStartedAtMs(context);
        long elapsedMs = Math.max(0L, System.currentTimeMillis() - startedAtMs);
        return elapsedMs / 60000L;
    }
}
