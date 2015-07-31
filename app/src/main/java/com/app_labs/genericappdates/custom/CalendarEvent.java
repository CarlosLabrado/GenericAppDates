package com.app_labs.genericappdates.custom;

import com.alamkanak.weekview.WeekViewEvent;

import java.util.Calendar;

/**
 * We create our own event to put the properties that we need
 */
public class CalendarEvent extends WeekViewEvent {
    String userId;
    String providerId;

    public CalendarEvent() {
    }

    public CalendarEvent(long id, String name, int startYear, int startMonth, int startDay, int startHour, int startMinute, int endYear, int endMonth, int endDay, int endHour, int endMinute, String userId, String providerId) {
        super(id, name, startYear, startMonth, startDay, startHour, startMinute, endYear, endMonth, endDay, endHour, endMinute);
        this.userId = userId;
        this.providerId = providerId;
    }

    public CalendarEvent(long id, String name, Calendar startTime, Calendar endTime, String userId, String providerId) {
        super(id, name, startTime, endTime);
        this.userId = userId;
        this.providerId = providerId;
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
