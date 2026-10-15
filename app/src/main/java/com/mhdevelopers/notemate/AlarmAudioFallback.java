package com.mhdevelopers.notemate;

import android.content.Context;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

/** Last-resort short alarm playback if Android refuses to start the ringing service.
 * This is a fallback, not a replacement for the foreground service. */
public final class AlarmAudioFallback {
    private static Ringtone active;
    private static final Handler handler=new Handler(Looper.getMainLooper());
    private AlarmAudioFallback(){}
    /** One audible system notification, only when the normal foreground alarm cannot start.
     * The primary due-time notification is silent so there is no double ringtone. */
    public static void postAudibleFallback(Context context,String title){
        try{
            if(Build.VERSION.SDK_INT>=33 &&
                context.checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=PackageManager.PERMISSION_GRANTED)
                return;
            NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            if(nm==null || !nm.areNotificationsEnabled())return;
            String channelId="mh_notemate_alarm_emergency_v1";
            NotificationChannel channel=new NotificationChannel(channelId,"Alarm sound fallback",
                NotificationManager.IMPORTANCE_HIGH);
            channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(channel);
            Intent open=new Intent(context,MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent tap=PendingIntent.getActivity(context,54003,open,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Notification notice=new Notification.Builder(context,channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("MH NoteMate alarm")
                .setContentText(title==null?"Your reminder is due":title)
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(tap).setAutoCancel(true).build();
            nm.notify(54003,notice);
        }catch(Exception error){
            android.util.Log.e("MHNoteMate","Audible fallback notification failed",error);
        }
    }

    public static void play(Context context,String selected){
        try{
            if(active!=null)active.stop();
            Uri uri=selected==null||selected.isEmpty()
                ?RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM):Uri.parse(selected);
            active=RingtoneManager.getRingtone(context.getApplicationContext(),uri);
            if(active==null)active=RingtoneManager.getRingtone(context.getApplicationContext(),
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
            if(active!=null){
                active.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
                active.play();
                final Ringtone playing=active;
                handler.postDelayed(()->{try{playing.stop();}catch(Exception ignored){}},15_000L);
            }
        }catch(Exception error){
            android.util.Log.e("MHNoteMate","Fallback alarm audio also failed",error);
            context.getSharedPreferences("mh_diagnostics",Context.MODE_PRIVATE).edit()
                .putString("last_alarm_error","Fallback alarm audio: "+error).apply();
        }
    }
}
