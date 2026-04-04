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
    private final float gsrFiltered;
    private final float gsrBaseline;
    private final String driverState;
    private final boolean possibleCrash;
    private final String rawPayload;

    private BleSensorData(boolean fingerDetected, float bpm, int avgBpm,
                          boolean suddenMovement, float motionAccel, float gyroMag,
                          float ax, float ay, float az,
                          float gsrFiltered, float gsrBaseline,
                          String driverState, boolean possibleCrash,
                          String rawPayload) {
        this.fingerDetected = fingerDetected;
        this.bpm            = bpm;
        this.avgBpm         = avgBpm;
        this.suddenMovement = suddenMovement;
        this.motionAccel    = motionAccel;
        this.gyroMag        = gyroMag;
        this.ax             = ax;
        this.ay             = ay;
        this.az             = az;
        this.gsrFiltered    = gsrFiltered;
        this.gsrBaseline    = gsrBaseline;
        this.driverState    = driverState != null ? driverState : "UNKNOWN";
        this.possibleCrash  = possibleCrash;
        this.rawPayload     = rawPayload;
    }

    public static BleSensorData fromPayload(String payload) {
        Map<String, String> v = new HashMap<>();
        for (String pair : payload.split(",")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) v.put(parts[0].trim(), parts[1].trim());
        }

        return new BleSensorData(
                "1".equals(v.get("f")),
                parseFloat(v.get("b")),
                parseInt(v.get("a")),
                "1".equals(v.get("s")),
                parseFloat(v.get("ma")),
                parseFloat(v.get("gm")),
                parseFloat(v.get("ax")),
                parseFloat(v.get("ay")),
                parseFloat(v.get("az")),
                parseFloat(v.get("gsr")),
                parseFloat(v.get("gsrb")),
                v.get("ds"),
                "1".equals(v.get("cr")),
                payload
        );
    }

    /**
     * Returns a copy of this object with driverState replaced.
     * Used so the app-side threshold classification overrides
     * whatever driverState the Arduino sent via BLE.
     */
    public BleSensorData withDriverState(String newState) {
        return new BleSensorData(
                fingerDetected, bpm, avgBpm, suddenMovement, motionAccel, gyroMag,
                ax, ay, az, gsrFiltered, gsrBaseline,
                newState, possibleCrash, rawPayload
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

    public boolean isFingerDetected() { return fingerDetected; }
    public float getBpm()             { return bpm; }
    public int getAvgBpm()            { return avgBpm; }
    public boolean isSuddenMovement() { return suddenMovement; }
    public float getMotionAccel()     { return motionAccel; }
    public float getGyroMag()         { return gyroMag; }
    public float getAx()              { return ax; }
    public float getAy()              { return ay; }
    public float getAz()              { return az; }
    public String getRawPayload()     { return rawPayload; }
    public float getGsrFiltered()     { return gsrFiltered; }
    public float getGsrBaseline()     { return gsrBaseline; }
    public String getDriverState()    { return driverState; }
    public boolean isPossibleCrash()  { return possibleCrash; }
}