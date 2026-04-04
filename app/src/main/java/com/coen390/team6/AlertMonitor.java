package com.coen390.team6;

import android.content.Context;

public final class AlertMonitor {
    private AlertMonitor() {}

    public static void processSensorState(Context context, BleSensorData sensorData) {
        boolean highFatigueDetected = sensorData.isFingerDetected()
                && "DROWSY".equalsIgnoreCase(sensorData.getDriverState());

        if (highFatigueDetected) {
            if (AlertRepository.hasActiveAlert(context, AlertItem.TYPE_FATIGUE)) {
                return;
            }

            DriverFatigueStatus fatigueStatus = DriverFatigueStatus.from(context);
            AlertItem alertItem = new AlertItem(
                    String.valueOf(System.currentTimeMillis()),
                    AlertItem.TYPE_FATIGUE,
                    "High Fatigue Risk",
                    "Fatigue score dropped into the danger zone (" + fatigueStatus.getScoreText()
                            + "). Pull over and rest as soon as it is safe.",
                    "",
                    "critical",
                    System.currentTimeMillis(),
                    true
            );
            AlertRepository.addAlert(context, alertItem);
            AlertNotifier.notifyAlert(context, alertItem);
            return;
        }

        if (sensorData.isFingerDetected()) {
            AlertRepository.resolveAlertsByType(
                    context,
                    AlertItem.TYPE_FATIGUE,
                    "Driver biometrics returned above the fatigue alert threshold."
            );
        }
    }
}
