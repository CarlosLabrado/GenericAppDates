package com.app_labs.genericappdates.fragments;


import android.content.DialogInterface;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.alamkanak.weekview.DateTimeInterpreter;
import com.alamkanak.weekview.WeekView;
import com.alamkanak.weekview.WeekViewEvent;
import com.app_labs.genericappdates.R;
import com.app_labs.genericappdates.custom.CalendarEvent;
import com.firebase.client.AuthData;
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

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * The calendar fragment
 */
public class CalendarFragment extends Fragment implements WeekView.MonthChangeListener, WeekView.EmptyViewClickListener, WeekView.EventClickListener {

    private static final String TAG = CalendarFragment.class.getSimpleName();

    private Firebase mRef;

    List<CalendarEvent> mEvents;
    List<WeekViewEvent> mEventsFake;

    private static final int TYPE_DAY_VIEW = 1;
    private static final int TYPE_THREE_DAY_VIEW = 2;
    private static final int TYPE_WEEK_VIEW = 3;
    private int mWeekViewType = TYPE_THREE_DAY_VIEW;

    private int preferredStartingHour = 8;

    private android.app.AlertDialog mEventDetailDialog = null;

    private int mInt = 1;

    @Bind(R.id.weekView)
    WeekView mWeekView;

    @OnClick(R.id.button)
    public void buttonClicked() {
        mWeekView.notifyDatasetChanged();
    }

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

        ButterKnife.bind(this, view);

        mWeekView = (WeekView) view.findViewById(R.id.weekView);

        assignListenersToWeekView();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        mEvents = new ArrayList<CalendarEvent>();
        mEventsFake = new ArrayList<WeekViewEvent>();

        mRef = new Firebase("https://blazing-inferno-2048.firebaseio.com/events");

