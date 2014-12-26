package com.benoitletondor.easybudget.view;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.benoitletondor.easybudget.R;
import com.benoitletondor.easybudget.model.db.DB;
import com.benoitletondor.easybudget.view.calendar.CalendarFragment;
import com.benoitletondor.easybudget.view.expenses.ExpensesRecyclerViewAdapter;
import com.melnykov.fab.FloatingActionButton;
import com.roomorama.caldroid.CaldroidFragment;
import com.roomorama.caldroid.CaldroidListener;

import java.util.Calendar;
import java.util.Date;

/**
 * Main activity containing Calendar and List of expenses
 *
 * @author Benoit LETONDOR
 */
public class MainActivity extends ActionBarActivity
{
    private CalendarFragment            calendarFragment;
    private RecyclerView                expensesRecyclerView;
    private LinearLayoutManager         expensesLayoutManager;
    private ExpensesRecyclerViewAdapter expensesViewAdapter;

    private DB db;

// ------------------------------------------>

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DB(getApplicationContext());
        initCalendarFragment();
        initRecyclerView();
    }

    @Override
    protected void onDestroy()
    {
        calendarFragment = null;
        expensesRecyclerView = null;
        expensesLayoutManager = null;
        expensesViewAdapter = null;

        db.close();
        db = null;

        super.onDestroy();
    }

// ------------------------------------------>

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

// ------------------------------------------>

    private void initCalendarFragment()
    {
        calendarFragment = new CalendarFragment();

        Bundle args = new Bundle();
        Calendar cal = Calendar.getInstance();
        args.putInt(CaldroidFragment.MONTH, cal.get(Calendar.MONTH) + 1);
        args.putInt(CaldroidFragment.YEAR, cal.get(Calendar.YEAR));
        args.putBoolean(CaldroidFragment.ENABLE_SWIPE, true);
        args.putBoolean(CaldroidFragment.SIX_WEEKS_IN_CALENDAR, false);
        args.putInt(CalendarFragment.START_DAY_OF_WEEK, CalendarFragment.MONDAY);

        calendarFragment.setArguments(args);
        calendarFragment.setSelectedDates(new Date(), new Date());

        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        t.replace(R.id.calendarView, calendarFragment);
        t.commit();

        final CaldroidListener listener = new CaldroidListener()
        {
            @Override
            public void onSelectDate(Date date, View view)
            {
                expensesViewAdapter = new ExpensesRecyclerViewAdapter(db, date);
                expensesRecyclerView.swapAdapter(expensesViewAdapter, true);

                calendarFragment.setSelectedDates(date, date);
                calendarFragment.refreshView();
            }

            @Override
            public void onChangeMonth(int month, int year)
            {

            }
        };

        calendarFragment.setCaldroidListener(listener);
    }

    private void initRecyclerView()
    {
        expensesRecyclerView = (RecyclerView) findViewById(R.id.expensesRecyclerView);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.attachToRecyclerView(expensesRecyclerView);

        expensesLayoutManager = new LinearLayoutManager(this);
        expensesRecyclerView.setLayoutManager(expensesLayoutManager);

        expensesViewAdapter = new ExpensesRecyclerViewAdapter(db, new Date());
        expensesRecyclerView.setAdapter(expensesViewAdapter);
    }
}