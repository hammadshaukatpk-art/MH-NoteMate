package com.mhdevelopers.notemate;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

/** Plays the selected alarm sound when a precise reminder becomes due.
 * A foreground notification gives the user a Stop action, and playback ends
 * automatically after 60 seconds so it cannot ring indefinitely. */
public class AlarmRingService extends Service {
    public static final String ACTION_START="com.mhdevelopers.notemate.ALARM_START";
    public static final String ACTION_STOP="com.mhdevelopers.notemate.ALARM_STOP";
    private static final String CHANNEL="mh_notemate_ringing_v1";
    private static final int NOTIFICATION_ID=54001;
    private Ringtone ringtone;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable autoStop=this::stopSelf;

    @Override public IBinder onBind(Intent intent){return null;}

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null && ACTION_STOP.equals(intent.getAction())){
            stopSelf();return START_NOT_STICKY;
        }
        NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel=new NotificationChannel(CHANNEL,"Active MH NoteMate alarms",NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Shows a stop button while a scheduled alarm is ringing");
        channel.setSound(null,null);
        manager.createNotificationChannel(channel);
        String title=intent==null?"Scheduled reminder":intent.getStringExtra("title");
        if(title==null || title.trim().isEmpty())title="Scheduled reminder";
        Intent stopIntent=new Intent(this,AlarmRingService.class).setAction(ACTION_STOP);
        PendingIntent stop=PendingIntent.getService(this,NOTIFICATION_ID,stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent open=PendingIntent.getActivity(this,NOTIFICATION_ID,
            new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification notification=new Notification.Builder(this,CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("MH NoteMate Alarm")
            .setContentText(title+" • Tap Stop Alarm to silence")
            .setContentIntent(open)
            .addAction(android.R.drawable.ic_media_pause,"Stop Alarm",stop)
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true).build();
        if(Build.VERSION.SDK_INT>=29){
            startForeground(NOTIFICATION_ID,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        }else startForeground(NOTIFICATION_ID,notification);
        try{
            if(ringtone!=null && ringtone.isPlaying())ringtone.stop();
            Uri sound=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if(sound==null)sound=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            ringtone=RingtoneManager.getRingtone(this,sound);
            if(ringtone!=null){
                ringtone.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
                if(Build.VERSION.SDK_INT>=28)ringtone.setLooping(true);
                ringtone.play();
            }
        }catch(Exception ignored){/* The alarm notification remains as a fallback. */}
        handler.removeCallbacks(autoStop);
        handler.postDelayed(autoStop,60_000L);
        return START_NOT_STICKY;
    }
    @Override public void onDestroy(){
        handler.removeCallbacks(autoStop);
        try{if(ringtone!=null && ringtone.isPlaying())ringtone.stop();}catch(Exception ignored){}
        ringtone=null;
        super.onDestroy();
    }
}