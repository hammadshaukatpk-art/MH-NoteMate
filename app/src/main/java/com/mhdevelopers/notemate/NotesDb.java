package com.mhdevelopers.notemate;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class NotesDb extends SQLiteOpenHelper {
    public static class Note {
        public long id, dueAt, updatedAt;
        public String title="", body="", category="Personal", repeat="Never", toneUri="";
        public int color=0, before=0, task=0, done=0, pinned=0;
    }
    public NotesDb(Context c) { super(c, "mh_notemate.db", null, 3); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, body TEXT NOT NULL, category TEXT NOT NULL, color INTEGER NOT NULL DEFAULT 0, due_at INTEGER NOT NULL DEFAULT 0, remind_before INTEGER NOT NULL DEFAULT 0, repeat_rule TEXT NOT NULL DEFAULT 'Never', is_task INTEGER NOT NULL DEFAULT 0, is_done INTEGER NOT NULL DEFAULT 0, pinned INTEGER NOT NULL DEFAULT 0, updated_at INTEGER NOT NULL, tone_uri TEXT NOT NULL DEFAULT '')");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        boolean legacy=false;
        try(Cursor c=db.rawQuery("PRAGMA table_info(notes)",null)) {
            while(c.moveToNext()) if("_id".equals(c.getString(1))) legacy=true;
        }
        if(legacy){
            db.execSQL("ALTER TABLE notes RENAME TO legacy_notes");
            onCreate(db);
            db.execSQL("INSERT INTO notes(title,body,category,color,due_at,remind_before,repeat_rule,is_task,is_done,pinned,updated_at) SELECT title,COALESCE(body,''),'Personal',2,COALESCE(due,0),0,'Never',COALESCE(kind,0),COALESCE(done,0),0,strftime('%s','now')*1000 FROM legacy_notes");
            db.execSQL("DROP TABLE legacy_notes");
        }
        if(oldVersion<3) db.execSQL("ALTER TABLE notes ADD COLUMN tone_uri TEXT NOT NULL DEFAULT ''");
    }
    private ContentValues values(Note n) {
        ContentValues v=new ContentValues();
        v.put("title",n.title);v.put("body",n.body);v.put("category",n.category);v.put("color",n.color);
        v.put("due_at",n.dueAt);v.put("remind_before",n.before);v.put("repeat_rule",n.repeat);v.put("tone_uri",n.toneUri);
        v.put("is_task",n.task);v.put("is_done",n.done);v.put("pinned",n.pinned);v.put("updated_at",System.currentTimeMillis());
        return v;
    }
    public long save(Note n) {
        if(n.id==0) return getWritableDatabase().insert("notes",null,values(n));
        getWritableDatabase().update("notes",values(n),"id=?",new String[]{String.valueOf(n.id)});
        return n.id;
    }
    public void delete(long id) { getWritableDatabase().delete("notes","id=?",new String[]{String.valueOf(id)}); }
    public Note get(long id) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT * FROM notes WHERE id=?",new String[]{String.valueOf(id)})) {
            return c.moveToFirst()?read(c):null;
        }
    }
    public List<Note> all() {
        List<Note> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT * FROM notes ORDER BY pinned DESC, updated_at DESC",null)) {
            while(c.moveToNext()) out.add(read(c));
        }
        return out;
    }
    private Note read(Cursor c) {
        Note n=new Note();n.id=c.getLong(c.getColumnIndexOrThrow("id"));
        n.title=c.getString(c.getColumnIndexOrThrow("title"));n.body=c.getString(c.getColumnIndexOrThrow("body"));
        n.category=c.getString(c.getColumnIndexOrThrow("category"));n.color=c.getInt(c.getColumnIndexOrThrow("color"));
        n.dueAt=c.getLong(c.getColumnIndexOrThrow("due_at"));n.before=c.getInt(c.getColumnIndexOrThrow("remind_before"));
        n.repeat=c.getString(c.getColumnIndexOrThrow("repeat_rule"));n.toneUri=c.getString(c.getColumnIndexOrThrow("tone_uri"));n.task=c.getInt(c.getColumnIndexOrThrow("is_task"));
        n.done=c.getInt(c.getColumnIndexOrThrow("is_done"));n.pinned=c.getInt(c.getColumnIndexOrThrow("pinned"));
        n.updatedAt=c.getLong(c.getColumnIndexOrThrow("updated_at"));return n;
    }
}
