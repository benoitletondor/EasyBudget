package com.benoitletondor.easybudget.view;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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

            @Override
            public void onCaldroidViewCreated()
            {
                Button leftButton = calendarFragment.getLeftArrowButton();
                Button rightButton = calendarFragment.getRightArrowButton();
                TextView textView = calendarFragment.getMonthTitleTextView();

                textView.setTextColor(MainActivity.this.getResources().getColor(R.color.primary_text));

                leftButton.setText("<");
                leftButton.setTextSize(25);
                leftButton.setGravity(Gravity.CENTER);
                leftButton.setTextColor(MainActivity.this.getResources().getColor(R.color.primary_light));
                leftButton.setBackgroundResource(R.drawable.calendar_month_switcher_button_drawable);
                
                rightButton.setText(">");
                rightButton.setTextSize(25);
                rightButton.setGravity(Gravity.CENTER);
                rightButton.setTextColor(MainActivity.this.getResources().getColor(R.color.primary_light));
                rightButton.setBackgroundResource(R.drawable.calendar_month_switcher_button_drawable);

                // Remove border on lollipop
                if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP )
                {
                    leftButton.setOutlineProvider(null);
                    rightButton.setOutlineProvider(null);
                }

                calendarFragment.refreshView();
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