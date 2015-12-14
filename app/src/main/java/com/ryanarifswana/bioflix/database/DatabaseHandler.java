package com.ryanarifswana.bioflix.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import com.ryanarifswana.bioflix.database.model.Session;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ariftopcu on 11/27/15.
 */
public class DatabaseHandler extends SQLiteOpenHelper{
    private static final int DATABASE_VERSION = 3;
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
    private static final String KEY_SKIN_TEMP_ARRAY = "temp_array";
    private static final String KEY_SKIN_TEMP_TIMES = "temp_times";

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
                + KEY_GSR_ARRAY + " TEXT," + KEY_GSR_TIMES + " TEXT,"
                + KEY_SKIN_TEMP_ARRAY + " TEXT," + KEY_SKIN_TEMP_TIMES + " TEXT"
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
        values.put(KEY_SKIN_TEMP_ARRAY, "");
        values.put(KEY_SKIN_TEMP_TIMES, "");

        long id = db.insert(TABLE_SESSIONS, null, values);
        db.close();
        return id;
    }

    public void endSession(long id, long endTime) {
        SQLiteDatabase db = this.getWritableDatabase();

        String query = "SELECT *" + " FROM " + TABLE_SESSIONS + " WHERE " + KEY_ID + "=" + id;
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            ContentValues values = new ContentValues();
            values.put(KEY_END_TIME, endTime);
            values.put(KEY_COMPLETE, 1);
            db.update(TABLE_SESSIONS, values, KEY_ID + " = ?", new String[]{String.valueOf(id)});
        }
        db.close();
    }

    //Return complete sessions only
    public List<Session> getAllSessions() {
        List<Session> sessionList = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + TABLE_SESSIONS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
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
        db.close();
        return sessionList;
    }

    public Session getSession(long id) {
        log("getSession() called");
        Session session = null;
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT *" + " FROM " + TABLE_SESSIONS + " WHERE " + KEY_ID + "=" + id;
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            session = new Session(
                    cursor.getLong(cursor.getColumnIndex(KEY_ID)),
                    cursor.getString(cursor.getColumnIndex(KEY_MOVIE_NAME)),
                    cursor.getString(cursor.getColumnIndex(KEY_VIEWER_NAME)),
                    cursor.getLong(cursor.getColumnIndex(KEY_START_TIME)),
                    cursor.getLong(cursor.getColumnIndex(KEY_END_TIME)),
                    cursor.getInt(cursor.getColumnIndex(KEY_COMPLETE)),
                    cursor.getString(cursor.getColumnIndex(KEY_HR_ARRAY)),
                    cursor.getString(cursor.getColumnIndex(KEY_HR_TIMES)),
                    cursor.getString(cursor.getColumnIndex(KEY_GSR_ARRAY)),
                    cursor.getString(cursor.getColumnIndex(KEY_GSR_TIMES)),
                    cursor.getString(cursor.getColumnIndex(KEY_SKIN_TEMP_ARRAY)),
                    cursor.getString(cursor.getColumnIndex(KEY_SKIN_TEMP_TIMES))
            );
        }
        db.close();
        return session;
    }

    public void concludeHr(long id, int[] hr, long[] hrTimes, int bufferLength) {
        log("concludeHr() called");
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT *" + " FROM " + TABLE_SESSIONS + " WHERE " + KEY_ID + "=" + id;
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            StringBuilder hrBuilder = new StringBuilder(cursor.getString(cursor.getColumnIndex(KEY_HR_ARRAY)));
            StringBuilder hrTimesBuilder = new StringBuilder(cursor.getString(cursor.getColumnIndex(KEY_HR_TIMES)));
            if(bufferLength == 0) { //if the buffer is empty, just remove the last comma
                hrBuilder.setLength(hrBuilder.length() - 1);
                hrTimesBuilder.setLength(hrTimesBuilder.length() - 1);
            } else {
                for (int i = 0; i < bufferLength; i++) {
                    if (hr[i] > 0) {
                        if (i == bufferLength - 1) {
                            hrBuilder.append(hr[i]);
                            hrTimesBuilder.append(hrTimes[i]);
                        } else {
                            hrBuilder.append(hr[i]).append(",");
                            hrTimesBuilder.append(hrTimes[i]).append(",");
                        }
                    }
                }
            }

            ContentValues values = new ContentValues();
            values.put(KEY_HR_ARRAY, hrBuilder.toString());
            values.put(KEY_HR_TIMES, hrTimesBuilder.toString());
            db.update(TABLE_SESSIONS, values, KEY_ID + " = ?", new String[]{String.valueOf(id)});
        }
        db.close();
    }

    public void concludeGsr(long id, int[] gsr, long[] gsrTimes, int bufferLength) {
        log("concludeGsr() called");
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT *" + " FROM " + TABLE_SESSIONS + " WHERE " + KEY_ID + "=" + id;
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            StringBuilder gsrBuilder = new StringBuilder(cursor.getString(cursor.getColumnIndex(KEY_GSR_ARRAY)));
            StringBuilder gsrTimesBuilder = new StringBuilder(cursor.getString(cursor.getColumnIndex(KEY_GSR_TIMES)));
            if(bufferLength == 0 && gsrBuilder.length() > 1) { //if the buffer is empty and if there's gsr data, just remove the last comma
                gsrBuilder.setLength(gsrBuilder.length() - 1);
                gsrTimesBuilder.setLength(gsrTimesBuilder.length() - 1);
            } else {
                for (int i = 0; i < bufferLength; i++) {
                    if (gsr[i] > 0) {
                        if (i == bufferLength - 1) {
                            gsrBuilder.append(gsr[i]);
                            gsrTimesBuilder.append(gsrTimes[i]);
                        } else {
                            gsrBuilder.append(gsr[i]).append(",");
                            gsrTimesBuilder.append(gsrTimes[i]).append(",");
                        }
                    }
                }
            }

            ContentValues values = new ContentValues();
            values.put(KEY_GSR_ARRAY, gsrBuilder.toString());
            values.put(KEY_GSR_TIMES, gsrTimesBuilder.toString());
            db.update(TABLE_SESSIONS, values, KEY_ID + " = ?", new String[]{String.valueOf(id)});
        }
        db.close();
    }

    public void concludeSkinTemp(long id, double[] temp, long[] tempTimes, int bufferLength) {
        log("concludeSkinTemp() called");
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT *" + " FROM " + TABLE_SESSIONS + " WHERE " + KEY_ID + "=" + id;
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            StringBuilder tempBuilder = new StringBuilder(cursor.getString(cursor.getColumnIndex(KEY_SKIN_TEMP_ARRAY)));
            StringBuilder tempTimesBuilder = new StringBuilder(cursor.getString(cursor.getColumnIndex(KEY_SKIN_TEMP_TIMES)));
            if(bufferLength == 0 && tempBuilder.length() > 1) { //if the buffer is empty, remove the last comma
                tempBuilder.setLength(tempBuilder.length() - 1);
                tempTimesBuilder.setLength(tempTimesBuilder.length() - 1);
            } else {
                for (int i = 0; i < bufferLength; i++) {
                    if (temp[i] > 0) {
                        if (i == bufferLength - 1) {
                            tempBuilder.append(temp[i]);
                            tempTimesBuilder.append(tempTimes[i]);
                        } else {
                            tempBuilder.append(temp[i]).append(",");
                            tempTimesBuilder.append(tempTimes[i]).append(",");
                        }
                    }
                }
            }

            ContentValues values = new ContentValues();
            values.put(KEY_SKIN_TEMP_ARRAY, tempBuilder.toString());
            values.put(KEY_SKIN_TEMP_TIMES, tempTimesBuilder.toString());
            db.update(TABLE_SESSIONS, values, KEY_ID + " = ?", new String[]{String.valueOf(id)});
        }
        db.close();
    }

    public void appendHR(long id, int[] hr, long[] hrTimes) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT *" + " FROM " + TABLE_SESSIONS + " WHERE " + KEY_ID + "=" + id;
        Cursor cursor = db.rawQuery(query, null);

        if(cursor.moveToFirst()) {
            StringBuilder hrBuilder = new StringBuilder(cursor.getString(cursor.getColumnIndex(KEY_HR_ARRAY)));
            StringBuilder hrTimesBuilder = new StringBuilder(cursor.getString(cursor.getColumnIndex(KEY_HR_TIMES)));
            for(int i = 0; i < hr.length; i++) {
                if(hr[i] > 0) {
                    hrBuilder.append(hr[i]).append(",");
                    hrTimesBuilder.append(hrTimes[i]).append(",");
                }
            }
            ContentValues values = new ContentValues();
            values.put(KEY_HR_ARRAY, hrBuilder.toString());
            values.put(KEY_HR_TIMES, hrTimesBuilder.toString());
            db.update(TABLE_SESSIONS, values, KEY_ID + " = ?", new String[]{String.valueOf(id)});
        }
        db.close();
    }

    public void appendGsr(long id, int[] gsr, long[] gsrTimes) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT *" + " FROM " + TABLE_SESSIONS + " WHERE " + KEY_ID + "=" + id;
        Cursor cursor = db.rawQuery(query, null);

        if(cursor.moveToFirst()) {
            StringBuilder gsrBuilder = new StringBuilder(cursor.getString(cursor.getColumnIndex(KEY_GSR_ARRAY)));
            StringBuilder gsrTimesBuilder = new StringBuilder(cursor.getString(cursor.getColumnIndex(KEY_GSR_TIMES)));
            for(int i = 0; i < gsr.length; i++) {
                if(gsr[i] > 0) {
                    gsrBuilder.append(gsr[i]).append(",");
                    gsrTimesBuilder.append(gsrTimes[i]).append(",");
                }
            }
            ContentValues values = new ContentValues();
            values.put(KEY_GSR_ARRAY, gsrBuilder.toString());
            values.put(KEY_GSR_TIMES, gsrTimesBuilder.toString());
            db.update(TABLE_SESSIONS, values, KEY_ID + " = ?", new String[]{String.valueOf(id)});
        }
        db.close();
    }

    public void appendSkinTemp(long id, double[] tempArray, long[] tempTimesArray) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT *" + " FROM " + TABLE_SESSIONS + " WHERE " + KEY_ID + "=" + id;
        Cursor cursor = db.rawQuery(query, null);

        if(cursor.moveToFirst()) {
            StringBuilder tempBuilder = new StringBuilder(cursor.getString(cursor.getColumnIndex(KEY_SKIN_TEMP_ARRAY)));
            StringBuilder tempTimesBuilder = new StringBuilder(cursor.getString(cursor.getColumnIndex(KEY_SKIN_TEMP_TIMES)));
            for(int i = 0; i < tempArray.length; i++) {
                if(tempArray[i] > 0) {
                    tempBuilder.append(tempArray[i]).append(",");
                    tempTimesBuilder.append(tempTimesArray[i]).append(",");
                }
            }
            ContentValues values = new ContentValues();
            values.put(KEY_SKIN_TEMP_ARRAY, tempBuilder.toString());
            values.put(KEY_SKIN_TEMP_TIMES, tempTimesBuilder.toString());
            db.update(TABLE_SESSIONS, values, KEY_ID + " = ?", new String[]{String.valueOf(id)});
        }
        db.close();
    }

    private void log(String s) {
        Log.d("DatabaseHandler", s);
    }
}
