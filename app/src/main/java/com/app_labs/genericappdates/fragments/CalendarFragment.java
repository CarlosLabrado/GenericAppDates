package com.app_labs.genericappdates.fragments;


import android.graphics.RectF;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.alamkanak.weekview.DateTimeInterpreter;
import com.alamkanak.weekview.WeekView;
import com.alamkanak.weekview.WeekViewEvent;
import com.app_labs.genericappdates.R;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * The calendar fragment
 */
public class CalendarFragment extends Fragment implements WeekView.MonthChangeListener, WeekView.EmptyViewClickListener {

    private Firebase mRef;

    List<WeekViewEvent> mEvents;
    List<WeekViewEvent> mEventsFake;

    private static final int TYPE_DAY_VIEW = 1;
    private static final int TYPE_THREE_DAY_VIEW = 2;
    private static final int TYPE_WEEK_VIEW = 3;
    private int mWeekViewType = TYPE_THREE_DAY_VIEW;

    private int preferredStartingHour = 8;

    @InjectView(R.id.weekView)
    WeekView mWeekView;

    public CalendarFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main, menu);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        ButterKnife.inject(view);

        mWeekView = (WeekView) view.findViewById(R.id.weekView);

        assignListenersToWeekView();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        mEvents = new ArrayList<WeekViewEvent>();
        mEventsFake = new ArrayList<WeekViewEvent>();

        mRef = new Firebase("https://blazing-inferno-2048.firebaseio.com/events");

        mRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                WeekViewEvent newEvent = dataSnapshot.getValue(WeekViewEvent.class);
                TimeZone timeZone = Calendar.getInstance().getTimeZone();
                newEvent.getStartTime().setTimeZone(timeZone);
                newEvent.getEndTime().setTimeZone(timeZone);
                if (!containsEvents(newEvent)) {
                    mEvents.add(newEvent);
                    mWeekView.notifyDatasetChanged();
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                WeekViewEvent eventToRemove = dataSnapshot.getValue(WeekViewEvent.class);
                TimeZone timeZone = Calendar.getInstance().getTimeZone();
                eventToRemove.getStartTime().setTimeZone(timeZone);
                eventToRemove.getEndTime().setTimeZone(timeZone);
                if (removeEvent(eventToRemove)) {
                    mWeekView.notifyDatasetChanged();
                }

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

    }

    public void assignListenersToWeekView() {
        // Show a toast message about the touched event.
        mWeekView.setOnEventClickListener(new WeekView.EventClickListener() {
            @Override
            public void onEventClick(WeekViewEvent weekViewEvent, RectF rectF) {
                Toast.makeText(getActivity(), "Clicked " + weekViewEvent.getName(), Toast.LENGTH_SHORT).show();
            }
        });
        // use this to add a new event
        mWeekView.setEmptyViewClickListener(this);

        // The week view has infinite scrolling horizontally. We have to provide the mEvents of a
        // month every time the month changes on the week view.
        mWeekView.setMonthChangeListener(this);

        // Set long press listener for mEvents.
//        mWeekView.setEventLongPressListener(new WeekView.EventLongPressListener() {
//            @Override
//            public void onEventLongPress(WeekViewEvent weekViewEvent, RectF rectF) {
//                Toast.makeText(getActivity(), "Long pressed event: " + weekViewEvent.getName(), Toast.LENGTH_SHORT).show();
//            }
//        });

        // Set up a date time interpreter to interpret how the date and time will be formatted in
        // the week view. This is optional.
        setupDateTimeInterpreter(false);

        mWeekView.goToHour(preferredStartingHour);
    }

    /**
     * Set up a date time interpreter which will show short date values when in week view and long
     * date values otherwise.
     *
     * @param shortDate True if the date values should be short.
     */
    private void setupDateTimeInterpreter(final boolean shortDate) {
        mWeekView.setDateTimeInterpreter(new DateTimeInterpreter() {
            @Override
            public String interpretDate(Calendar date) {
                SimpleDateFormat weekdayNameFormat = new SimpleDateFormat("EEE", Locale.getDefault());
                String weekday = weekdayNameFormat.format(date.getTime());
                SimpleDateFormat format = new SimpleDateFormat(" dd/MM", Locale.getDefault());

                // All android api level do not have a standard way of getting the first letter of
                // the week day name. Hence we get the first char programmatically.
                // Details: http://stackoverflow.com/questions/16959502/get-one-letter-abbreviation-of-week-day-of-a-date-in-java#answer-16959657
                if (shortDate)
                    weekday = String.valueOf(weekday.charAt(0));
                return weekday.toUpperCase() + format.format(date.getTime());
            }

            @Override
            public String interpretTime(int hour) {
                if (hour > 11) {
                    if (hour == 12) {
                        return "12 PM";
                    }
                    return ((hour - 12) + " PM");
                } else {
                    if (hour == 0) {
                        return "12 AM";
                    } else {
                        return (hour + " AM");
                    }
                }
            }
        });
    }


    private String getEventTitle(Calendar time) {
        return String.format("Event of %02d:%02d %s/%d", time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE), time.get(Calendar.MONTH) + 1, time.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        setupDateTimeInterpreter(id == R.id.action_week_view);
        switch (id) {
            case R.id.action_today:
                mWeekView.goToToday();
                setupDateTimeInterpreter(true);
                mWeekView.goToHour(preferredStartingHour);
                return true;
            case R.id.action_day_view:
                if (mWeekViewType != TYPE_DAY_VIEW) {
                    item.setChecked(!item.isChecked());
                    mWeekViewType = TYPE_DAY_VIEW;
                    mWeekView.setNumberOfVisibleDays(1);

                    // Lets change some dimensions to best fit the view.
                    mWeekView.setColumnGap((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
                    mWeekView.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
                    mWeekView.setEventTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
                    mWeekView.goToHour(preferredStartingHour);
                }
                return true;
            case R.id.action_three_day_view:
                if (mWeekViewType != TYPE_THREE_DAY_VIEW) {
                    item.setChecked(!item.isChecked());
                    mWeekViewType = TYPE_THREE_DAY_VIEW;
                    mWeekView.setNumberOfVisibleDays(3);

                    // Lets change some dimensions to best fit the view.
                    mWeekView.setColumnGap((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
                    mWeekView.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
                    mWeekView.setEventTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
                    mWeekView.goToHour(preferredStartingHour);
                }
                return true;
            case R.id.action_week_view:
                if (mWeekViewType != TYPE_WEEK_VIEW) {
                    item.setChecked(!item.isChecked());
                    mWeekViewType = TYPE_WEEK_VIEW;
                    mWeekView.setNumberOfVisibleDays(7);

                    // Lets change some dimensions to best fit the view.
                    mWeekView.setColumnGap((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics()));
                    mWeekView.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10, getResources().getDisplayMetrics()));
                    mWeekView.setEventTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10, getResources().getDisplayMetrics()));
                    mWeekView.goToHour(preferredStartingHour);
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public List<WeekViewEvent> onMonthChange(int newYear, int newMonth) {
        // Populate the week view with some mEvents.

        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);

        if (newMonth == currentMonth) {
            return mEvents;
        } else {
            return mEventsFake;
        }
    }

    @Override
    public void onEmptyViewClicked(Calendar calendar) {
        eventWriter(calendar);
        Toast.makeText(getActivity(), "Empty thing clicked", Toast.LENGTH_SHORT).show();
    }


    /**
     * we decided against saving the event on mEvents here and rather we wait for the firebase
     * onChildAdded call to save and display, so we just push here
     *
     * @param startTime
     */
    public void eventWriter(Calendar startTime) {

        int startingMinute = startTime.get(Calendar.MINUTE);
        int roundedMinute = round(startingMinute);

        startTime.set(Calendar.MINUTE, roundedMinute);

        Calendar endTime = (Calendar) startTime.clone();
        endTime.add(Calendar.HOUR, 1);

        WeekViewEvent event = new WeekViewEvent(1, getEventTitle(startTime), startTime, endTime);
        event.setColor(getResources().getColor(R.color.event_color_01));
        mRef.push().setValue(event);
    }

    /**
     * rounds to the nearest 10th
     *
     * @param num number to be rounded
     * @return rounded number
     */
    private int round(int num) {
        return (int) (Math.rint((double) num / 10) * 10);
    }

    /**
     * compares events to check if it is already on our list
     *
     * @param eventToAdd the new event
     * @return true if contains
     */
    private boolean containsEvents(WeekViewEvent eventToAdd) {

        for (WeekViewEvent existingEvent : mEvents) {
            if (existingEvent.getStartTime().getTimeInMillis() == eventToAdd.getStartTime().getTimeInMillis()
                    && existingEvent.getEndTime().getTimeInMillis() == eventToAdd.getEndTime().getTimeInMillis()) {
                return true;
            }
        }
        return false;
    }

    /**
     * tries to remove an event
     *
     * @param eventToRemove the event that is going to be removed
     * @return true if contains
     */
    private boolean removeEvent(WeekViewEvent eventToRemove) {

        for (WeekViewEvent existingEvent : mEvents) {
            if (existingEvent.getStartTime().getTimeInMillis() == eventToRemove.getStartTime().getTimeInMillis()
                    && existingEvent.getEndTime().getTimeInMillis() == eventToRemove.getEndTime().getTimeInMillis()) {
                mEvents.remove(existingEvent);
                return true;
            }
        }
        return false;
    }
}
