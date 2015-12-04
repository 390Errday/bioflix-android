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
    private String hrArray;
    private String hrTimes;
    private String gsrArray;
    private String gsrTimes;

    public Session() {
        complete = false;
    }

    public Session(
            long id,
            String movieName,
            String viewerName,
            long startTime,
            long endTime,
            int complete,
            String hrArray,
            String hrTimes,
            String gsrArray,
            String gsrTimes) {

        this.id = id;
        this.movieName = movieName;
        this.viewerName = viewerName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.complete = (complete == 1) ? true : false;
        this.hrArray = hrArray;
        this.hrTimes = hrTimes;
        this.gsrArray = gsrArray;
        this.gsrTimes = gsrTimes;
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
    public String getHrArray() {
        return hrArray;
    }
    public String getHrTimes() {
        return hrTimes;
    }
    public String getGsrArray() {
        return gsrArray;
    }
    public String getGsrTimes() {
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

    public void setHrArray(String hrArray) {
        this.hrArray = hrArray;
    }

    public void setHrTimes(String hrTimes) {
        this.hrTimes = hrTimes;
    }

    public void setGsrArray(String gsrArray) {
        this.gsrArray = gsrArray;
    }

    public void setGsrTimes(String gsrTimes) {
        this.gsrTimes = gsrTimes;
    }
}
