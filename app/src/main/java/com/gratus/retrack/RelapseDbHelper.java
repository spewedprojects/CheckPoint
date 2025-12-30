package com.gratus.retrack;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class RelapseDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "retrack_data.db";
    private static final int DB_VERSION = 1;
    public static final String TABLE_RELAPSE = "relapse_history";

    // Columns
    private static final String COL_START = "streak_start_ts";
    private static final String COL_END = "streak_end_ts";
    private static final String COL_DURATION = "streak_duration_ms";
    private static final String COL_REASON = "why_it_happened";
    private static final String COL_STEPS = "next_steps";

    public RelapseDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + TABLE_RELAPSE + " (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        COL_START + " INTEGER NOT NULL," +
                        COL_END + " INTEGER NOT NULL," +
                        COL_DURATION + " INTEGER NOT NULL," +
                        COL_REASON + " TEXT," +
                        COL_STEPS + " TEXT" +
                        ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // No migration needed yet
    }

    // --- CRUD OPERATIONS ---

    public void addRelapse(long start, long end, String reason, String steps) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_START, start);
        values.put(COL_END, end);
        values.put(COL_DURATION, end - start);
        values.put(COL_REASON, reason);
        values.put(COL_STEPS, steps);
        db.insert(TABLE_RELAPSE, null, values);
        db.close();
    }

    public List<RelapseLog> getAllRelapses() {
        List<RelapseLog> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        // Order by newest first
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_RELAPSE + " ORDER BY " + COL_END + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                RelapseLog log = new RelapseLog(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_START)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_END)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_DURATION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_REASON)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_STEPS))
                );
                log.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                list.add(log);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public long getBestStreakDuration() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT MAX(" + COL_DURATION + ") FROM " + TABLE_RELAPSE, null);
        long max = 0;
        if (cursor.moveToFirst()) {
            max = cursor.getLong(0);
        }
        cursor.close();
        db.close();
        return max;
    }

    public boolean hasRecords() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + TABLE_RELAPSE + " LIMIT 1", null);
        boolean hasRecords = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return hasRecords;
    }

}