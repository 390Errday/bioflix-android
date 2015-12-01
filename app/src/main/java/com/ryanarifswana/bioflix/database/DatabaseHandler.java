package com.ryanarifswana.bioflix.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.ryanarifswana.bioflix.database.model.Session;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ariftopcu on 11/27/15.
 */
public class DatabaseHandler extends SQLiteOpenHelper{
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "biosessions";
    private static final String TABLE_SESSIONS = "sessions";

    private static final String KEY_ID = "id";
    private static final String KEY_MOVIE_NAME = "movie_name";
    private static final String KEY_START_TIME = "start_time";
    private static final String KEY_END_TIME = "end_time";
    private static final String KEY_VIEWER_NAME = "viewer_name";
    private static final String KEY_COMPLETE = "complete";
    private static final String KEY_HR_ARRAY = "hr_array";
    private static final String KEY_HR_TIMES = "hr_times";
    private static final String KEY_GSR_ARRAY = "gsr_array";
    private static final String KEY_GSR_TIMES = "gsr_times";

    //private static final String KEY_IMDB_ID = "imdb_id";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_SESSIONS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_MOVIE_NAME + " TEXT,"
                + KEY_START_TIME + " INTEGER," + KEY_END_TIME + " INTEGER,"
                + KEY_VIEWER_NAME + " TEXT," + KEY_COMPLETE + " INTEGER,"
                + KEY_HR_ARRAY + " TEXT," + KEY_HR_TIMES + " TEXT,"
                + KEY_GSR_ARRAY + " TEXT," + KEY_GSR_TIMES + " TEXT"
                + ")";
        db.execSQL(CREATE_CONTACTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSIONS);

        // Create tables again
        onCreate(db);
    }

    // Adds a new session, returns the id of the session
    public long newSession(String movieName, String viewerName, long startTime) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_MOVIE_NAME, movieName);
        values.put(KEY_VIEWER_NAME, viewerName);
        values.put(KEY_START_TIME, startTime);
        values.put(KEY_COMPLETE, 0);
        values.put(KEY_HR_ARRAY, "");
        values.put(KEY_HR_TIMES, "");
        values.put(KEY_GSR_ARRAY, "");
        values.put(KEY_GSR_TIMES, "");

        long id = db.insert(TABLE_SESSIONS, null, values);
        db.close();
        return id;
    }

    public void endSession(long id, long endTime) {
        SQLiteDatabase db = this.getWritableDatabase();

        String query = "SELECT *" + " FROM " + TABLE_SESSIONS + " WHERE " + KEY_ID + "=" + id;
        Cursor cursor = db.rawQuery(query, null);
        if(cursor.moveToFirst()) {
            ContentValues values = new ContentValues();
            values.put(KEY_END_TIME, endTime);
            values.put(KEY_COMPLETE, 1);
            db.update(TABLE_SESSIONS, values, KEY_ID + " = ?", new String[]{String.valueOf(id)});
        }
    }

    //Return complete sessions only
    public List<Session> getAllSessions() {
        List<Session> sessionList = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + TABLE_SESSIONS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        Log.d("Cursor count:", "" + cursor.getCount());
        if (cursor.moveToFirst()) {
            do {
                //only return complete sessions
                if(cursor.getInt(cursor.getColumnIndex(KEY_COMPLETE)) == 1) {
                    Session session = new Session();
                    session.setId(cursor.getLong(cursor.getColumnIndex(KEY_ID)));
                    session.setMovieName(cursor.getString(cursor.getColumnIndex(KEY_MOVIE_NAME)));
                    session.setStartTime(cursor.getInt(cursor.getColumnIndex(KEY_START_TIME)));
                    session.setEndTime(cursor.getInt(cursor.getColumnIndex(KEY_END_TIME)));
                    session.setViewerName(cursor.getString(cursor.getColumnIndex(KEY_VIEWER_NAME)));
                    session.setComplete(true);
                    sessionList.add(session);
                }
            } while (cursor.moveToNext());
        }
        return sessionList;
    }

    public int appenHR(long id, int[] hr, long[] hrTimes) {
        SQLiteDatabase db = this.getWritableDatabase();

        String query = "SELECT *" + " FROM " + TABLE_SESSIONS + " WHERE " + KEY_ID + "=" + id;
        Cursor cursor = db.rawQuery(query, null);
        String hrString = "";
        String hrTimesString = "";
        if(cursor.moveToFirst()) {
            hrString = cursor.getString(cursor.getColumnIndex(KEY_HR_ARRAY));
            hrTimesString = cursor.getString(cursor.getColumnIndex(KEY_HR_TIMES));
            for(int i = 0; i < hr.length; i++) {
                if(hr[i] > 0) {
                    hrString += hr[i] + ",";
                    hrTimesString += hrTimes[i] + ",";
                }
            }
        }

        ContentValues values = new ContentValues();
        values.put(KEY_HR_ARRAY, hrString);
        values.put(KEY_HR_TIMES, hrTimesString);

        return db.update(TABLE_SESSIONS, values, KEY_ID + " = ?", new String[] { String.valueOf(id) });
    }

    public int appenGsr(long id, int[] gsr, long[] gsrTimes) {
        SQLiteDatabase db = this.getWritableDatabase();

        String query = "SELECT *" + " FROM " + TABLE_SESSIONS + " WHERE " + KEY_ID + "=" + id;
        Cursor cursor = db.rawQuery(query, null);
        String gsrString = "";
        String gsrTimesString = "";
        if(cursor.moveToFirst()) {
            gsrString = cursor.getString(cursor.getColumnIndex(KEY_GSR_ARRAY));
            gsrTimesString = cursor.getString(cursor.getColumnIndex(KEY_GSR_TIMES));
            for(int i = 0; i < gsr.length; i++) {
                if(gsr[i] > 0) {
                    gsrString += gsr[i]+",";
                    gsrTimesString += gsrTimes[i]+",";
                }
            }
        }

        ContentValues values = new ContentValues();
        values.put(KEY_GSR_ARRAY, gsrString);
        values.put(KEY_GSR_TIMES, gsrTimesString);

        return db.update(TABLE_SESSIONS, values, KEY_ID + " = ?", new String[] { String.valueOf(id) });
    }
}
