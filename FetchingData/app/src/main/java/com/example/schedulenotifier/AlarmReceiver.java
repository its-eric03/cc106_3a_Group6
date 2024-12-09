package com.example.schedulenotifier;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.shedulenotifer.R;

public class AlarmReceiver extends BroadcastReceiver {

    private static MediaPlayer mediaPlayer; // Static for sound control across intents

    @Override
    public void onReceive(Context context, Intent intent) {
        // Retrieve alarm details
        String subject = intent.getStringExtra("subject");
        String time = intent.getStringExtra("time");
        String roomNo = intent.getStringExtra("room_no");

        if (subject == null) subject = "Unknown Subject";
        if (time == null) time = "Unknown Time";
        if (roomNo == null) roomNo = "Unknown Room";

        Log.d("AlarmReceiver", "Alarm triggered: Subject=" + subject + ", Time=" + time + ", Room=" + roomNo);

        // Play alarm sound
        playAlarmSound(context);

        // Show notification with a deleteIntent
        showNotification(context, subject, time, roomNo);
    }

    private void playAlarmSound(Context context) {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(context, R.raw.sound_alarm); // Replace with your sound resource
                if (mediaPlayer != null) {
                    mediaPlayer.setLooping(true); // Loop the sound until stopped
                    mediaPlayer.start();
                    Log.d("AlarmReceiver", "Alarm sound started.");
                } else {
                    Log.e("AlarmReceiver", "MediaPlayer initialization failed.");
                }
            }
        } catch (Exception e) {
            Log.e("AlarmReceiver", "Error playing alarm sound: " + e.getMessage(), e);
        }
    }

    public static void stopAlarmSound() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            Log.d("AlarmReceiver", "Alarm sound stopped.");
        }
    }

    private void showNotification(Context context, String subject, String time, String roomNo) {
        String channelId = "alarm_channel";
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) {
            Log.e("AlarmReceiver", "NotificationManager is null.");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Class Alarm",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Alarm for scheduled class");
            notificationManager.createNotificationChannel(channel);
        }

        // Create a unique notificationId
        int notificationId = (subject + time + roomNo).hashCode();

        // Intent to stop the alarm sound when the notification is dismissed
        Intent deleteIntent = new Intent(context, NotificationDismissReceiver.class);
        deleteIntent.putExtra("notificationId", notificationId);

        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId, // Unique notificationId
                deleteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(context, channelId)
                .setContentTitle("Class Reminder")
                .setContentText("Class: " + subject + "\nTime: " + time + "\nRoom: " + roomNo)
                .setSmallIcon(R.drawable.ic_notif) // Replace with your notification icon
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)

                .setAutoCancel(true)
                .setDeleteIntent(deletePendingIntent) // Attach delete intent
                .build();

        notificationManager.notify(notificationId, notification);
    }

}
