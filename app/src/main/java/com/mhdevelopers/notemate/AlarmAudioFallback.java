package com.mhdevelopers.notemate;

import android.content.Context;
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
