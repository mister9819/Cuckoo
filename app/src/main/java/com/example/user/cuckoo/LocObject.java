package com.example.user.cuckoo;

import android.graphics.Bitmap;

public class LocObject {
    private int id;
    private double lat, lon;
    private long timestamp, ntimestamp;
    private boolean sync, arrive;
    private String comment, title;
    private Bitmap image;

    public LocObject(String title, double lat, double lon, Bitmap image, boolean sync, boolean arrive) {
        this.lat = lat;
        this.lon = lon;
        this.sync = sync;
        id = 0;
        comment = " ";
        this.title = title;
        timestamp = System.currentTimeMillis();
        ntimestamp = 0;
        this.arrive = arrive;
        this.image = image;
    }

    public LocObject(int id, String title, double lat, double lon, String comment, long timestamp, long ntimestamp, boolean sync, boolean arrive) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
        this.sync = sync;
        this.comment = comment;
        this.timestamp = timestamp;
        this.title = title;
        this.ntimestamp = ntimestamp;
        this.arrive = arrive;
        this.image = null;
    }

    public LocObject(int id, String title, double lat, double lon, String comment, long timestamp, long ntimestamp, boolean sync, boolean arrive, Bitmap image) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
        this.sync = sync;
        this.comment = comment;
        this.timestamp = timestamp;
        this.title = title;
        this.ntimestamp = ntimestamp;
        this.arrive = arrive;
        this.image = image;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public boolean isSync() {
        return sync;
    }

    public String getComment() {
        return comment;
    }

    public int getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getNtimestamp() {
        return ntimestamp;
    }

    public String getTitle() {
        return title;
    }

    public Bitmap getImage() {
        return image;
    }

    public boolean isArrive() {
        return arrive;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setNtimestamp(long ntimestamp) {
        this.ntimestamp = ntimestamp;
    }

    public void setSync(boolean sync) {
        this.sync = sync;
    }
}
