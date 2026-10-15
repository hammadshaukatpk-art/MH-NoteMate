package com.mhdevelopers.notemate;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent) {
        if(Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())){
            // Repost any imminent advance alert without the previous build's
            // auto-expiration behavior.
            context.getSharedPreferences("mh_reminder_alerts",Context.MODE_PRIVATE)
                .edit().clear().apply();
        }
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()) || Intent.ACTION_TIME_CHANGED.equals(intent.getAction()) || Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction()))
            AlarmHelper.rescheduleAll(context);
    }
}
