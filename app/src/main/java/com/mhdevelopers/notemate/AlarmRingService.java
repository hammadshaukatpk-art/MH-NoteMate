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
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.PowerManager;
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
    private static final String CHANNEL="mh_notemate_ringing_silent_v4";
    private static final int NOTIFICATION_ID=54001;
    private Ringtone ringtone;
    private MediaPlayer player;
    private ToneGenerator tone;
    private PowerManager.WakeLock wakeLock;
    private long playingNoteId=-1L;
    private long playingDueAt=-1L;
    private boolean audioActive=false;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable autoStop=this::stopSelf;

    @Override public IBinder onBind(Intent intent){return null;}

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        try{return startAlarm(intent,flags,startId);}
        catch(RuntimeException error){
            android.util.Log.e("MHNoteMate","Alarm service startup error",error);
            try{getSharedPreferences("mh_diagnostics",MODE_PRIVATE).edit()
                .putString("last_alarm_error","Alarm service crash prevented: "+error).apply();}
            catch(Exception ignored){}
            try{AlarmAudioFallback.postAudibleFallback(this,"Your reminder is due");}
            catch(Exception ignored){}
            try{stopSelf();}catch(Exception ignored){}
            return START_NOT_STICKY;
        }
    }
    private int startAlarm(Intent intent,int flags,int startId){
        if(intent!=null && ACTION_STOP.equals(intent.getAction())){
            stopSelf();return START_NOT_STICKY;
        }
        long requestedId=intent==null?-1L:intent.getLongExtra("note_id",-1L);
        long requestedDue=intent==null?-1L:intent.getLongExtra("due_at",-1L);
        if(audioActive && requestedId>=0 && requestedId==playingNoteId && requestedDue==playingDueAt){
            android.util.Log.i("MHNoteMate","Duplicate alarm start ignored");
            return START_NOT_STICKY;
        }
        NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel=new NotificationChannel(CHANNEL,"Active MH NoteMate alarms",NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Shows a stop button while a scheduled alarm is ringing");
        channel.setSound(null,null);
        channel.enableVibration(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
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
            .setPriority(Notification.PRIORITY_LOW)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true).build();
        try{
            if(Build.VERSION.SDK_INT>=29)
                startForeground(NOTIFICATION_ID,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            else startForeground(NOTIFICATION_ID,notification);
        }catch(Exception foregroundError){
            android.util.Log.e("MHNoteMate","Foreground alarm service could not start",foregroundError);
            // Android requires a foreground service to promote itself promptly.
            // Continuing after startForeground fails can cause a fatal system crash.
            getSharedPreferences("mh_diagnostics",MODE_PRIVATE).edit()
                .putString("last_alarm_error","Foreground service: "+foregroundError.getClass().getSimpleName()
                    +": "+String.valueOf(foregroundError.getMessage())).apply();
            AlarmAudioFallback.postAudibleFallback(this,title);
            // Never leave an ongoing 'ringing' notification after a failed service.
            try{manager.cancel(NOTIFICATION_ID);}catch(Exception ignored){}
            stopSelf();
            return START_NOT_STICKY;
        }
        try{
            PowerManager power=(PowerManager)getSystemService(POWER_SERVICE);
            if(power!=null){
                wakeLock=power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MHNoteMate:AlarmAudio");
                wakeLock.acquire(65_000L);
            }
        }catch(Exception error){android.util.Log.w("MHNoteMate","Wake lock unavailable",error);}
        try{
            stopAudio();
            playingNoteId=requestedId;
            playingDueAt=requestedDue;
            String selected=intent==null?"":intent.getStringExtra("tone_uri");
            Uri sound=null;
            if(selected!=null && !selected.isEmpty()) {
                try{sound=Uri.parse(selected);}catch(Exception error){
                    android.util.Log.w("MHNoteMate","Selected ringtone unavailable",error);
                }
            }
            if(sound==null)sound=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if(sound==null)sound=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            // Prefer MediaPlayer for consistent looping on OEM Android builds.
            try{
                player=new MediaPlayer();
                player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
                player.setDataSource(this,sound);
                player.setLooping(true);
                player.prepare();
                player.start();
                audioActive=true;
                android.util.Log.i("MHNoteMate","Alarm audio started");
            }catch(Exception mediaError){
                android.util.Log.w("MHNoteMate","MediaPlayer failed; using ringtone fallback",mediaError);
                if(player!=null){player.release();player=null;}
                ringtone=RingtoneManager.getRingtone(this,sound);
                if(ringtone==null)ringtone=RingtoneManager.getRingtone(this,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                if(ringtone!=null){
                    ringtone.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
                    if(Build.VERSION.SDK_INT>=28)ringtone.setLooping(true);
                    ringtone.play();
                    audioActive=ringtone.isPlaying();
                }else android.util.Log.e("MHNoteMate","No playable alarm ringtone on this device");
            }
            // Final audible fallback if the device has no usable ringtone URI.
            if((player==null || !player.isPlaying()) && (ringtone==null || !ringtone.isPlaying())){
                tone=new ToneGenerator(AudioManager.STREAM_ALARM,100);
                audioActive=tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,3000);
                android.util.Log.w("MHNoteMate","Playing synthesized alarm fallback");
            }
            AudioManager audio=(AudioManager)getSystemService(AUDIO_SERVICE);
            if(audio!=null && audio.getStreamVolume(AudioManager.STREAM_ALARM)==0)
                android.util.Log.e("MHNoteMate","Alarm volume is ZERO; Android cannot play audible alarms");
        }catch(Exception error){android.util.Log.e("MHNoteMate","Unable to play selected alarm ringtone",error);}
        handler.removeCallbacks(autoStop);
        handler.postDelayed(autoStop,60_000L);
        return START_NOT_STICKY;
    }
    private void stopAudio(){
        audioActive=false;
        playingNoteId=-1L;
        playingDueAt=-1L;
        try{if(player!=null){if(player.isPlaying())player.stop();player.release();}}catch(Exception ignored){}
        player=null;
        try{if(ringtone!=null && ringtone.isPlaying())ringtone.stop();}catch(Exception ignored){}
        ringtone=null;
        if(tone!=null){try{tone.stopTone();tone.release();}catch(Exception ignored){}tone=null;}
    }
    @Override public void onDestroy(){
        handler.removeCallbacks(autoStop);
        stopAudio();
        if(wakeLock!=null){try{if(wakeLock.isHeld())wakeLock.release();}catch(Exception ignored){}wakeLock=null;}
        try{((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);}
        catch(Exception ignored){}
        super.onDestroy();
    }
}