package com.ryanarifswana.bioflix.database.model;

/**
 * Created by ariftopcu on 11/27/15.
 */
public class Session {
    private long id;
    private String movieName;
    private String viewerName;
    private long startTime;
    private long endTime;
    private boolean complete;
    private int[] hrArray;
    private long[] hrTimes;
    private int[] gsrArray;
    private long[] gsrTimes;

    public Session() {
        complete = false;
    }

    public Session(String movieName, String viewerName, long startTime) {
        this.movieName = movieName;
        this.viewerName = viewerName;
        this.startTime = startTime;
        complete = false;
    }

    public long getId() {
        return id;
    }
    public String getMovieName() {
        return movieName;
    }
    public String getViewerName() {
        return viewerName;
    }
    public long getStartTime() {
        return startTime;
    }
    public long getEndTime() {
        return endTime;
    }
    public int[] getHrArray() {
        return hrArray;
    }
    public long[] getHrTimes() {
        return hrTimes;
    }
    public int[] getGsrArray() {
        return gsrArray;
    }
    public long[] getGsrTimes() {
        return gsrTimes;
    }

    public boolean isComplete(){
        return complete;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setMovieName(String movieName) {
        this.movieName = movieName;
    }

    public void setViewerName(String viewerName) {
        this.viewerName = viewerName;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public void setHrArray(int[] hrArray) {
        this.hrArray = hrArray;
    }

    public void setHrTimes(long[] hrTimes) {
        this.hrTimes = hrTimes;
    }

    public void setGsrArray(int[] gsrArray) {
        this.gsrArray = gsrArray;
    }

    public void setGsrTimes(long[] gsrTimes) {
        this.gsrTimes = gsrTimes;
    }
}
