package com.coen390.team6;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public final class FatigueAlertNotifier {

    public static final String CHANNEL_ID = "fatigue_alerts";
    public static final int NOTIFICATION_ID = 3901;

    private FatigueAlertNotifier() {
    }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Fatigue Alerts",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Critical fatigue alerts and lock-screen alarms");
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0, 500, 200, 500, 200, 1000});
        channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        channel.setSound(soundUri, attributes);
        manager.createNotificationChannel(channel);
    }

    public static void showFatigueAlert(
            Context context,
            int heartRate,
            String fatigueLevel,
            String driveTime,
            long driveTimeMinutes
    ) {
        ensureChannel(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent fullScreenIntent = new Intent(context, FatigueAlertActivity.class)
                .putExtra("heartRate", heartRate)
                .putExtra("fatigueLevel", fatigueLevel)
                .putExtra("driveTime", driveTime)
                .putExtra("driveTimeMinutes", driveTimeMinutes)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                0,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(getAlertTitle(fatigueLevel))
                .setContentText(getAlertMessage(fatigueLevel))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(getAlertMessage(fatigueLevel)))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .setOngoing(true)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                .setVibrate(new long[]{0, 500, 200, 500, 200, 1000});

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
    }

    public static void cancel(Context context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);
    }

    private static String getAlertTitle(String fatigueLevel) {
        if ("FATIGUED".equals(fatigueLevel)) {
            return "Fatigue detected";
        }
        return "Fatigue detected";
    }

    private static String getAlertMessage(String fatigueLevel) {
        if ("FATIGUED".equals(fatigueLevel)) {
            return "Critical fatigue detected. Review the driver alert immediately.";
        }
        return "Critical fatigue detected. Open the alert now.";
    }
}