        mRef.addAuthStateListener(new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {

                Log.e("authentication", "what happened?" + authData.toString());
            }
        });

        mRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                CalendarEvent newEvent = dataSnapshot.getValue(CalendarEvent.class);
                newEvent.setFirebaseKey(dataSnapshot.getKey());
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
                CalendarEvent eventToRemove = dataSnapshot.getValue(CalendarEvent.class);
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
                Log.e("firebaseError", firebaseError.getDetails());

            }
        });

    }

    public void assignListenersToWeekView() {
        // Show a toast message about the touched event.
        mWeekView.setOnEventClickListener(this);
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


    /**
     * This is the main thing that handles the drawing of the events, it gets called 3 times
     * to provide for smooth scrolling, we only want the events for the current month
     *
     * @param newYear
     * @param newMonth
     * @return the events
     */
    @Override
    public List<WeekViewEvent> onMonthChange(int newYear, int newMonth) {
        // Populate the week view with some mEvents.

        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);

        List<WeekViewEvent> returnList = new ArrayList<>();

        if (newMonth == currentMonth) {
            for (CalendarEvent calendarEvent : mEvents) {
                if (calendarEvent.getProviderId().equalsIgnoreCase("A")) {
                    returnList.add(calendarEvent);
                }
            }
            Log.i(TAG, "onMonthChange " + returnList.size() + returnList.toString());
            return returnList;
        } else {
            Log.i(TAG, "onMonthChange FAKE" + mEventsFake.toString());
            return mEventsFake;
        }
    }

    @Override
    public void onEmptyViewClicked(final Calendar calendar) {

        int startingMinute = calendar.get(Calendar.MINUTE);
        int roundedMinute = roundTo30(startingMinute);

        calendar.set(Calendar.MINUTE, roundedMinute);

        if (!eventIsInThePast(calendar)) { // not in the past
            if (!eventOverlaps(calendar)) {
                String eventTime = calendarToStringFormat(calendar);
                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.dialog_title))
                        .setMessage(getString(R.string.dialog_content) + "\n" + eventTime)
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.dialog_yes), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                eventWriter(calendar);
                            }
                        })
                        .setNegativeButton(getString(R.string.dialog_no), null)
                        .show();
            } else {
                Toast.makeText(getActivity(), R.string.toast_event_overlaps, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getActivity(), R.string.toast_event_in_the_past, Toast.LENGTH_SHORT).show();

        }
    }


    /**
     * we decided against saving the event on mEvents here and rather we wait for the firebase
     * onChildAdded call to save and display, so we just push here
     *
     * @param startTime
     */
    public void eventWriter(Calendar startTime) {

        Calendar endTime = (Calendar) startTime.clone();
        endTime.add(Calendar.MINUTE, 30); // events in this scenario are of 30 minutes

        String provider = mRef.getAuth().getProvider();
        String user = "";
        int color = getResources().getColor(R.color.event_color_03);

        if (provider.equalsIgnoreCase("password")) {
            user = (String) mRef.getAuth().getProviderData().get("email");
            color = getResources().getColor(R.color.event_color_01);
        } else if (provider.equalsIgnoreCase("facebook")) {
            user = (String) mRef.getAuth().getProviderData().get("displayName");
            color = getResources().getColor(R.color.event_color_02);
        }


        String author = mRef.getAuth().getUid();

        CalendarEvent event = new CalendarEvent(mInt, user + " " + getEventTitle(startTime), startTime, endTime, user, "A", author, null);
        mInt++;
        event.setColor(color);
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
     * This will only allow us to make events every half hour
     *
     * @param num minute selected
     * @return 0 or 30
     */
    private int roundTo30(int num) {
        if (num >= 0 && num < 30) {
            return 0;
        } else {
            return 30;
        }
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
    private boolean removeEvent(CalendarEvent eventToRemove) {

        for (CalendarEvent existingEvent : mEvents) {
            if (existingEvent.getStartTime().getTimeInMillis() == eventToRemove.getStartTime().getTimeInMillis()
                    && existingEvent.getEndTime().getTimeInMillis() == eventToRemove.getEndTime().getTimeInMillis()) {
                mEvents.remove(existingEvent);
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the event overlaps
     *
     * @param eventToVerify the event that we want to add
     * @return true if overlaps
     */
    private boolean eventOverlaps(Calendar eventToVerify) {
        int startingMinute = eventToVerify.get(Calendar.MINUTE);
        int roundedMinute = roundTo30(startingMinute);

        eventToVerify.set(Calendar.MINUTE, roundedMinute);

        Calendar endTimeTemp = (Calendar) eventToVerify.clone();
        endTimeTemp.add(Calendar.MINUTE, 30); // events in this scenario are of 30 minutes

        long globalStartTime = 0L;
        long globalEndTime = 0L;
        long newStartTime = eventToVerify.getTimeInMillis();
        long newEndTime = endTimeTemp.getTimeInMillis();
        for (CalendarEvent event : mEvents) {
            globalStartTime = event.getStartTime().getTimeInMillis();
            globalEndTime = event.getEndTime().getTimeInMillis();
            if ((newStartTime >= globalStartTime && newEndTime <= globalEndTime)
                    || (newEndTime > globalStartTime && newEndTime <= globalEndTime)
                    || (newStartTime >= globalStartTime && newStartTime < globalEndTime)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Don't let creation of events in the past
     *
     * @param eventToVerify
     * @return
     */
    private boolean eventIsInThePast(Calendar eventToVerify) {
        Calendar now = Calendar.getInstance();
        long nowLong = now.getTimeInMillis();
        long eventLong = eventToVerify.getTimeInMillis();
        if (eventLong < nowLong) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * Handles the clicks on existing events
     *
     * @param weekViewEvent the week view object
     * @param rectF         no idea
     */
    @Override
    public void onEventClick(WeekViewEvent weekViewEvent, RectF rectF) {
//        Toast.makeText(getActivity(), "Clicked " + weekViewEvent.getName(), Toast.LENGTH_SHORT).show();
        CalendarEvent calendarEvent = (CalendarEvent) weekViewEvent;

        String myUid = mRef.getAuth().getUid();
        String eventUid = calendarEvent.getAuthor();
        if (myUid.equalsIgnoreCase(eventUid)) { // if the author of the event is the same as the current logged user
            inflateEventDetailDialog(calendarEvent);

        } else {
            Toast.makeText(getActivity(), "you cant delete this because is not yours", Toast.LENGTH_SHORT).show();
        }

    }

    private void inflateEventDetailDialog(final CalendarEvent calendarEvent) {
        final CalendarEvent[] temporalEvent = new CalendarEvent[1];
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_event_detail, null);
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getActivity());
        builder.setView(view);

        builder.setTitle(R.string.dialog_buildt_in_title);

        mEventDetailDialog = builder.show();

        TextView textViewDetailDay = (TextView) view.findViewById(R.id.textView_detail_day);
        TextView textViewDetailProviderName = (TextView) view.findViewById(R.id.textView_detail_provider_name);


        textViewDetailDay.setText(calendarToStringFormat(calendarEvent.getStartTime()));
        textViewDetailProviderName.setText(calendarEvent.getProviderId());

        FloatingActionButton fabUpArrow = (FloatingActionButton) view.findViewById(R.id.fab_up_arrow);
        FloatingActionButton fabDownArrow = (FloatingActionButton) view.findViewById(R.id.fab_down_arrow);

        fabUpArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                temporalEvent[0] = subtract30Min(calendarEvent);
            }
        });

        Button buttonErase = (Button) view.findViewById(R.id.buttonErase);
        buttonErase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Firebase deleteRef = mRef.child(calendarEvent.getFirebaseKey());
                deleteRef.removeValue();
                mEventDetailDialog.dismiss();

                Snackbar
                        .make(mWeekView, getResources().getString(R.string.snack_date_erased), Snackbar.LENGTH_LONG)
                        .setAction(R.string.snack_date_undo, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mRef.push().setValue(calendarEvent);
                            }
                        })
                        .show();
            }
        });

//        mEventDetailDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
//            @Override
//            public void onDismiss(DialogInterface dialog) {
//
//            }
//        });


    }

    private CalendarEvent subtract30Min(CalendarEvent calendarEvent) {
        Calendar calendarStart = calendarEvent.getStartTime();
        Calendar calendarEnd = calendarEvent.getEndTime();

        calendarStart.add(Calendar.MINUTE, -30);
        calendarEnd.add(Calendar.MINUTE, -30);
        calendarEvent.setStartTime(calendarStart);
        calendarEvent.setEndTime(calendarEnd);

        return calendarEvent;
    }

    /**
     * formats the calendar to the way we want
     *
     * @param calendar calendar to format
     * @return formatted string
     */
    public String calendarToStringFormat(Calendar calendar) {

        return String.format("%02d:%02d del %s/%d",
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1);
    }
}
