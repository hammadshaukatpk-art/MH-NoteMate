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
    private static final String PREVIEW_CHANNEL="notemate_upcoming_v1";
    private static final String ALARM_CHANNEL="notemate_alarm_v2";
    @Override public void onReceive(Context context,Intent intent) {
        long id=intent.getLongExtra("note_id",-1);
        int stage=intent.getIntExtra("stage",1);
        NotesDb db=new NotesDb(context);NotesDb.Note n=db.get(id);
        if(n==null || n.done==1 || n.dueAt<=0) {db.close();return;}
        NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId=stage==0?PREVIEW_CHANNEL:ALARM_CHANNEL;
        NotificationChannel channel=new NotificationChannel(channelId,
            stage==0?"Upcoming reminders":"Due alarms",NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(stage==0?"Advance alerts for upcoming notes and tasks":"Alarm sound when a note or task is due");
        channel.enableVibration(true);
        if(stage==1)channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
        manager.createNotificationChannel(channel);
        Intent open=new Intent(context,MainActivity.class);open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent tap=PendingIntent.getActivity(context,(int)id,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        String heading=stage==0?"Upcoming: "+n.title:"Reminder: "+n.title;
        String lead="In 15 minutes";
        String detail=stage==0?lead+(n.body.isEmpty()?"":" • "+n.body):n.body.isEmpty()?"It's time for your scheduled note or task.":n.body;
        Notification notification=new Notification.Builder(context,channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(heading).setContentText(detail)
            .setStyle(new Notification.BigTextStyle().bigText(detail)).setContentIntent(tap)
            .setAutoCancel(true).setCategory(stage==1?Notification.CATEGORY_ALARM:Notification.CATEGORY_REMINDER)
            .setPriority(Notification.PRIORITY_MAX).setVisibility(Notification.VISIBILITY_PUBLIC).build();
        if(Build.VERSION.SDK_INT<33 || context.checkSelfPermission("android.permission.POST_NOTIFICATIONS")==PackageManager.PERMISSION_GRANTED)
            manager.notify((int)(id*2+stage),notification);
        if(stage==1 && !"Never".equals(n.repeat)) {
            Calendar next=Calendar.getInstance();next.setTimeInMillis(n.dueAt);
            int field="Daily".equals(n.repeat)?Calendar.DAY_OF_YEAR:Calendar.WEEK_OF_YEAR;
            do {next.add(field,1);} while(next.getTimeInMillis()<=System.currentTimeMillis());
            n.dueAt=next.getTimeInMillis();db.save(n);AlarmHelper.schedule(context,n);
        }
        db.close();
    }
}