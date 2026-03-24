package com.coen390.team6;

import java.util.HashMap;
import java.util.Map;

public final class BleSensorData {
    private final boolean fingerDetected;
    private final float bpm;
    private final int avgBpm;
    private final boolean suddenMovement;
    private final String rawPayload;

    private BleSensorData(boolean fingerDetected, float bpm, int avgBpm, boolean suddenMovement, String rawPayload) {
        this.fingerDetected = fingerDetected;
        this.bpm = bpm;
        this.avgBpm = avgBpm;
        this.suddenMovement = suddenMovement;
        this.rawPayload = rawPayload;
    }

    public static BleSensorData fromPayload(String payload) {
        Map<String, String> values = new HashMap<>();
        String[] pairs = payload.split(",");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                values.put(parts[0].trim(), parts[1].trim());
            }
        }

        boolean fingerDetected = "1".equals(values.get("finger"));
        float bpm = parseFloat(values.get("bpm"));
        int avgBpm = parseInt(values.get("avgBpm"));
        boolean suddenMovement = "1".equals(values.get("sudden"));

        return new BleSensorData(fingerDetected, bpm, avgBpm, suddenMovement, payload);
    }

    private static float parseFloat(String value) {
        if (value == null || value.isEmpty()) {
            return 0f;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    private static int parseInt(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public boolean isFingerDetected() {
        return fingerDetected;
    }

    public float getBpm() {
        return bpm;
    }

    public int getAvgBpm() {
        return avgBpm;
    }

    public boolean isSuddenMovement() {
        return suddenMovement;
    }

    public String getRawPayload() {
        return rawPayload;
    }
}
