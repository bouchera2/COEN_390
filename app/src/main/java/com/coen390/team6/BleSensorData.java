package com.coen390.team6;

import java.util.HashMap;
import java.util.Map;

public final class BleSensorData {
    private final boolean fingerDetected;
    private final float bpm;
    private final int avgBpm;
    private final boolean suddenMovement;
    private final float motionAccel;
    private final float gyroMag;
    private final float ax;
    private final float ay;
    private final float az;
    private final String rawPayload;

    private BleSensorData(boolean fingerDetected, float bpm, int avgBpm,
                          boolean suddenMovement, float motionAccel, float gyroMag,
                          float ax, float ay, float az, String rawPayload) {
        this.fingerDetected = fingerDetected;
        this.bpm = bpm;
        this.avgBpm = avgBpm;
        this.suddenMovement = suddenMovement;
        this.motionAccel = motionAccel;
        this.gyroMag = gyroMag;
        this.ax = ax;
        this.ay = ay;
        this.az = az;
        this.rawPayload = rawPayload;
    }

    public static BleSensorData fromPayload(String payload) {
        Map<String, String> values = new HashMap<>();
        for (String pair : payload.split(",")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                values.put(parts[0].trim(), parts[1].trim());
            }
        }

        return new BleSensorData(
                "1".equals(values.get("f")),
                parseFloat(values.get("b")),
                parseInt(values.get("a")),
                "1".equals(values.get("s")),
                parseFloat(values.get("ma")),
                parseFloat(values.get("gm")),
                parseFloat(values.get("ax")),
                parseFloat(values.get("ay")),
                parseFloat(values.get("az")),
                payload
        );
    }

    private static float parseFloat(String value) {
        if (value == null || value.isEmpty()) return 0f;
        try { return Float.parseFloat(value); }
        catch (NumberFormatException e) { return 0f; }
    }

    private static int parseInt(String value) {
        if (value == null || value.isEmpty()) return 0;
        try { return Integer.parseInt(value); }
        catch (NumberFormatException e) { return 0; }
    }

    public boolean isFingerDetected()  { return fingerDetected; }
    public float getBpm()              { return bpm; }
    public int getAvgBpm()             { return avgBpm; }
    public boolean isSuddenMovement()  { return suddenMovement; }
    public float getMotionAccel()      { return motionAccel; }
    public float getGyroMag()          { return gyroMag; }
    public float getAx()               { return ax; }
    public float getAy()               { return ay; }
    public float getAz()               { return az; }
    public String getRawPayload()      { return rawPayload; }
}