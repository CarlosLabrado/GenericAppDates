package com.app_labs.genericappdates.custom;

import com.alamkanak.weekview.WeekViewEvent;

import java.util.Calendar;

/**
 * We create our own event to put the properties that we need
 */
public class CalendarEvent extends WeekViewEvent {
    String userId;
    String providerId;
    String author;

    public CalendarEvent() {
    }

    public CalendarEvent(String userId, String providerId, String author) {
        this.userId = userId;
        this.providerId = providerId;
        this.author = author;
    }

    public CalendarEvent(long id, String name, int startYear, int startMonth, int startDay, int startHour, int startMinute, int endYear, int endMonth, int endDay, int endHour, int endMinute, String userId, String providerId, String author) {
        super(id, name, startYear, startMonth, startDay, startHour, startMinute, endYear, endMonth, endDay, endHour, endMinute);
        this.userId = userId;
        this.providerId = providerId;
        this.author = author;
    }

    public CalendarEvent(long id, String name, Calendar startTime, Calendar endTime, String userId, String providerId, String author) {
        super(id, name, startTime, endTime);
        this.userId = userId;
        this.providerId = providerId;
        this.author = author;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
}
