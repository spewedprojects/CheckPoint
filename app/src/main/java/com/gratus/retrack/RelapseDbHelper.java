package com.gratus.retrack;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class RelapseDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "checkpoint.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_RELAPSE = "relapse_history";

    public RelapseDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + TABLE_RELAPSE + " (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "streak_start_ts INTEGER NOT NULL," +
                        "streak_end_ts INTEGER NOT NULL," +
                        "streak_duration_ms INTEGER NOT NULL," +
                        "why_it_happened TEXT," +
                        "next_steps TEXT" +
                        ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // No migration yet
    }
}
