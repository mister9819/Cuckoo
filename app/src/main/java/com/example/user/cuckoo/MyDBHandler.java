package com.example.user.cuckoo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MyDBHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "mydata.db";
    private static final String TABLE_LOCATIONS = "locations";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_LAT = "lat";
    private static final String COLUMN_LON = "lon";
    private static final String COLUMN_COMMENT = "comment";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_NTIMESTAMP = "ntimestamp";
    private static final String COLUMN_SYNC = "sync";
    private static final String COLUMN_ARRIVE = "arrive";
    private static final String COLUMN_IMAGE = "image";

    MyDBHandler(Context context, SQLiteDatabase.CursorFactory factory) {
        super(context, DATABASE_NAME, factory, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String query = "CREATE TABLE " + TABLE_LOCATIONS + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE + " TEXT NOT NULL, " +
                COLUMN_LAT + " TEXT NOT NULL, " +
                COLUMN_LON + " TEXT NOT NULL, " +
                COLUMN_COMMENT + " TEXT NOT NULL, " +
                COLUMN_TIMESTAMP + " TEXT NOT NULL, " +
                COLUMN_NTIMESTAMP + " TEXT NOT NULL, " +
                COLUMN_SYNC + " INTEGER DEFAULT 0, " +
                COLUMN_ARRIVE + " INTEGER DEFAULT 0, " +
                COLUMN_IMAGE + " BLOB " +
                ");";
        sqLiteDatabase.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS + ";");
        onCreate(sqLiteDatabase);
    }

    void addLocation(LocObject locObject) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, locObject.getTitle());
        values.put(COLUMN_LAT, locObject.getLat());
        values.put(COLUMN_LON, locObject.getLon());
        values.put(COLUMN_COMMENT, locObject.getComment());
        values.put(COLUMN_TIMESTAMP, locObject.getTimestamp());
        values.put(COLUMN_NTIMESTAMP, locObject.getNtimestamp());
        values.put(COLUMN_SYNC, locObject.isSync());
        values.put(COLUMN_ARRIVE, locObject.isArrive());
        values.put(COLUMN_IMAGE, getBytes(locObject.getImage()));

        SQLiteDatabase db = getWritableDatabase();
        db.insert(TABLE_LOCATIONS, null, values);
        db.close();
    }

    void deleteLocation(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_LOCATIONS + " WHERE " + COLUMN_ID + " = " + id + ";");
        db.close();
    }

    void syncLocation(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_LOCATIONS + " SET " + COLUMN_SYNC + " = 1 WHERE " + COLUMN_ID + " = " + id + ";");
        db.close();
    }

    void updateComment(int id, String comment) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_LOCATIONS + " SET " + COLUMN_COMMENT + " = \'" + comment + "\' WHERE " + COLUMN_ID + " = " + id + ";");
        db.close();
    }

    void updateAlarm(int id, long alarm) {
        SQLiteDatabase db = getWritableDatabase();
        String query = "UPDATE " + TABLE_LOCATIONS + " SET " + COLUMN_NTIMESTAMP + " = \'" + alarm + "\' WHERE " + COLUMN_ID + " = " + id + ";";
        //Log.e("HMM", query);
        db.execSQL(query);
        db.close();
    }

    List<LocObject> getLocationData() {
        List<LocObject> locas = new ArrayList<>();
        LocObject loca;
        String lat, lon, comment, title, timestamp, ntimestamp;
        int id;
        boolean sync, arrive;
        SQLiteDatabase db = getWritableDatabase();
        String query = "SELECT " + COLUMN_ID + ", " + COLUMN_LAT + ", " + COLUMN_LON + ", "
                        + COLUMN_TITLE + ", " + COLUMN_COMMENT + ", " + COLUMN_TIMESTAMP + ", "
                        + COLUMN_NTIMESTAMP + ", " + COLUMN_SYNC + ", " + COLUMN_ARRIVE
                        + " FROM " + TABLE_LOCATIONS + " ORDER BY " + COLUMN_ID + " DESC;";

        db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + TABLE_LOCATIONS + "';");
        Cursor c = db.rawQuery(query, null);
        for (c.moveToLast(); !c.isBeforeFirst(); c.moveToPrevious()) {
            title = c.getString(c.getColumnIndex(COLUMN_TITLE));
            lat = c.getString(c.getColumnIndex(COLUMN_LAT));
            lon = c.getString(c.getColumnIndex(COLUMN_LON));
            comment = c.getString(c.getColumnIndex(COLUMN_COMMENT));
            timestamp = c.getString(c.getColumnIndex(COLUMN_TIMESTAMP));
            ntimestamp = c.getString(c.getColumnIndex(COLUMN_NTIMESTAMP));
            id = c.getInt(c.getColumnIndex(COLUMN_ID));
            sync = (c.getInt(c.getColumnIndex(COLUMN_SYNC)) == 1);
            arrive = (c.getInt(c.getColumnIndex(COLUMN_ARRIVE)) == 1);
            loca = new LocObject(id, title, Double.valueOf(lat), Double.valueOf(lon), comment, Long.valueOf(timestamp), Long.valueOf(ntimestamp), sync, arrive);
            locas.add(0, loca);
        }
        db.close();
        c.close();
        return locas;
    }

    List<LocObject> getLocationDataWithImage() {
        List<LocObject> locas = new ArrayList<>();
        LocObject loca;
        String lat, lon, comment, title, timestamp, ntimestamp;
        int id;
        boolean sync, arrive;
        byte data[];
        SQLiteDatabase db = getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_LOCATIONS + " ORDER BY " + COLUMN_ID + " DESC;";

        db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + TABLE_LOCATIONS + "';");
        Cursor c = db.rawQuery(query, null);
        for (c.moveToLast(); !c.isBeforeFirst(); c.moveToPrevious()) {
            title = c.getString(c.getColumnIndex(COLUMN_TITLE));
            lat = c.getString(c.getColumnIndex(COLUMN_LAT));
            lon = c.getString(c.getColumnIndex(COLUMN_LON));
            comment = c.getString(c.getColumnIndex(COLUMN_COMMENT));
            timestamp = c.getString(c.getColumnIndex(COLUMN_TIMESTAMP));
            ntimestamp = c.getString(c.getColumnIndex(COLUMN_NTIMESTAMP));
            id = c.getInt(c.getColumnIndex(COLUMN_ID));
            sync = (c.getInt(c.getColumnIndex(COLUMN_SYNC)) == 1);
            arrive = (c.getInt(c.getColumnIndex(COLUMN_ARRIVE)) == 1);
            data = c.getBlob(c.getColumnIndex(COLUMN_IMAGE));
            Bitmap image = getImage(data);
            loca = new LocObject(id, title, Double.valueOf(lat), Double.valueOf(lon), comment, Long.valueOf(timestamp), Long.valueOf(ntimestamp), sync, arrive, image);
            locas.add(0, loca);
        }
        db.close();
        c.close();
        return locas;
    }

    // convert from bitmap to byte array
    public static byte[] getBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stream.toByteArray();
    }

    // convert from byte array to bitmap
    public static Bitmap getImage(byte[] image) {
        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }
}
