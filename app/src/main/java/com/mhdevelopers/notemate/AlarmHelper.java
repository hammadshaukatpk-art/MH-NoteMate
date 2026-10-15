package com.mhdevelopers.notemate;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;

public final class AlarmHelper {
    private AlarmHelper() {}
    private static PendingIntent pending(Context c,long id,int stage) {
        Intent i=new Intent(c,ReminderReceiver.class);
        i.setAction("com.mhdevelopers.notemate.REMIND_"+stage);
        i.putExtra("note_id",id);i.putExtra("stage",stage);
        return PendingIntent.getBroadcast(c,(int)(id*2+stage),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    public static void cancel(Context c,long id) {
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pending(c,id,0));am.cancel(pending(c,id,1));
    }
    public static void schedule(Context c,NotesDb.Note n) {
        cancel(c,n.id);
        if(n.dueAt<=0 || n.done==1) return;
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        // If saved inside the 15-minute window, notify immediately, exactly once per due time.
        long now=System.currentTimeMillis();
        long advanceAt=n.dueAt-15*60_000L;
        if(n.dueAt>now && advanceAt<=now) {
            String key="advance_notified_"+n.id;
            if(c.getSharedPreferences("mh_reminder_alerts",Context.MODE_PRIVATE).getLong(key,0L)!=n.dueAt) {
                Intent immediate=new Intent(c,ReminderReceiver.class);
                immediate.putExtra("note_id",n.id);
                immediate.putExtra("stage",0);
                new ReminderReceiver().onReceive(c,immediate);
            }
        } else {
            set(c,am,advanceAt,pending(c,n.id,0),false);
        }
        set(c,am,n.dueAt,pending(c,n.id,1),true);
    }
    private static void set(Context context,AlarmManager am,long at,PendingIntent pi,boolean due) {
        if(at<=System.currentTimeMillis()) return;
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            android.util.Log.w("MHNoteMate","Exact alarms disabled: scheduled reminder may be delayed");
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
            return;
        }
        try {
            if(due){
                Intent open=new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent show=PendingIntent.getActivity(context,0,open,
                    PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
                // User-visible alarm clock survives Doze and gives reliable due-time delivery.
                am.setAlarmClock(new AlarmManager.AlarmClockInfo(at,show),pi);
            }else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
        }catch(SecurityException e){
            android.util.Log.e("MHNoteMate","Exact alarm access denied",e);
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
        }
    }
    public static void rescheduleAll(Context c) {
        NotesDb db=new NotesDb(c);
        for(NotesDb.Note n:db.all()) {
            if(n.dueAt > 0 && n.dueAt <= System.currentTimeMillis() && !"Never".equals(n.repeat) && n.done == 0) {
                Calendar next=Calendar.getInstance();next.setTimeInMillis(n.dueAt);
                int field="Daily".equals(n.repeat)?Calendar.DAY_OF_YEAR:Calendar.WEEK_OF_YEAR;
                do { next.add(field,1); } while(next.getTimeInMillis() <= System.currentTimeMillis());
                n.dueAt=next.getTimeInMillis();db.save(n);
            }
            schedule(c,n);
        }
        db.close();
    }
}