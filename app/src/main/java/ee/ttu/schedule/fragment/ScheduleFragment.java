package ee.ttu.schedule.fragment;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.content.SyncStatusObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
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
import com.alamkanak.weekview.WeekViewLoader;
import com.vadimstrukov.ttuschedule.R;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;

import ee.ttu.schedule.drawable.DayOfMonthDrawable;
import ee.ttu.schedule.model.Event;
import ee.ttu.schedule.provider.BaseContract;
import ee.ttu.schedule.provider.EventContract;
import ee.ttu.schedule.utils.Constants;
import ee.ttu.schedule.utils.SyncUtils;

public class ScheduleFragment extends Fragment implements WeekViewLoader, WeekView.EventClickListener, LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener, SyncStatusObserver, DateTimeInterpreter {

    public static final int TYPE_DAY_VIEW = 1;
    public static final int TYPE_THREE_DAY_VIEW = 2;
    private static final String ARG_TYPE = "arg_type";
    private int WEEK_TYPE;

    private Map<Integer, List<WeekViewEvent>> eventMap = new HashMap<>();

    private SwipeRefreshLayout swipeRefreshLayout;
    private Object syncObserverHandle;
    private WeekView mWeekView;
    private String[] colorArray;

    private SyncUtils syncUtils;

    public ScheduleFragment() {

    }

    public static ScheduleFragment newInstance(int type) {
        Bundle args = new Bundle();
        ScheduleFragment scheduleFragment = new ScheduleFragment();
        args.putInt(ARG_TYPE, type);
        scheduleFragment.setArguments(args);
        return scheduleFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        syncUtils = new SyncUtils(getActivity());
        colorArray = getResources().getStringArray(R.array.colors);
        if (getArguments() != null)
            WEEK_TYPE = getArguments().getInt(ARG_TYPE, TYPE_THREE_DAY_VIEW);
        setHasOptionsMenu(true);
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_schedule, container, false);
        mWeekView = (WeekView) rootView.findViewById(R.id.weekView);
        swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setEnabled(false);
        mWeekView.setOnEventClickListener(this);
        mWeekView.setWeekViewLoader(this);
        mWeekView.setColumnGap((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
        mWeekView.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
        mWeekView.setEventTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
        mWeekView.setDateTimeInterpreter(this);
        mWeekView.goToHour(8);
        switch (WEEK_TYPE) {
            case TYPE_DAY_VIEW:
                mWeekView.setNumberOfVisibleDays(1);
                break;
            case TYPE_THREE_DAY_VIEW:
                mWeekView.setNumberOfVisibleDays(3);
                break;
        }
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        onStatusChanged(0);
        final int mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING | ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE;
        syncObserverHandle = ContentResolver.addStatusChangeListener(mask, this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (syncObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(syncObserverHandle);
            syncObserverHandle = null;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
        MenuItem menuItem = menu.findItem(R.id.action_today);
        setTodayIcon((LayerDrawable) menuItem.getIcon(), getActivity());
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_today:
                mWeekView.goToToday();
                mWeekView.goToHour(8);
                return true;
            case R.id.action_update:
                syncUtils.syncEvents(PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext()).getString(Constants.GROUP, null));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onEventClick(WeekViewEvent event, RectF eventRect) {
        final SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.getDefault());
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
        alertDialog.setTitle(event.getName());
        String dateStart = format.format(event.getStartTime().getTime());
        String dateEnd = format.format(event.getEndTime().getTime());
        alertDialog.setMessage(dateStart + "--" + dateEnd + "\n" + event.getDescription() + "\n" + event.getLocation());
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    private void setTodayIcon(LayerDrawable icon, Context context) {
        DayOfMonthDrawable today;
        // Reuse current drawable if possible
        Drawable currentDrawable = icon.findDrawableByLayerId(R.id.today_icon_day);
        if (currentDrawable != null && currentDrawable instanceof DayOfMonthDrawable) {
            today = (DayOfMonthDrawable) currentDrawable;
        } else {
            today = new DayOfMonthDrawable(context);
        }
        // Set the day and update the icon
        Calendar calendar = GregorianCalendar.getInstance(TimeZone.getDefault());
        today.setDayOfMonth(calendar.get(Calendar.DAY_OF_MONTH));
        icon.mutate();
        icon.setDrawableByLayerId(R.id.today_icon_day, today);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String orderBy = EventContract.EventColumns.KEY_DT_START + " ASC";
        return new CursorLoader(getActivity(), EventContract.Event.CONTENT_URI, null, null, null, orderBy);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        final Random random = new Random();
        List<WeekViewEvent> events = new ArrayList<>();
        int id = -1;
        data.moveToPosition(-1);
        while (data.moveToNext()) {
            final Calendar begin = new GregorianCalendar(TimeZone.getDefault());
            final Calendar end = new GregorianCalendar(TimeZone.getDefault());
            begin.setTimeInMillis(data.getLong(1));
            end.setTimeInMillis(data.getLong(2));
            if (id == -1)
                id = begin.get(Calendar.WEEK_OF_YEAR);
            if (begin.get(Calendar.WEEK_OF_YEAR) != id) {
                eventMap.put(id, events);
                events = new ArrayList<>();
            }
            final WeekViewEvent event = new WeekViewEvent(data.getLong(0), data.getString(5), data.getString(4), data.getString(3), begin, end);
            event.setColor(Color.parseColor(colorArray[random.nextInt(colorArray.length)]));
            events.add(event);
            id = begin.get(Calendar.WEEK_OF_YEAR);
            if(data.isLast())
                eventMap.put(id, events);
        }
        mWeekView.notifyDatasetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        eventMap.clear();
    }

    @Override
    public void onRefresh() {

    }

    @Override
    public void onStatusChanged(int which) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                NetworkInfo networkInfo = ((ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
                if (networkInfo == null) {
                    ContentResolver.cancelSync(syncUtils.getAccount(), BaseContract.CONTENT_AUTHORITY);
                    Toast.makeText(getActivity(), getActivity().getString(R.string.err_network), Toast.LENGTH_SHORT).show();
                }
                boolean syncActive = ContentResolver.isSyncActive(syncUtils.getAccount(), BaseContract.CONTENT_AUTHORITY);
                boolean syncPending = ContentResolver.isSyncPending(syncUtils.getAccount(), BaseContract.CONTENT_AUTHORITY);
                swipeRefreshLayout.setRefreshing(syncActive || syncPending);
            }
        });
    }

    @Override
    public double toWeekViewPeriodIndex(Calendar instance) {
        return instance.get(Calendar.WEEK_OF_YEAR);
    }

    @Override
    public List<WeekViewEvent> onLoad(int periodIndex) {
        if (!eventMap.containsKey(periodIndex)) {
            return new ArrayList<>();
        }
        return eventMap.get(periodIndex);
    }

    @Override
    public String interpretDate(Calendar date) {
        final SimpleDateFormat format = new SimpleDateFormat("EEE M/d", Locale.getDefault());
        return format.format(date.getTime());
    }

    @Override
    public String interpretTime(int hour) {
        return hour + ":00";
    }
}
