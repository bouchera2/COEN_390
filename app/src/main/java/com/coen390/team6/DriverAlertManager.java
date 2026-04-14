package com.coen390.team6;

import android.content.Context;
import android.content.Intent;

import org.json.JSONObject;

public final class DriverAlertManager {

    private static final long CONNECTION_GRACE_PERIOD_MS = 60000L;
    private static final long ALERT_COOLDOWN_MS = 15000L;

    private DriverAlertManager() {
    }

    public static void evaluateAndNotify(Context context,
                                         int heartRate,
                                         int fatigueScore,
                                         String driverState) {
        String alertLevel = resolveAlertLevel(fatigueScore);
        JSONObject activeAlert = AlertHistoryPreferences.getActiveAlert(context);
        String suppressedAlertLevel = AlertHistoryPreferences.getSuppressedAlertLevel(context);

        if (isWithinConnectionGracePeriod(context)) {
            return;
        }

        if (alertLevel == null) {
            if (activeAlert != null) {
                AlertHistoryPreferences.archiveActiveAlert(
                        context,
                        "Resolved",
                        "Alert condition cleared and monitoring returned to a safe state."
                );
                FatigueAlertNotifier.cancel(context);
            }
            AlertHistoryPreferences.clearSuppressedAlertLevel(context);
            return;
        }

        if (alertLevel.equals(suppressedAlertLevel)) {
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

        String alertTitle = "Driver Fatigue Detected";
        String alertMessage = "Fatigue indicators are elevated. Immediate rest is recommended for driver safety.";

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

        Intent alertIntent = new Intent(context, FatigueAlertActivity.class)
                .putExtra("heartRate", heartRate)
                .putExtra("fatigueLevel", alertLevel)
                .putExtra("driveTime", driveTime)
                .putExtra("driveTimeMinutes", driveMinutes)
                .putExtra("fatigueScore", fatigueScore)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(alertIntent);
    }

    private static String resolveAlertLevel(int fatigueScore) {
        String scoreState = DetailedAnalysisActivity.getScoreState(fatigueScore);
        if ("FATIGUED".equals(scoreState)) {
            return "FATIGUED";
        }
        return null;
    }

    private static boolean isWithinConnectionGracePeriod(Context context) {
        long connectedSinceMs = BleSensorPreferences.getConnectedSinceMs(context);
        if (connectedSinceMs <= 0L) {
            return false;
        }
        return (System.currentTimeMillis() - connectedSinceMs) < CONNECTION_GRACE_PERIOD_MS;
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
