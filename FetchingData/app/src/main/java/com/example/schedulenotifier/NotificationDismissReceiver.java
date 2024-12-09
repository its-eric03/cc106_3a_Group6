package com.example.schedulenotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationDismissReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("NotificationDismiss", "Notification dismissed. Stopping alarm sound.");

        // Stop the alarm sound
        AlarmReceiver alarmReceiver = new AlarmReceiver();
        alarmReceiver.stopAlarmSound();
    }
}
