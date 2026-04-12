package com.coen390.team6;

import android.content.Context;
import android.content.Intent;
import android.app.KeyguardManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class FatigueAlertActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        );
        setContentView(R.layout.activity_fatigue_alert);

        KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);
        if (keyguardManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager.requestDismissKeyguard(this, null);
        }

        int heartRate = getIntent().getIntExtra("heartRate", 0);
        String fatigueLevel = getIntent().getStringExtra("fatigueLevel");

        TextView tvAlertHR = findViewById(R.id.tvAlertHR);
        TextView tvAlertFatigue = findViewById(R.id.tvAlertFatigue);

        if (heartRate > 0) {
            tvAlertHR.setText(heartRate + " BPM");
        }
        if (fatigueLevel != null) {
            tvAlertFatigue.setText(formatFatigueLevel(fatigueLevel));
        }

        startLoudAlarm();
        startAggressiveVibration();

        Button btnFindRestStop = findViewById(R.id.btnFindRestStop);
        Button btnDismiss = findViewById(R.id.btnDismissAlert);

        btnFindRestStop.setOnClickListener(v -> {
            stopAlarm();
            FatigueAlertNotifier.cancel(this);
            AlertHistoryPreferences.setSuppressedAlertLevel(this, fatigueLevel);
            AlertHistoryPreferences.archiveActiveAlert(
                    this,
                    "Resolved",
                    "Driver selected nearest rest stop guidance after high fatigue warning."
            );
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=rest+stop+near+me"));
            intent.setPackage("com.google.android.apps.maps");
            startActivity(intent);
            finish();
        });

        btnDismiss.setOnClickListener(v -> {
            stopAlarm();
            FatigueAlertNotifier.cancel(this);
            AlertHistoryPreferences.setSuppressedAlertLevel(this, fatigueLevel);
            AlertHistoryPreferences.archiveActiveAlert(
                    this,
                    "Dismissed",
                    "Driver dismissed the fatigue warning as a false alarm."
            );
            finish();
        });
    }

    private void startLoudAlarm() {
        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0);

            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            mediaPlayer.setDataSource(this, alarmUri);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception ignored) {
        }
    }

    private void startAggressiveVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vm.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }

        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 500, 200, 500, 200, 1000, 300, 500, 200, 500};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }
    }

    private void stopAlarm() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    private static String formatFatigueLevel(String fatigueLevel) {
        if ("FATIGUED".equals(fatigueLevel)) {
            return "Fatigued";
        }
        return fatigueLevel;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlarm();
    }
}
