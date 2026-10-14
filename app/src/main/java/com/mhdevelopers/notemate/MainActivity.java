package com.mhdevelopers.notemate;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    static final int BLUE=Color.rgb(24,104,240), INK=Color.rgb(25,43,76), BG=Color.rgb(244,248,255);
    Db db; LinearLayout root,items; TextView count; boolean showTasks=false;
    int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+0.5f);}
    GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    LinearLayout.LayoutParams lp(int w,int h,int margin){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));p.setMargins(dp(margin),dp(margin),dp(margin),dp(margin));return p;}
    void add(LinearLayout box,View v,int height){box.addView(v,new LinearLayout.LayoutParams(-1,height<0?height:dp(height)));}
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.WHITE);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);db=new Db(this);requestNotificationPermission();render();}
    void requestNotificationPermission(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},5);}
    void render(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);root=col();root.setPadding(dp(20),dp(28),dp(20),dp(20));scroll.addView(root);setContentView(scroll);
        TextView brand=text("MH NoteMate",29,BLUE,true);add(root,brand,48);
        TextView sub=text("Your notes, tasks & reminders in one place",14,INK,false);add(root,sub,42);
        LinearLayout hero=col();hero.setPadding(dp(20),dp(22),dp(20),dp(22));hero.setBackground(shape(BLUE,22));
        TextView welcome=text("Stay organized ✨",23,Color.WHITE,true);add(hero,welcome,42);
        count=text("",14,Color.WHITE,false);add(hero,count,28);root.addView(hero,lp(-1,-2,0));
        LinearLayout tabs=new LinearLayout(this);tabs.setOrientation(0);
        Button all=button("All Notes",!showTasks);Button tasks=button("Tasks",showTasks);Button add=button("+ Add",false);
        tabs.addView(all,new LinearLayout.LayoutParams(0,dp(55),1));tabs.addView(tasks,new LinearLayout.LayoutParams(0,dp(55),1));tabs.addView(add,new LinearLayout.LayoutParams(0,dp(55),1));root.addView(tabs,lp(-1,-2,8));
        all.setOnClickListener(v->{showTasks=false;render();});tasks.setOnClickListener(v->{showTasks=true;render();});add.setOnClickListener(v->showEditor(false));
        TextView heading=text(showTasks?"My Tasks":"My Notes & Reminders",20,INK,true);root.addView(heading,lp(-1,44,5));
        items=col();root.addView(items,new LinearLayout.LayoutParams(-1,-2));refresh();
        TextView foot=text("MH Developers  •  Notes saved on your device",12,Color.GRAY,false);foot.setGravity(Gravity.CENTER);root.addView(foot,lp(-1,55,12));
    }
    Button button(String label,boolean active){Button b=new Button(this);b.setAllCaps(false);b.setText(label);b.setTextSize(12);b.setTextColor(active?Color.WHITE:BLUE);b.setBackground(shape(active?BLUE:Color.WHITE,14));return b;}
    void refresh(){
        items.removeAllViews();Cursor c=db.getReadableDatabase().rawQuery("SELECT _id,title,body,kind,due,done FROM notes "+(showTasks?"WHERE kind=1 ":"")+"ORDER BY _id DESC",null);
        count.setText(c.getCount()+" saved items • add a note or schedule a reminder");
        if(c.getCount()==0){TextView empty=text("Nothing here yet. Tap + Add to get started.",15,Color.GRAY,false);empty.setGravity(Gravity.CENTER);items.addView(empty,lp(-1,110,8));}
        while(c.moveToNext()){
            final int id=c.getInt(0),kind=c.getInt(3),done=c.getInt(5);final String title=c.getString(1),body=c.getString(2);final long due=c.getLong(4);
            LinearLayout card=col();card.setPadding(dp(16),dp(14),dp(16),dp(14));card.setBackground(shape(Color.WHITE,16));
            TextView t=text((kind==1?"☑  ":"✎  ")+title,18,INK,true);add(card,t,-2);
            if(body!=null&&!body.isEmpty()){TextView desc=text(body,14,Color.DKGRAY,false);desc.setPadding(0,dp(8),0,0);add(card,desc,-2);}
            if(due>0){TextView date=text("⏰ "+new SimpleDateFormat("dd MMM yyyy, hh:mm a",Locale.getDefault()).format(new Date(due)),12,BLUE,false);date.setPadding(0,dp(8),0,0);add(card,date,-2);}
            LinearLayout actions=new LinearLayout(this);
            if(kind==1){Button mark=button(done==1?"Completed ✓":"Mark Done",done==1);actions.addView(mark,new LinearLayout.LayoutParams(0,dp(46),1));mark.setOnClickListener(v->{db.getWritableDatabase().execSQL("UPDATE notes SET done=? WHERE _id=?",new Object[]{done==1?0:1,id});refresh();});}
            Button del=button("Delete",false);actions.addView(del,new LinearLayout.LayoutParams(0,dp(46),1));del.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Delete item?").setMessage(title).setPositiveButton("Delete",(a,b)->{db.getWritableDatabase().delete("notes","_id=?",new String[]{""+id});((AlarmManager)getSystemService(ALARM_SERVICE)).cancel(pending(id));refresh();}).setNegativeButton("Cancel",null).show());
            card.addView(actions,new LinearLayout.LayoutParams(-1,dp(50)));items.addView(card,lp(-1,-2,6));
        }c.close();
    }
    void showEditor(boolean taskDefault){
        LinearLayout form=col();form.setPadding(dp(22),dp(10),dp(22),dp(10));
        EditText title=new EditText(this);title.setSingleLine(true);title.setHint("Title");add(form,title,58);
        EditText body=new EditText(this);body.setHint("Write your note...");body.setMinLines(3);body.setGravity(Gravity.TOP);add(form,body,120);
        CheckBox task=new CheckBox(this);task.setText("This is a task");task.setChecked(taskDefault||showTasks);add(form,task,48);
        CheckBox reminder=new CheckBox(this);reminder.setText("Set reminder / alarm");add(form,reminder,48);
        final Calendar when=Calendar.getInstance();when.add(Calendar.HOUR_OF_DAY,1);final TextView selected=text("Reminder time: Not set",13,BLUE,false);add(form,selected,35);
        Button choose=button("Choose date and time",false);add(form,choose,50);
        choose.setOnClickListener(v->{Calendar now=Calendar.getInstance();DatePickerDialog d=new DatePickerDialog(this,(picker,y,m,day)->{when.set(y,m,day);new TimePickerDialog(this,(tp,h,min)->{when.set(Calendar.HOUR_OF_DAY,h);when.set(Calendar.MINUTE,min);when.set(Calendar.SECOND,0);selected.setText("Reminder: "+new SimpleDateFormat("dd MMM yyyy hh:mm a",Locale.getDefault()).format(when.getTime()));reminder.setChecked(true);},now.get(Calendar.HOUR_OF_DAY),now.get(Calendar.MINUTE),false).show();},now.get(Calendar.YEAR),now.get(Calendar.MONTH),now.get(Calendar.DAY_OF_MONTH));d.getDatePicker().setMinDate(System.currentTimeMillis()-1000);d.show();});
        new AlertDialog.Builder(this).setTitle("New Note / Task").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{
            String name=title.getText().toString().trim();if(name.isEmpty()){Toast.makeText(this,"Please enter a title",Toast.LENGTH_SHORT).show();return;}
            long due=reminder.isChecked()?when.getTimeInMillis():0;if(due>0&&due<System.currentTimeMillis()){Toast.makeText(this,"Reminder time must be in the future",Toast.LENGTH_LONG).show();return;}
            ContentValues cv=new ContentValues();cv.put("title",name);cv.put("body",body.getText().toString());cv.put("kind",task.isChecked()?1:0);cv.put("due",due);cv.put("done",0);
            int id=(int)db.getWritableDatabase().insert("notes",null,cv);if(due>0)schedule(this,id,name,body.getText().toString(),due);render();
        }).show();
    }
    static PendingIntent pending(Context c,int id,String title,String body){
        Intent i=new Intent(c,ReminderReceiver.class);i.putExtra("title",title);i.putExtra("body",body);i.putExtra("id",id);
        return PendingIntent.getBroadcast(c,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    PendingIntent pending(int id){return pending(this,id,"","");}
    static void schedule(Context c,int id,String title,String body,long when){
        AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);PendingIntent pi=pending(c,id,title,body);
        try{if(Build.VERSION.SDK_INT>=31&&!a.canScheduleExactAlarms())a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);else a.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);}
        catch(SecurityException e){a.set(AlarmManager.RTC_WAKEUP,when,pi);}
    }
    public static class ReminderReceiver extends android.content.BroadcastReceiver {
        @Override public void onReceive(Context c,Intent i){
            NotificationManager n=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
            if(Build.VERSION.SDK_INT>=26)n.createNotificationChannel(new NotificationChannel("reminders","MH NoteMate Reminders",NotificationManager.IMPORTANCE_HIGH));
            if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
            Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,"reminders"):new Notification.Builder(c);
            b.setContentTitle(i.getStringExtra("title")).setContentText(i.getStringExtra("body")).setSmallIcon(android.R.drawable.ic_dialog_info).setAutoCancel(true).setDefaults(Notification.DEFAULT_ALL);
            Intent open=new Intent(c,MainActivity.class);b.setContentIntent(PendingIntent.getActivity(c,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
            n.notify(i.getIntExtra("id",1),b.build());
        }
    }
    public static class BootReceiver extends android.content.BroadcastReceiver {
        @Override public void onReceive(Context c,Intent i){
            Db db=new Db(c);Cursor cur=db.getReadableDatabase().rawQuery("SELECT _id,title,body,due FROM notes WHERE due>?",new String[]{""+System.currentTimeMillis()});
            while(cur.moveToNext())schedule(c,cur.getInt(0),cur.getString(1),cur.getString(2),cur.getLong(3));cur.close();db.close();
        }
    }
    static class Db extends SQLiteOpenHelper {
        Db(Context c){super(c,"mh_notemate.db",null,1);}
        @Override public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE notes (_id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,body TEXT,kind INTEGER DEFAULT 0,due INTEGER DEFAULT 0,done INTEGER DEFAULT 0)");}
        @Override public void onUpgrade(SQLiteDatabase d,int a,int b){}
    }
}
