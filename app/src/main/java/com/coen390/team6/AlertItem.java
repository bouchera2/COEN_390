package com.coen390.team6;

import org.json.JSONException;
import org.json.JSONObject;

public final class AlertItem {
    public static final String TYPE_FATIGUE = "fatigue_high";
    public static final String STATUS_ACTIVE = "Active";
    public static final String STATUS_RESOLVED = "Resolved";

    private final String id;
    private final String type;
    private final String title;
    private final String message;
    private final String resolutionNote;
    private final String severity;
    private final long timestamp;
    private final boolean active;

    public AlertItem(
            String id,
            String type,
            String title,
            String message,
            String resolutionNote,
            String severity,
            long timestamp,
            boolean active
    ) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.resolutionNote = resolutionNote;
        this.severity = severity;
        this.timestamp = timestamp;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public String getSeverity() {
        return severity;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isActive() {
        return active;
    }

    public String getStatusLabel() {
        return active ? STATUS_ACTIVE : STATUS_RESOLVED;
    }

    public AlertItem withResolution(String note) {
        return new AlertItem(
                id,
                type,
                title,
                message,
                note,
                severity,
                timestamp,
                false
        );
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("type", type);
        json.put("title", title);
        json.put("message", message);
        json.put("resolutionNote", resolutionNote);
        json.put("severity", severity);
        json.put("timestamp", timestamp);
        json.put("active", active);
        return json;
    }

    public static AlertItem fromJson(JSONObject json) throws JSONException {
        return new AlertItem(
                json.optString("id"),
                json.optString("type"),
                json.optString("title"),
                json.optString("message"),
                json.optString("resolutionNote"),
                json.optString("severity", "warning"),
                json.optLong("timestamp"),
                json.optBoolean("active")
        );
    }
}
