package com.pelea.ui;

public class BookingInfo {
	public String deskId;
    public String email;
    public String name;
    public String photo;
    public String date;
    public String interval;

    public BookingInfo(String deskId, String email, String name, String photo, String date, String interval) {
        this.deskId = deskId;
        this.email = email;
        this.name = name;
        this.photo = photo;
        this.date = date;
        this.interval = interval;
    }
    
    public String getDeskId() { return deskId; }
    public String getDate() { return date; }
    public String getInterval() { return interval; }
}