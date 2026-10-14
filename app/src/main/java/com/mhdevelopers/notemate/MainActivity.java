package com.mhdevelopers.notemate;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowInsets;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private final int BLUE=Color.rgb(20,103,247), DARK=Color.rgb(23,34,56), MUTED=Color.rgb(102,114,137);
    private final int BG=Color.rgb(248,250,254), WHITE=Color.WHITE;
    private final int[] PALE={0xFFFFF7DA,0xFFFFE9EF,0xFFE3F1FF,0xFFE4F9E5,0xFFEDE5FF,0xFFFFEDE0};
    private final String[] CATS={"Personal","Work","Study","Other","Important"};
    private final String[] BEFORE={"At time only","5 minutes","10 minutes","15 minutes","30 minutes","1 hour","1 day"};
    private final int[] MINUTES={0,5,10,15,30,60,1440};
    private final String[] REPEAT={"Never","Daily","Weekly"};
    private NotesDb db;
    private LinearLayout root, content, bottomBar;
    private String tab="Home",filter="All",search="";
    private long selectedDay=System.currentTimeMillis();
    private boolean editorOpen=false;
    private String editorReturnTab="Notes";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);getWindow().setStatusBarColor(WHITE);getWindow().setNavigationBarColor(WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        db=new NotesDb(this);
        requestNotifications();showSplash();
    }
    private void requestNotifications() {
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},12);
    }
    private int d(float x){return (int)(getResources().getDisplayMetrics().density*x+0.5f);}
    private GradientDrawable shape(int color,int radius) {GradientDrawable b=new GradientDrawable();b.setColor(color);b.setCornerRadius(d(radius));return b;}
    private GradientDrawable gradient(int a,int b,int radius) {GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});g.setCornerRadius(d(radius));return g;}
    private LinearLayout vert(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout horiz(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:d(w),h<0?h:d(h));}
    private void pad(View v,int l,int t,int r,int b){v.setPadding(d(l),d(t),d(r),d(b));}
    private TextView txt(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private TextView center(String s,int size,int color,boolean bold){TextView t=txt(s,size,color,bold);t.setGravity(Gravity.CENTER);return t;}
    private void space(LinearLayout l,int h){l.addView(new View(this),lp(1,h));}
    private LinearLayout card(int color,int radius){LinearLayout l=vert();l.setBackground(shape(color,radius));pad(l,16,16,16,16);l.setElevation(d(1));return l;}
    private TextView clickText(String s,int size,int color,boolean bold,Runnable run){TextView t=txt(s,size,color,bold);pad(t,8,8,8,8);t.setOnClickListener(v->run.run());return t;}
    private void weight(LinearLayout row,View v){row.addView(v,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));}
    private String date(long t,String pattern){return new SimpleDateFormat(pattern,Locale.ENGLISH).format(new Date(t));}
    private void handleModernInsets(View view) {
        // Android 15+ enforces edge-to-edge. Keep the content below system bars.
        if(Build.VERSION.SDK_INT>=35) {
            view.setOnApplyWindowInsetsListener((v,insets)->{
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(bars.left,bars.top,bars.right,bars.bottom);
                return insets;
            });
        }
    }
    private void showSplash(){
        LinearLayout splash=vert();splash.setGravity(Gravity.CENTER);splash.setBackground(gradient(0xFFF8FCFF,0xFFD5EBFF,0));
        ImageView image=new ImageView(this);image.setImageResource(com.mhdevelopers.notemate.R.drawable.splash_logo);image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        splash.addView(image,lp(292,315));space(splash,8);
        TextView title=center("MH NoteMate",32,DARK,true);SpannableString coloredTitle=new SpannableString("MH NoteMate");coloredTitle.setSpan(new ForegroundColorSpan(0xFF06A9EA),3,11,0);title.setText(coloredTitle);splash.addView(title);
        space(splash,12);splash.addView(center("Your Notes, Tasks & Reminders\nin One Place",15,MUTED,false));
        LinearLayout progress=horiz();progress.setGravity(Gravity.CENTER);TextView line=center("━━━━━",24,BLUE,true);progress.addView(line);LinearLayout.LayoutParams loading=lp(-1,40);loading.topMargin=d(40);splash.addView(progress,loading);
        LinearLayout.LayoutParams creditLp=lp(-1,38);creditLp.topMargin=d(16);
        splash.addView(center("MH Developers",12,MUTED,false),creditLp);
        handleModernInsets(splash);setContentView(splash);new Handler(getMainLooper()).postDelayed(()->render("Home"),1250);
    }
    private void render(String screen){
        editorOpen=false;tab=screen;
        root=vert();root.setBackgroundColor(BG);
        LinearLayout header=horiz();header.setBackgroundColor(WHITE);pad(header,18,12,18,12);
        TextView menu=clickText("☰",23,DARK,false,()->showMenu());header.addView(menu);
        TextView brand=txt(screen.equals("Home")?"MH NoteMate":screen,20,DARK,true);
        if(screen.equals("Home")){SpannableString logoText=new SpannableString("MH NoteMate");logoText.setSpan(new ForegroundColorSpan(BLUE),3,11,0);brand.setText(logoText);}
        weight(header,brand);
        header.addView(clickText("⌕",30,DARK,false,()->searchDialog()));
        header.addView(clickText("●",19,BLUE,true,()->render("Settings")));
        root.addView(header,lp(-1,58));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
        content=vert();pad(content,18,16,18,28);scroll.addView(content);
        FrameLayout page=new FrameLayout(this);
        page.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        root.addView(page,new LinearLayout.LayoutParams(-1,0,1));
        if(screen.equals("Home"))home();
        if(screen.equals("Notes"))notes(false);
        if(screen.equals("Tasks"))notes(true);
        if(screen.equals("Calendar"))calendar();
        if(screen.equals("Settings"))settings();
        if(!screen.equals("Settings")) {
            TextView fab=center("+",34,WHITE,false);
            fab.setBackground(gradient(0xFF20B9FF,0xFF165BF4,100));
            fab.setElevation(d(8));
            FrameLayout.LayoutParams fabParams=new FrameLayout.LayoutParams(d(60),d(60),Gravity.BOTTOM|Gravity.RIGHT);
            fabParams.setMargins(0,0,d(22),d(20));page.addView(fab,fabParams);
            fab.setContentDescription("Add a new note or task");
            fab.setOnClickListener(v->{if(screen.equals("Tasks"))edit(new NotesDb.Note(),true);
                else if(screen.equals("Notes"))edit(new NotesDb.Note(),false);
                else chooseNew();});
        }
        navigation();handleModernInsets(root);setContentView(root);
    }
    private void navigation(){
        bottomBar=horiz();bottomBar.setBackgroundColor(WHITE);pad(bottomBar,3,7,3,8);
        String[] pages={"Home","Notes","Tasks","Calendar","Settings"};String[] icons={"⌂","▣","✓","▦","⚙"};
        for(int i=0;i<pages.length;i++){
            final String page=pages[i];boolean active=tab.equals(page);
            LinearLayout item=vert();item.setGravity(Gravity.CENTER);TextView symbol=center(icons[i],24,active?BLUE:MUTED,false);
            item.addView(symbol);item.addView(center(page,10,active?BLUE:MUTED,active));
            item.setOnClickListener(v->render(page));bottomBar.addView(item,new LinearLayout.LayoutParams(0,d(54),1));
        }
        root.addView(bottomBar,lp(-1,70));
    }
    private void heading(String text,String action,Runnable run){
        LinearLayout line=horiz();TextView label=txt(text,20,DARK,true);weight(line,label);
        if(action!=null)line.addView(clickText(action,13,BLUE,false,run));content.addView(line);space(content,12);
    }
    private void home(){
        LinearLayout hero=card(BLUE,20);hero.setBackground(gradient(0xFF50B5FF,0xFF145DF3,20));pad(hero,20,23,20,23);
        Calendar c=Calendar.getInstance();int hour=c.get(Calendar.HOUR_OF_DAY);
        String greeting=hour<12?"Good Morning":hour<17?"Good Afternoon":"Good Evening";
        hero.addView(txt(greeting+"\nHammad 👋     ☀️",23,WHITE,true));space(hero,16);
        hero.addView(txt(date(System.currentTimeMillis(),"EEE, dd MMM yyyy"),14,WHITE,false));space(hero,5);
        content.addView(hero,lp(-1,-2));space(content,18);
        List<NotesDb.Note> all=db.all();int notes=0,tasks=0,reminders=0,done=0;
        for(NotesDb.Note n:all){if(n.task==0)notes++;else{tasks++;if(n.done==1)done++;}if(n.dueAt>System.currentTimeMillis() && n.done==0)reminders++;}
        LinearLayout a=horiz(),b=horiz();
        stat(a,"▤","Notes",notes,0xFFE5F2FF,"Notes");stat(a,"✓","Tasks",tasks,0xFFE4F9E5,"Tasks");
        stat(b,"♬","Reminders",reminders,0xFFFFECDD,"Calendar");stat(b,"★","Completed",done,0xFFEDE5FF,"Tasks");
        content.addView(a);space(content,10);content.addView(b);space(content,22);
        heading("Today's Schedule","See All",()->render("Calendar"));
        List<NotesDb.Note> today=new ArrayList<>();
        for(NotesDb.Note n:all)if(n.dueAt>0 && n.done==0 && sameDay(n.dueAt,System.currentTimeMillis()))today.add(n);
        Collections.sort(today,Comparator.comparingLong(n->n.dueAt));
        if(today.isEmpty())empty("No reminders scheduled for today", "Tap + to create your first reminder.");
        for(NotesDb.Note n:today)noteCard(n,true);
        space(content,85);content.addView(center("MH Developers",11,MUTED,false));
    }
    private void stat(LinearLayout row,String icon,String label,int number,int color,String dest){
        LinearLayout box=card(WHITE,16);LinearLayout top=horiz();
        TextView bubble=center(icon,23,BLUE,true);bubble.setBackground(shape(color,14));top.addView(bubble,lp(44,44));
        LinearLayout info=vert();pad(info,10,0,0,0);info.addView(txt(label,12,DARK,false));info.addView(txt(""+number,22,DARK,true));top.addView(info);
        box.addView(top);box.setOnClickListener(v->render(dest));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,d(88),1);p.setMargins(d(3),0,d(3),0);row.addView(box,p);
    }
    private void notes(boolean task){
        if(task){LinearLayout top=horiz();top.addView(txt("My Tasks",24,DARK,true),new LinearLayout.LayoutParams(0,-2,1));content.addView(top);space(content,14);}
        if(!task){
            HorizontalScrollView chips=new HorizontalScrollView(this);chips.setHorizontalScrollBarEnabled(false);LinearLayout row=horiz();
            for(String cat:new String[]{"All","Personal","Work","Study","Other","Important"}){
                TextView chip=center(cat,13,filter.equals(cat)?WHITE:DARK,false);pad(chip,15,9,15,9);
                chip.setBackground(shape(filter.equals(cat)?BLUE:0xFFEDEFF5,22));
                LinearLayout.LayoutParams p=lp(-2,38);p.rightMargin=d(7);row.addView(chip,p);
                chip.setOnClickListener(v->{filter=cat;render("Notes");});
            }chips.addView(row);content.addView(chips);space(content,15);
        }
        List<NotesDb.Note> list=db.all();int count=0;
        for(NotesDb.Note n:list){if((n.task==1)!=task)continue;
            if(!task && !filter.equals("All") && !filter.equals(n.category))continue;
            if(!search.isEmpty() && !(n.title+" "+n.body).toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)))continue;
            noteCard(n,false);count++;
        }
        if(count==0)empty(task?"No tasks yet":"No notes found",task?"Add a task to start planning.":"Tap + to create a colorful note.");
        space(content,85);content.addView(center("MH Developers",11,MUTED,false));
    }
    private void noteCard(NotesDb.Note n,boolean compact){
        LinearLayout box=card(PALE[Math.floorMod(n.color,PALE.length)],17);
        LinearLayout line=horiz();
        if(n.task==1){CheckBox check=new CheckBox(this);check.setButtonTintList(android.content.res.ColorStateList.valueOf(BLUE));check.setChecked(n.done==1);
            check.setOnCheckedChangeListener((v,checked)->{n.done=checked?1:0;db.save(n);AlarmHelper.schedule(this,n);render(tab);});line.addView(check);}
        else {TextView icon=center(n.pinned==1?"★":"▤",23,BLUE,true);line.addView(icon,lp(42,42));}
        LinearLayout words=vert();TextView title=txt(n.title,16,DARK,true);if(n.done==1)title.setPaintFlags(title.getPaintFlags()|android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        words.addView(title);
        if(!n.body.trim().isEmpty()){TextView desc=txt(n.body.replace('\n',' '),13,MUTED,false);desc.setMaxLines(compact?1:2);words.addView(desc);}
        if(n.dueAt>0)words.addView(txt("◷ "+date(n.dueAt,"dd MMM, h:mm a")+(n.before>0?"  •  "+n.before+"m early":""),11,MUTED,false));
        else words.addView(txt(n.category+"  •  "+date(n.updatedAt,"dd MMM, h:mm a"),11,MUTED,false));
        line.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        line.addView(clickText("⋮",23,MUTED,false,()->noteOptions(n)));
        box.addView(line);box.setOnClickListener(v->edit(n,n.task==1));
        LinearLayout.LayoutParams p=lp(-1,-2);p.bottomMargin=d(11);content.addView(box,p);
    }
    private void noteOptions(NotesDb.Note n){
        new AlertDialog.Builder(this).setTitle(n.title).setItems(new String[]{"Edit","Pin / Unpin","Delete"},(dialog,which)->{
            if(which==0)edit(n,n.task==1);
            if(which==1){n.pinned=1-n.pinned;db.save(n);render(tab);}
            if(which==2)new AlertDialog.Builder(this).setTitle("Delete note?").setMessage("This action cannot be undone.")
                .setNegativeButton("Cancel",null).setPositiveButton("Delete",(di,w)->{AlarmHelper.cancel(this,n.id);db.delete(n.id);render(tab);}).show();
        }).show();
    }
    private void empty(String title,String detail){
        LinearLayout l=card(WHITE,17);l.setGravity(Gravity.CENTER);pad(l,18,35,18,35);
        l.addView(center("📝",36,BLUE,false));space(l,9);l.addView(center(title,16,DARK,true));space(l,5);
        l.addView(center(detail,13,MUTED,false));content.addView(l);
    }
    private boolean sameDay(long a,long b){return date(a,"yyyyMMdd").equals(date(b,"yyyyMMdd"));}
    private void calendar(){
        heading("My Calendar",null,null);
        Calendar c=Calendar.getInstance();c.setTimeInMillis(selectedDay);
        DatePicker picker=new DatePicker(this);picker.setCalendarViewShown(false);picker.init(c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH),(v,y,m,day)->{
            Calendar picked=Calendar.getInstance();picked.set(y,m,day);selectedDay=picked.getTimeInMillis();render("Calendar");
        });content.addView(picker,lp(-1,-2));space(content,16);
        heading("Schedule • "+date(selectedDay,"dd MMM yyyy"),"+ New",()->edit(new NotesDb.Note(),true));
        List<NotesDb.Note> list=db.all();int count=0;
        for(NotesDb.Note n:list)if(n.dueAt>0 && sameDay(n.dueAt,selectedDay)){noteCard(n,true);count++;}
        if(count==0)empty("No plans on this day","Choose another date or add a reminder.");
    }
    private void settings(){
        heading("Preferences",null,null);
        LinearLayout c=card(WHITE,17);
        settingsRow(c,"🔔","Notification permission","Allow MH NoteMate to send reminders",()->requestNotifications());
        settingsRow(c,"⏰","Precise alarm access","For reminders to ring at the chosen time",()->exactAlarmSettings());
        settingsRow(c,"🔁","Restore scheduled alarms","Reschedule all upcoming reminders",()->{AlarmHelper.rescheduleAll(this);Toast.makeText(this,"Reminders refreshed",Toast.LENGTH_SHORT).show();});
        content.addView(c);space(content,18);heading("About",null,null);
        LinearLayout about=card(WHITE,18);about.setGravity(Gravity.CENTER);space(about,10);
        ImageView icon=new ImageView(this);icon.setImageResource(R.mipmap.ic_launcher);about.addView(icon,lp(90,90));
        space(about,10);about.addView(center("MH NoteMate",23,DARK,true));space(about,5);
        about.addView(center("Your Notes, Tasks & Reminders in One Place",13,MUTED,false));space(about,14);
        about.addView(center("Version 1.0.0",12,MUTED,false));space(about,10);
        about.addView(center("Developed by MH Developers",14,BLUE,true));space(about,10);
        content.addView(about);space(content,18);
        content.addView(center("Your notes are saved locally on this device.\nNo account or internet connection required.",12,MUTED,false));
    }
    private void settingsRow(LinearLayout parent,String icon,String title,String subtitle,Runnable action){
        LinearLayout row=horiz();TextView left=center(icon,26,BLUE,false);row.addView(left,lp(46,55));
        LinearLayout text=vert();text.addView(txt(title,15,DARK,true));text.addView(txt(subtitle,12,MUTED,false));row.addView(text,new LinearLayout.LayoutParams(0,-2,1));
        row.addView(txt("›",26,MUTED,false));pad(row,0,8,0,8);parent.addView(row);row.setOnClickListener(v->action.run());
    }
    private void exactAlarmSettings(){
        if(Build.VERSION.SDK_INT>=31){
            AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
            if(!am.canScheduleExactAlarms()){
                try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:"+getPackageName())));}
                catch(Exception e){Toast.makeText(this,"Open system settings to allow alarms",Toast.LENGTH_LONG).show();}
            }else Toast.makeText(this,"Precise alarms are already allowed",Toast.LENGTH_SHORT).show();
        }else Toast.makeText(this,"Precise alarms are available",Toast.LENGTH_SHORT).show();
    }
    private void showMenu(){new AlertDialog.Builder(this).setTitle("MH NoteMate").setItems(new String[]{"Home","Notes","Tasks","Calendar","Settings"},(d,w)->render(new String[]{"Home","Notes","Tasks","Calendar","Settings"}[w])).show();}
    private void searchDialog(){
        EditText input=new EditText(this);input.setSingleLine(true);input.setHint("Search notes and tasks");
        new AlertDialog.Builder(this).setTitle("Search").setView(input).setNegativeButton("Cancel",null)
            .setNeutralButton("Clear",(d,w)->{search="";render("Notes");})
            .setPositiveButton("Search",(d,w)->{search=input.getText().toString().trim();filter="All";render("Notes");}).show();
    }
    private void chooseNew(){new AlertDialog.Builder(this).setTitle("Create new").setItems(new String[]{"📝 Note or reminder","✓ Task or scheduled task"},(di,w)->edit(new NotesDb.Note(),w==1)).show();}
    private TextView primary(String text){TextView t=center(text,16,WHITE,true);t.setBackground(gradient(0xFF23B5FC,0xFF1558F0,14));return t;}
    private EditText field(String hint,int lines){
        EditText e=new EditText(this);e.setTextSize(16);e.setTextColor(DARK);e.setHintTextColor(MUTED);e.setHint(hint);
        e.setBackground(shape(WHITE,14));pad(e,15,13,15,13);
        if(lines>1){e.setGravity(Gravity.TOP);e.setMinLines(lines);e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);}
        else {e.setSingleLine(true);e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);}
        return e;
    }
    private Spinner spinner(String[] items,int selected){
        Spinner s=new Spinner(this);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,items);s.setAdapter(adapter);s.setSelection(Math.max(0,selected));return s;
    }
    private void editorRow(LinearLayout panel,String label,View value){
        LinearLayout line=horiz();TextView title=txt(label,14,DARK,true);line.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        line.addView(value,new LinearLayout.LayoutParams(d(155),-2));pad(line,0,7,0,7);panel.addView(line);
    }
    private void edit(NotesDb.Note note,boolean isTask){
        editorReturnTab=tab;
        editorOpen=true;
        NotesDb.Note n=note; n.task=isTask?1:0;
        root=vert();root.setBackgroundColor(BG);
        LinearLayout bar=horiz();pad(bar,16,10,16,10);bar.setBackgroundColor(WHITE);
        bar.addView(clickText("‹",30,DARK,false,()->render(editorReturnTab)));
        TextView title=txt(n.id==0?(isTask?"Add Task":"Add Note"):(isTask?"Edit Task":"Edit Note"),21,DARK,true);weight(bar,title);
        TextView save=primary("✓");bar.addView(save,lp(46,44));root.addView(bar,lp(-1,64));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);content=vert();pad(content,18,18,18,20);scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        EditText name=field(isTask?"Task title":"Note title",1);name.setText(n.title);content.addView(name,lp(-1,58));space(content,13);
        EditText body=field("Write your thoughts, details or checklist...",6);body.setText(n.body);content.addView(body,lp(-1,160));space(content,16);
        space(content,1);
        LinearLayout colors=horiz();final int[] color={n.color};
        for(int i=0;i<PALE.length;i++){
            final int index=i;TextView dot=center(i==color[0]?"✓":"",19,BLUE,true);dot.setBackground(shape(PALE[i],30));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,d(43),1);p.setMargins(d(3),0,d(3),0);colors.addView(dot,p);
            dot.setOnClickListener(v->{color[0]=index;for(int j=0;j<colors.getChildCount();j++)((TextView)colors.getChildAt(j)).setText(j==index?"✓":"");});
        }content.addView(colors);space(content,16);
        LinearLayout options=card(WHITE,17);
        Spinner category=spinner(CATS,Math.max(0,Arrays.asList(CATS).indexOf(n.category)));
        CheckBox alarm=new CheckBox(this);alarm.setText("Enable reminder notification");alarm.setTextSize(14);alarm.setTextColor(DARK);alarm.setButtonTintList(android.content.res.ColorStateList.valueOf(BLUE));alarm.setChecked(n.dueAt>0);
        final Calendar when=Calendar.getInstance();when.setTimeInMillis(n.dueAt>0?n.dueAt:System.currentTimeMillis()+3600_000L);
        TextView day=clickText(date(when.getTimeInMillis(),"dd MMM yyyy")+"   ›",14,BLUE,true,()->{});
        TextView time=clickText(date(when.getTimeInMillis(),"h:mm a")+"   ›",14,BLUE,true,()->{});
        day.setOnClickListener(v->{alarm.setChecked(true);new DatePickerDialog(this,(view,y,m,d)->{when.set(Calendar.YEAR,y);when.set(Calendar.MONTH,m);when.set(Calendar.DAY_OF_MONTH,d);day.setText(date(when.getTimeInMillis(),"dd MMM yyyy")+"   ›");},when.get(Calendar.YEAR),when.get(Calendar.MONTH),when.get(Calendar.DAY_OF_MONTH)).show();});
        time.setOnClickListener(v->{alarm.setChecked(true);new TimePickerDialog(this,(view,h,min)->{when.set(Calendar.HOUR_OF_DAY,h);when.set(Calendar.MINUTE,min);when.set(Calendar.SECOND,0);when.set(Calendar.MILLISECOND,0);time.setText(date(when.getTimeInMillis(),"h:mm a")+"   ›");},when.get(Calendar.HOUR_OF_DAY),when.get(Calendar.MINUTE),false).show();});
        LinearLayout alarmOptions=vert();editorRow(alarmOptions,"Date",day);editorRow(alarmOptions,"Time",time);
        int selectedBefore=0;for(int i=0;i<MINUTES.length;i++)if(MINUTES[i]==n.before)selectedBefore=i;
        Spinner before=spinner(BEFORE,selectedBefore);editorRow(alarmOptions,"Remind before",before);
        Spinner repeat=spinner(REPEAT,Arrays.asList(REPEAT).indexOf(n.repeat));editorRow(alarmOptions,"Repeat",repeat);
        options.addView(alarmOptions);editorRow(options,"Category",category);options.addView(alarm);
        alarmOptions.setVisibility(View.VISIBLE);
        alarm.setOnCheckedChangeListener((b,checked)->{day.setAlpha(checked?1f:0.7f);time.setAlpha(checked?1f:0.7f);});
        content.addView(options);space(content,20);
        TextView saveBig=primary(n.id==0?"Save Note":"Save Changes");content.addView(saveBig,lp(-1,52));
        Runnable saveAction=()->{
            String value=name.getText().toString().trim();if(value.isEmpty()){name.setError("Please enter a title");name.requestFocus();return;}
            n.title=value;n.body=body.getText().toString().trim();n.color=color[0];n.category=CATS[category.getSelectedItemPosition()];
            n.dueAt=alarm.isChecked()?when.getTimeInMillis():0;
            n.before=alarm.isChecked()?MINUTES[before.getSelectedItemPosition()]:0;
            n.repeat=alarm.isChecked()?REPEAT[repeat.getSelectedItemPosition()]:"Never";
            if(n.dueAt>0 && n.dueAt<=System.currentTimeMillis()){Toast.makeText(this,"Choose a future date and time",Toast.LENGTH_LONG).show();return;}
            n.id=db.save(n);AlarmHelper.schedule(this,n);
            if(n.dueAt>0 && Build.VERSION.SDK_INT>=31){
                AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
                if(!am.canScheduleExactAlarms())new AlertDialog.Builder(this).setTitle("Enable precise alarms")
                    .setMessage("Your reminder is saved. For reliable on-time alerts, allow MH NoteMate to schedule exact alarms in Android settings.")
                    .setNegativeButton("Later",(d,w)->render(isTask?"Tasks":"Notes"))
                    .setPositiveButton("Open Settings",(d,w)->{exactAlarmSettings();render(isTask?"Tasks":"Notes");}).show();
                else render(isTask?"Tasks":"Notes");
            }else render(isTask?"Tasks":"Notes");
            Toast.makeText(this,"Saved successfully",Toast.LENGTH_SHORT).show();
        };
        save.setOnClickListener(v->saveAction.run());saveBig.setOnClickListener(v->saveAction.run());
        space(content,25);content.addView(center("MH Developers",11,MUTED,false));
        handleModernInsets(root);setContentView(root);
    }
    @Override public void onBackPressed(){if(editorOpen)render(editorReturnTab);else if(!tab.equals("Home"))render("Home");else super.onBackPressed();}
    @Override protected void onResume(){super.onResume();if(db!=null && !editorOpen && root!=null && !tab.isEmpty())AlarmHelper.rescheduleAll(this);}
    @Override protected void onDestroy(){if(db!=null)db.close();super.onDestroy();}
}
