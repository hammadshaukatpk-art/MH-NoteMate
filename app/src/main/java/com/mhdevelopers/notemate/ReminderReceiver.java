package com.mhdevelopers.notemate;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.app.Notification;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String PREVIEW_CHANNEL="notemate_upcoming_v3";
    private static final String ALARM_CHANNEL="notemate_alarm_silent_v5";
    @Override public void onReceive(Context context,Intent intent) {
        try{
        handleReminder(context,intent);
        }catch(Exception error){
            android.util.Log.e("MHNoteMate","Reminder broadcast failed",error);
            context.getSharedPreferences("mh_diagnostics",Context.MODE_PRIVATE).edit()
                .putString("last_alarm_error","Reminder broadcast: "+error.getClass().getSimpleName()
                    +": "+String.valueOf(error.getMessage())).apply();
        }
    }
    private void handleReminder(Context context,Intent intent) {
        long id=intent.getLongExtra("note_id",-1);
        int stage=intent.getIntExtra("stage",1);
        NotesDb db=new NotesDb(context);NotesDb.Note n=db.get(id);
        if(n==null || n.done==1 || n.dueAt<=0) {db.close();return;}
        if(stage==0 && n.dueAt<=System.currentTimeMillis()){db.close();return;}
        if(stage==0 && context.getSharedPreferences("mh_reminder_alerts",Context.MODE_PRIVATE)
                .getLong("advance_notified_"+id,0L)==n.dueAt){db.close();return;}
        NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        // The advance notification must disappear once the actual alarm is due.
        // Do not remove the advance notification before the due alarm is handled.
        // Otherwise it vanishes at the deadline even if Android blocks ringing.
        String channelId=stage==0?PREVIEW_CHANNEL:ALARM_CHANNEL;
        NotificationChannel channel=new NotificationChannel(channelId,
            stage==0?"Upcoming reminders":"Due alarms",NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(stage==0?"Advance alerts for upcoming notes and tasks":"Silent due-time status; the ringing service plays the selected alarm tone");
        channel.enableVibration(stage==0);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        if(stage==1){
            // Critical: the alarm service is the ONLY source of alarm audio.
            // The old due channel played a second default alarm tone over the selected tone.
            channel.setSound(null,null);
        }else channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
            new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
        manager.createNotificationChannel(channel);
        Intent open=new Intent(context,MainActivity.class);open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent tap=PendingIntent.getActivity(context,(int)id,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        String heading=stage==0?"Upcoming: "+n.title:"Reminder: "+n.title;
        long remaining=Math.max(1,(n.dueAt-System.currentTimeMillis()+59_999L)/60_000L);
        String lead=remaining>=15?"In 15 minutes":"Due in "+remaining+" minute"+(remaining==1?"":"s");
        String detail=stage==0?lead+(n.body.isEmpty()?"":" • "+n.body):n.body.isEmpty()?"It's time for your scheduled note or task.":n.body;
        Notification notification=new Notification.Builder(context,channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(heading).setContentText(detail)
            .setStyle(new Notification.BigTextStyle().bigText(detail)).setContentIntent(tap)
            .setAutoCancel(true).setCategory(stage==1?Notification.CATEGORY_ALARM:Notification.CATEGORY_REMINDER)
            .setPriority(stage==0?Notification.PRIORITY_MAX:Notification.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build();
        boolean dueNotificationPosted=false;
        if(Build.VERSION.SDK_INT<33 || context.checkSelfPermission("android.permission.POST_NOTIFICATIONS")==PackageManager.PERMISSION_GRANTED) {
            NotificationChannel active=manager.getNotificationChannel(channelId);
            if(manager.areNotificationsEnabled() && active!=null &&
                active.getImportance()!=NotificationManager.IMPORTANCE_NONE){
                try{
                    manager.notify((int)(id*2+stage),notification);
                    if(stage==1)dueNotificationPosted=true;
                    if(stage==0)context.getSharedPreferences("mh_reminder_alerts",Context.MODE_PRIVATE).edit()
                        .putLong("advance_notified_"+id,n.dueAt).apply();
                }catch(Exception error){android.util.Log.e("MHNoteMate","Reminder notification failed",error);}
            }else android.util.Log.e("MHNoteMate","Reminder notifications disabled in Android settings");
        }
        if(stage==1){
            if(dueNotificationPosted)manager.cancel((int)(id*2));
            // Exact alarm time: ring audibly until stopped or after 60 seconds.
            // Android can restrict background service starts on some devices;
            // the high-priority alarm notification above remains the fallback.
            try{
                Intent ring=new Intent(context,AlarmRingService.class);
                ring.setAction(AlarmRingService.ACTION_START);
                ring.putExtra("title",n.title);
                ring.putExtra("tone_uri",n.toneUri);
                ring.putExtra("note_id",n.id);
                ring.putExtra("due_at",n.dueAt);
                if(Build.VERSION.SDK_INT>=26)context.startForegroundService(ring);
                else context.startService(ring);
            }catch(Exception error){
                android.util.Log.e("MHNoteMate","Could not start reminder alarm service",error);
                context.getSharedPreferences("mh_diagnostics",Context.MODE_PRIVATE).edit()
                    .putString("last_alarm_error","Due alarm service start: "+error).apply();
                // If Android refuses the foreground-service start, attempt short
                // receiver-owned alarm playback rather than silently dropping it.
                AlarmAudioFallback.postAudibleFallback(context,n.title);
            }
        }
        if(stage==1 && !"Never".equals(n.repeat)) {
            Calendar next=Calendar.getInstance();next.setTimeInMillis(n.dueAt);
            int field="Daily".equals(n.repeat)?Calendar.DAY_OF_YEAR:Calendar.WEEK_OF_YEAR;
            do {next.add(field,1);} while(next.getTimeInMillis()<=System.currentTimeMillis());
            n.dueAt=next.getTimeInMillis();db.save(n);AlarmHelper.schedule(context,n);
        }
        db.close();
    }
}