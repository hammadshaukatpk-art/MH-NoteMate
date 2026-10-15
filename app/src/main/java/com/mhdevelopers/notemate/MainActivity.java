package com.mhdevelopers.notemate;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.media.RingtoneManager;
import android.media.Ringtone;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.content.Context;
import android.net.Uri;
import android.graphics.drawable.ColorDrawable;
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
    private boolean splashShowing=false;
    private String editorReturnTab="Notes";
    private static final int PICK_ALARM_TONE=810;
    private String selectedToneUri="";
    private TextView selectedToneLabel;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);getWindow().setStatusBarColor(WHITE);getWindow().setNavigationBarColor(WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        db=new NotesDb(this);
        requestNotifications();
        if(Build.VERSION.SDK_INT>=33){
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,()->handleBack());
        }
        showSplash();
    }
    private void requestNotifications() {
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            {getPreferences(MODE_PRIVATE).edit().putBoolean("notification_asked",true).apply();requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},12);}
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
        splashShowing=true;
        FrameLayout stage=new FrameLayout(this);
        stage.setBackground(gradient(0xFFF8FCFF,0xFFD5EBFF,0));
        LinearLayout splash=vert();splash.setGravity(Gravity.CENTER);
        ImageView image=new ImageView(this);image.setImageResource(R.drawable.splash_logo);image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        splash.addView(image,lp(292,315));space(splash,8);
        TextView title=center("MH NoteMate",32,DARK,true);
        SpannableString coloredTitle=new SpannableString("MH NoteMate");
        coloredTitle.setSpan(new ForegroundColorSpan(0xFF06A9EA),3,11,0);
        title.setText(coloredTitle);splash.addView(title);
        space(splash,12);splash.addView(center("Your Notes, Tasks & Reminders\nin One Place",15,MUTED,false));
        LinearLayout progress=horiz();progress.setGravity(Gravity.CENTER);
        TextView line=center("━━━━━",24,BLUE,true);progress.addView(line);
        LinearLayout.LayoutParams loading=lp(-1,40);loading.topMargin=d(40);splash.addView(progress,loading);
        FrameLayout.LayoutParams mainParams=new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER);
        mainParams.bottomMargin=d(34);stage.addView(splash,mainParams);
        TextView credit=center("Credit : MH Developers",16,BLUE,true);
        credit.setLetterSpacing(0.025f);
        FrameLayout.LayoutParams footer=new FrameLayout.LayoutParams(-1,d(50),Gravity.BOTTOM);
        footer.setMargins(d(12),0,d(12),d(24));stage.addView(credit,footer);
        handleModernInsets(stage);setContentView(stage);
        new Handler(getMainLooper()).postDelayed(()->render("Home"),1600);
    }
    private View toolbarIcon(boolean searchIcon,Runnable action){
        View icon=new View(this){
            final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas c){
                super.onDraw(c);
                float cx=getWidth()/2f,cy=getHeight()/2f,unit=d(1);
                paint.setColor(DARK);paint.setStrokeWidth(d(2.2f));paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStyle(Paint.Style.STROKE);
                if(searchIcon){
                    c.drawCircle(cx-d(3),cy-d(3),d(7),paint);
                    c.drawLine(cx+d(2),cy+d(2),cx+d(10),cy+d(10),paint);
                }else{
                    c.drawLine(cx-d(9),cy-d(4),cx+d(9),cy-d(4),paint);
                    c.drawLine(cx-d(9),cy+d(4),cx+d(9),cy+d(4),paint);
                }
            }
        };
        icon.setContentDescription(searchIcon?"Search notes":"Open navigation menu");
        icon.setOnClickListener(v->action.run());
        return icon;
    }
    private View plusIcon(Runnable action){
        View plus=new View(this){
            final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas c){
                super.onDraw(c);float x=getWidth()/2f,y=getHeight()/2f;
                paint.setColor(WHITE);paint.setStrokeWidth(d(3));paint.setStrokeCap(Paint.Cap.ROUND);
                c.drawLine(x-d(10),y,x+d(10),y,paint);
                c.drawLine(x,y-d(10),x,y+d(10),paint);
            }
        };
        plus.setBackground(gradient(0xFF20B9FF,0xFF165BF4,100));plus.setElevation(d(8));
        plus.setContentDescription("Add a new note or task");plus.setOnClickListener(v->action.run());
        return plus;
    }
    private TextView dialogHeading(String label){
        TextView title=txt(label,21,WHITE,true);pad(title,20,20,20,20);
        title.setBackground(gradient(0xFF50B5FF,0xFF145DF3,16));return title;
    }
    private AlertDialog themedDialog(LinearLayout layout){
        AlertDialog dialog=new AlertDialog.Builder(this).setView(layout).create();
        dialog.setOnShowListener(x->{
            if(dialog.getWindow()!=null){dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*.89f),-2);}
        });return dialog;
    }
    private TextView dialogButton(String label,boolean filled){
        TextView b=center(label,15,filled?WHITE:BLUE,true);pad(b,13,13,13,13);
        b.setBackground(shape(filled?BLUE:0xFFE9F1FF,12));return b;
    }
    private TextView creditFooter(){
        TextView credit=center("Credit: MH Developers",13,BLUE,true);
        credit.setLetterSpacing(0.018f);
        credit.setBackgroundColor(WHITE);
        return credit;
    }
    private View editorBackArrow(Runnable action){
        View arrow=new View(this){
            private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas canvas){
                super.onDraw(canvas);
                float cx=getWidth()/2f,cy=getHeight()/2f;
                paint.setColor(DARK);paint.setStrokeWidth(d(2.5f));
                paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
                paint.setStyle(Paint.Style.STROKE);
                android.graphics.Path path=new android.graphics.Path();
                path.moveTo(cx+d(5),cy-d(9));path.lineTo(cx-d(4),cy);
                path.lineTo(cx+d(5),cy+d(9));
                canvas.drawPath(path,paint);
            }
        };
        arrow.setContentDescription("Back to "+editorReturnTab);
        arrow.setOnClickListener(v->action.run());
        return arrow;
    }
    private void handleBack(){
        if(splashShowing){return;}
        if(editorOpen){render(editorReturnTab);return;}
        if(!"Home".equals(tab)){render("Home");return;}
        showExitConfirmation();
    }
    private void showExitConfirmation(){
        LinearLayout shell=vert();shell.setBackground(shape(WHITE,20));
        shell.addView(dialogHeading("Exit MH NoteMate?"));
        LinearLayout body=vert();pad(body,20,18,20,20);
        TextView message=txt("Do you really want to exit the app?",15,DARK,false);
        body.addView(message);space(body,22);
        LinearLayout actions=horiz();
        TextView no=dialogButton("No, Stay",false),yes=dialogButton("Yes, Exit",true);
        LinearLayout.LayoutParams first=new LinearLayout.LayoutParams(0,d(48),1);first.rightMargin=d(10);
        actions.addView(no,first);actions.addView(yes,new LinearLayout.LayoutParams(0,d(48),1));
        body.addView(actions);shell.addView(body);
        AlertDialog dialog=themedDialog(shell);
        no.setOnClickListener(v->dialog.dismiss());
        yes.setOnClickListener(v->{dialog.dismiss();finishAffinity();});
        dialog.show();
    }
    private void render(String screen){
        editorOpen=false;splashShowing=false;tab=screen;
        root=vert();root.setBackgroundColor(BG);
        LinearLayout header=horiz();header.setBackgroundColor(WHITE);pad(header,12,5,12,5);
        View menu=toolbarIcon(false,()->showMenu());header.addView(menu,lp(44,48));
        TextView brand=txt(screen.equals("Home")?"MH NoteMate":screen,20,DARK,true);
        if(screen.equals("Home")){SpannableString logoText=new SpannableString("MH NoteMate");logoText.setSpan(new ForegroundColorSpan(BLUE),3,11,0);brand.setText(logoText);}
        pad(brand,4,0,0,0);weight(header,brand);
        View searchButton=toolbarIcon(true,()->searchDialog());header.addView(searchButton,lp(48,48));
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
            View fab=plusIcon(()->{if(screen.equals("Tasks"))edit(new NotesDb.Note(),true);
                else if(screen.equals("Notes"))edit(new NotesDb.Note(),false);
                else chooseNew();});
            FrameLayout.LayoutParams fabParams=new FrameLayout.LayoutParams(d(60),d(60),Gravity.BOTTOM|Gravity.RIGHT);
            fabParams.setMargins(0,0,d(22),d(20));page.addView(fab,fabParams);
        }
        if(screen.equals("Notes") || screen.equals("Tasks")){
            root.addView(creditFooter(),lp(-1,30));
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
        hero.addView(txt(greeting+"  ☀️",23,WHITE,true));space(hero,16);
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
        space(content,85);
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
        space(content,85);
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
        themedChoices(n.title,new String[]{"Edit","Pin / Unpin","Delete"},
            new String[]{"Open and update this item","Keep it at the top of your list","Permanently remove this item"},
            new String[]{"✎","★","⌫"},which->{
                if(which==0)edit(n,n.task==1);
                if(which==1){n.pinned=1-n.pinned;db.save(n);render(tab);}
                if(which==2)themedConfirm("Delete "+(n.task==1?"Task":"Note")+"?",
                    "This action cannot be undone.","Cancel","Delete",null,()->{
                        AlarmHelper.cancel(this,n.id);db.delete(n.id);render(tab);
                    });
            });
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
        settingsRow(c,"🔔","Notification permission","Allow MH NoteMate to send reminders",()->openNotificationPermission());
        settingsRow(c,"⏰","Precise alarm access","For reminders to ring at the chosen time",()->exactAlarmSettings());
        settingsRow(c,"🔁","Restore scheduled alarms","Reschedule all upcoming reminders",()->{AlarmHelper.rescheduleAll(this);Toast.makeText(this,"Reminders refreshed",Toast.LENGTH_SHORT).show();});
        content.addView(c);space(content,18);heading("About",null,null);
        LinearLayout about=card(WHITE,18);about.setGravity(Gravity.CENTER);space(about,10);
        ImageView icon=new ImageView(this);icon.setImageResource(R.mipmap.ic_launcher);about.addView(icon,lp(90,90));
        space(about,10);about.addView(center("MH NoteMate",23,DARK,true));space(about,5);
        about.addView(center("Your Notes, Tasks & Reminders in One Place",13,MUTED,false));space(about,14);
        about.addView(center("Version 1.0",12,MUTED,false));space(about,10);
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
    private void openNotificationPermission(){
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
            boolean asked=getPreferences(MODE_PRIVATE).getBoolean("notification_asked",false);
            if(!asked || shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)){
                getPreferences(MODE_PRIVATE).edit().putBoolean("notification_asked",true).apply();
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},12);
                return;
            }
            // Permanently denied: Android cannot display the runtime prompt again.
            // Open the app's notification settings instead.
        }
        // Already granted? The row must still respond, allowing the user to review
        // notification channels and settings instead of silently doing nothing.
        try{
            Intent i=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            i.putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName());
            startActivity(i);
        }catch(Exception e){
            try{startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}
            catch(Exception ignored){Toast.makeText(this,"Notification settings are unavailable on this device",Toast.LENGTH_LONG).show();}
        }
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==12 && !splashShowing){
            boolean allowed=grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED;
            Toast.makeText(this,allowed?"Notifications enabled":"Notifications not enabled. Tap again to open settings.",Toast.LENGTH_LONG).show();
        }
    }
    private void showMenu(){
        LinearLayout shell=vert();shell.setBackground(shape(WHITE,20));
        shell.addView(dialogHeading("MH NoteMate  •  Menu"));
        final AlertDialog dialog=themedDialog(shell);
        String[] labels={"Home","Notes","Tasks","Calendar","Settings"};
        String[] symbols={"⌂","▣","✓","▦","⚙"};
        for(int i=0;i<labels.length;i++){
            final String destination=labels[i];
            LinearLayout row=horiz();pad(row,18,10,18,10);
            TextView symbol=center(symbols[i],23,BLUE,true);symbol.setBackground(shape(0xFFEAF3FF,12));row.addView(symbol,lp(42,42));
            TextView name=txt(labels[i],16,DARK,tab.equals(labels[i]));pad(name,16,0,0,0);weight(row,name);
            row.addView(txt("›",23,MUTED,false));
            row.setOnClickListener(v->{dialog.dismiss();render(destination);});
            shell.addView(row,lp(-1,61));
            if(i<labels.length-1){View separator=new View(this);separator.setBackgroundColor(0xFFEDF1F8);
                LinearLayout.LayoutParams sep=lp(-1,1);sep.leftMargin=d(18);sep.rightMargin=d(18);shell.addView(separator,sep);}
        }
        space(shell,8);dialog.show();
    }
    private void searchDialog(){
        LinearLayout shell=vert();shell.setBackground(shape(WHITE,20));
        shell.addView(dialogHeading("Search Notes & Tasks"));
        LinearLayout body=vert();pad(body,18,18,18,18);
        EditText input=field("Search notes and tasks...",1);input.setText(search);
        input.setBackground(shape(0xFFF0F5FD,12));body.addView(input,lp(-1,55));space(body,18);
        LinearLayout actions=horiz();TextView clear=dialogButton("Clear",false);
        TextView cancel=dialogButton("Cancel",false);TextView submit=dialogButton("Search",true);
        LinearLayout.LayoutParams btn=new LinearLayout.LayoutParams(0,d(47),1);
        btn.rightMargin=d(7);actions.addView(clear,btn);
        LinearLayout.LayoutParams btn2=new LinearLayout.LayoutParams(0,d(47),1);btn2.rightMargin=d(7);actions.addView(cancel,btn2);
        actions.addView(submit,new LinearLayout.LayoutParams(0,d(47),1));body.addView(actions);
        shell.addView(body);AlertDialog dialog=themedDialog(shell);
        clear.setOnClickListener(v->{search="";dialog.dismiss();render("Notes");});
        cancel.setOnClickListener(v->dialog.dismiss());
        submit.setOnClickListener(v->{search=input.getText().toString().trim();filter="All";dialog.dismiss();render("Notes");});
        dialog.show();
    }
    private interface ChoiceHandler {void select(int index);}

    private void themedChoices(String heading,String[] options,String[] descriptions,String[] icons,ChoiceHandler onChoose){
        LinearLayout shell=vert();shell.setBackground(shape(WHITE,20));
        shell.addView(dialogHeading(heading));
        LinearLayout body=vert();pad(body,18,16,18,18);
        final AlertDialog dialog=themedDialog(shell);
        for(int i=0;i<options.length;i++){
            final int index=i;
            LinearLayout row=horiz();pad(row,12,11,12,11);
            row.setBackground(shape(i%2==0?0xFFEDF5FF:0xFFF5F8FF,14));
            if(icons!=null && i<icons.length){
                TextView icon=center(icons[i],22,BLUE,true);
                icon.setBackground(shape(0xFFDDEBFF,12));
                row.addView(icon,lp(42,42));
            }
            LinearLayout labels=vert();pad(labels,12,0,0,0);
            labels.addView(txt(options[i],16,DARK,true));
            if(descriptions!=null && i<descriptions.length && descriptions[i]!=null){
                space(labels,4);labels.addView(txt(descriptions[i],12,MUTED,false));
            }
            weight(row,labels);
            row.addView(center("›",22,BLUE,true));
            row.setOnClickListener(v->{dialog.dismiss();onChoose.select(index);});
            body.addView(row,lp(-1,descriptions==null?56:69));
            space(body,9);
        }
        TextView cancel=dialogButton("Cancel",false);
        cancel.setOnClickListener(v->dialog.dismiss());
        body.addView(cancel,lp(-1,46));
        shell.addView(body);dialog.show();
    }

    private void themedConfirm(String heading,String message,String cancelLabel,String acceptLabel,Runnable onCancel,Runnable onAccept){
        LinearLayout shell=vert();shell.setBackground(shape(WHITE,20));
        shell.addView(dialogHeading(heading));
        LinearLayout body=vert();pad(body,20,18,20,20);
        TextView description=txt(message,15,DARK,false);
        description.setLineSpacing(d(4),1f);
        body.addView(description);space(body,24);
        LinearLayout buttons=horiz();
        TextView cancel=dialogButton(cancelLabel,false);
        TextView accept=dialogButton(acceptLabel,true);
        LinearLayout.LayoutParams first=new LinearLayout.LayoutParams(0,d(48),1);first.rightMargin=d(10);
        buttons.addView(cancel,first);buttons.addView(accept,new LinearLayout.LayoutParams(0,d(48),1));
        body.addView(buttons);shell.addView(body);
        final AlertDialog dialog=themedDialog(shell);
        cancel.setOnClickListener(v->{dialog.dismiss();if(onCancel!=null)onCancel.run();});
        accept.setOnClickListener(v->{dialog.dismiss();if(onAccept!=null)onAccept.run();});
        dialog.setOnCancelListener(v->{if(onCancel!=null)onCancel.run();});
        dialog.show();
    }

    private void themedDatePicker(Calendar when,TextView day){
        LinearLayout shell=vert();shell.setBackground(shape(WHITE,20));
        shell.addView(dialogHeading("Choose Reminder Date"));
        DatePicker picker=new DatePicker(this);
        picker.setCalendarViewShown(false);
        picker.init(when.get(Calendar.YEAR),when.get(Calendar.MONTH),when.get(Calendar.DAY_OF_MONTH),null);
        LinearLayout body=vert();pad(body,12,10,12,16);
        body.addView(picker,lp(-1,-2));space(body,8);
        LinearLayout buttons=horiz();
        TextView cancel=dialogButton("Cancel",false),apply=dialogButton("Set Date",true);
        LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,d(48),1);left.rightMargin=d(10);
        buttons.addView(cancel,left);buttons.addView(apply,new LinearLayout.LayoutParams(0,d(48),1));
        body.addView(buttons);shell.addView(body);
        final AlertDialog dialog=themedDialog(shell);
        cancel.setOnClickListener(v->dialog.dismiss());
        apply.setOnClickListener(v->{
            when.set(Calendar.YEAR,picker.getYear());when.set(Calendar.MONTH,picker.getMonth());
            when.set(Calendar.DAY_OF_MONTH,picker.getDayOfMonth());
            day.setText(date(when.getTimeInMillis(),"dd MMM yyyy")+"   ›");
            dialog.dismiss();
        });
        dialog.show();
    }

    private void themedTimePicker(Calendar when,TextView time){
        LinearLayout shell=vert();shell.setBackground(shape(WHITE,20));
        shell.addView(dialogHeading("Choose Reminder Time"));
        TimePicker picker=new TimePicker(this);
        picker.setIs24HourView(false);
        picker.setHour(when.get(Calendar.HOUR_OF_DAY));
        picker.setMinute(when.get(Calendar.MINUTE));
        LinearLayout body=vert();pad(body,12,10,12,16);
        body.addView(picker,lp(-1,-2));space(body,8);
        LinearLayout buttons=horiz();
        TextView cancel=dialogButton("Cancel",false),apply=dialogButton("Set Time",true);
        LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,d(48),1);left.rightMargin=d(10);
        buttons.addView(cancel,left);buttons.addView(apply,new LinearLayout.LayoutParams(0,d(48),1));
        body.addView(buttons);shell.addView(body);
        final AlertDialog dialog=themedDialog(shell);
        cancel.setOnClickListener(v->dialog.dismiss());
        apply.setOnClickListener(v->{
            when.set(Calendar.HOUR_OF_DAY,picker.getHour());when.set(Calendar.MINUTE,picker.getMinute());
            when.set(Calendar.SECOND,0);when.set(Calendar.MILLISECOND,0);
            time.setText(date(when.getTimeInMillis(),"h:mm a")+"   ›");
            dialog.dismiss();
        });
        dialog.show();
    }

    private void chooseNew(){
        // Match the app's blue-white dialogs instead of Android's plain system list.
        LinearLayout shell=vert();
        shell.setBackground(shape(WHITE,20));
        shell.addView(dialogHeading("Create New"));
        LinearLayout body=vert();
        pad(body,18,18,18,18);
        TextView intro=txt("What would you like to create?",15,MUTED,false);
        body.addView(intro);
        space(body,16);
        final AlertDialog dialog=themedDialog(shell);

        LinearLayout noteRow=horiz();
        pad(noteRow,13,12,13,12);
        noteRow.setBackground(shape(0xFFEDF5FF,15));
        TextView noteIcon=center("✎",25,BLUE,true);
        noteIcon.setBackground(shape(0xFFD8E9FF,13));
        noteRow.addView(noteIcon,lp(49,49));
        LinearLayout noteLabels=vert();
        pad(noteLabels,14,0,0,0);
        noteLabels.addView(txt("New Note",17,DARK,true));
        space(noteLabels,4);
        noteLabels.addView(txt("Write ideas, lists or reminders",12,MUTED,false));
        weight(noteRow,noteLabels);
        noteRow.addView(center("›",26,BLUE,true));
        noteRow.setOnClickListener(v->{dialog.dismiss();edit(new NotesDb.Note(),false);});
        body.addView(noteRow,lp(-1,76));

        space(body,12);
        LinearLayout taskRow=horiz();
        pad(taskRow,13,12,13,12);
        taskRow.setBackground(shape(0xFFEAF9F0,15));
        TextView taskIcon=center("✓",25,0xFF149C54,true);
        taskIcon.setBackground(shape(0xFFD3F5E0,13));
        taskRow.addView(taskIcon,lp(49,49));
        LinearLayout taskLabels=vert();
        pad(taskLabels,14,0,0,0);
        taskLabels.addView(txt("New Task",17,DARK,true));
        space(taskLabels,4);
        taskLabels.addView(txt("Plan a task with an alarm",12,MUTED,false));
        weight(taskRow,taskLabels);
        taskRow.addView(center("›",26,0xFF149C54,true));
        taskRow.setOnClickListener(v->{dialog.dismiss();edit(new NotesDb.Note(),true);});
        body.addView(taskRow,lp(-1,76));

        space(body,18);
        TextView cancel=dialogButton("Cancel",false);
        cancel.setOnClickListener(v->dialog.dismiss());
        body.addView(cancel,lp(-1,48));
        shell.addView(body);
        dialog.show();
    }
    private TextView primary(String text){TextView t=center(text,16,WHITE,true);t.setBackground(gradient(0xFF23B5FC,0xFF1558F0,14));return t;}
    private EditText field(String hint,int lines){
        EditText e=new EditText(this);e.setTextSize(16);e.setTextColor(DARK);e.setHintTextColor(MUTED);e.setHint(hint);
        e.setBackground(shape(WHITE,14));pad(e,15,13,15,13);
        if(lines>1){e.setGravity(Gravity.TOP);e.setMinLines(lines);e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);}
        else {e.setSingleLine(true);e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);}
        return e;
    }
    private Spinner spinner(String[] items,int selected){
        Spinner s=new Spinner(this){
            @Override public boolean performClick(){
                themedChoices("Select an Option",items,null,null,index->setSelection(index));
                return true;
            }
        };
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,items){
            @Override public View getView(int position,View convertView,ViewGroup parent){
                TextView label=center(getItem(position)+"  ▾",14,BLUE,true);
                pad(label,6,10,6,10);
                return label;
            }
        };
        s.setAdapter(adapter);s.setSelection(Math.max(0,selected));return s;
    }
    private void editorRow(LinearLayout panel,String label,View value){
        LinearLayout line=horiz();TextView title=txt(label,14,DARK,true);line.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        line.addView(value,new LinearLayout.LayoutParams(d(155),-2));pad(line,0,7,0,7);panel.addView(line);
    }
    private void edit(NotesDb.Note note,boolean isTask){
        editorReturnTab=tab;
        editorOpen=true;
        NotesDb.Note n=note; n.task=isTask?1:0;
        selectedToneUri=n.toneUri==null?"":n.toneUri;
        selectedToneLabel=null;
        root=vert();root.setBackgroundColor(BG);
        LinearLayout bar=horiz();pad(bar,16,10,16,10);bar.setBackgroundColor(WHITE);
        View back=editorBackArrow(()->render(editorReturnTab));
        LinearLayout.LayoutParams backSize=lp(44,48);backSize.rightMargin=d(12);
        bar.addView(back,backSize);
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
        day.setOnClickListener(v->{alarm.setChecked(true);themedDatePicker(when,day);});
        time.setOnClickListener(v->{alarm.setChecked(true);themedTimePicker(when,time);});
        LinearLayout alarmOptions=vert();editorRow(alarmOptions,"Date",day);editorRow(alarmOptions,"Time",time);
        TextView before=txt("15 minutes before",14,BLUE,true);
        editorRow(alarmOptions,"Notification",before);
        selectedToneLabel=clickText(alarmToneName(selectedToneUri)+"   ›",14,BLUE,true,()->{});
        selectedToneLabel.setOnClickListener(v->{
            Intent picker=new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            picker.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_ALARM);
            picker.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,"Select reminder alarm tone");
            picker.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT,true);
            picker.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT,false);
            picker.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                selectedToneUri.isEmpty()?RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM):Uri.parse(selectedToneUri));
            try{startActivityForResult(picker,PICK_ALARM_TONE);}
            catch(Exception error){Toast.makeText(this,"Ringtone picker unavailable on this phone",Toast.LENGTH_LONG).show();}
        });
        editorRow(alarmOptions,"Alarm tone",selectedToneLabel);
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
            n.before=alarm.isChecked()?15:0;
            n.repeat=alarm.isChecked()?REPEAT[repeat.getSelectedItemPosition()]:"Never";
            n.toneUri=selectedToneUri;
            if(n.dueAt>0 && n.dueAt<=System.currentTimeMillis()){Toast.makeText(this,"Choose a future date and time",Toast.LENGTH_LONG).show();return;}
            n.id=db.save(n);AlarmHelper.schedule(this,n);
            if(n.dueAt>0 && Build.VERSION.SDK_INT>=31){
                AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
                if(!am.canScheduleExactAlarms())themedConfirm("Enable Precise Alarms",
                    "Your reminder is saved. For reliable on-time alerts, allow MH NoteMate to schedule exact alarms in Android settings.",
                    "Later","Open Settings",
                    ()->render(isTask?"Tasks":"Notes"),
                    ()->{exactAlarmSettings();render(isTask?"Tasks":"Notes");});
                else render(isTask?"Tasks":"Notes");
            }else render(isTask?"Tasks":"Notes");
            Toast.makeText(this,"Saved successfully",Toast.LENGTH_SHORT).show();
        };
        save.setOnClickListener(v->saveAction.run());saveBig.setOnClickListener(v->saveAction.run());
        root.addView(creditFooter(),lp(-1,32));
        handleModernInsets(root);setContentView(root);
    }
    private String alarmToneName(String uri) {
        if(uri==null || uri.isEmpty())return "Phone default";
        try{
            Ringtone r=RingtoneManager.getRingtone(this,Uri.parse(uri));
            if(r!=null)return r.getTitle(this);
        }catch(Exception ignored){}
        return "Selected tone";
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==PICK_ALARM_TONE && resultCode==RESULT_OK && data!=null) {
            Uri picked=data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            selectedToneUri=picked==null?"":picked.toString();
            if(selectedToneLabel!=null)selectedToneLabel.setText(alarmToneName(selectedToneUri)+"   ›");
        }
    }
    @Override public void onBackPressed(){handleBack();}
    @Override protected void onResume(){super.onResume();if(db!=null && !editorOpen && root!=null && !tab.isEmpty())AlarmHelper.rescheduleAll(this);}
    @Override protected void onDestroy(){if(db!=null)db.close();super.onDestroy();}
}