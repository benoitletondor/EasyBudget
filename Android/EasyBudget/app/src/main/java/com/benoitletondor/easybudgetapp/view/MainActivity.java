package com.benoitletondor.easybudgetapp.view;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.benoitletondor.easybudgetapp.EasyBudget;
import com.benoitletondor.easybudgetapp.R;
import com.benoitletondor.easybudgetapp.helper.Logger;
import com.benoitletondor.easybudgetapp.helper.UIHelper;
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper;
import com.benoitletondor.easybudgetapp.helper.ParameterKeys;
import com.benoitletondor.easybudgetapp.helper.Parameters;
import com.benoitletondor.easybudgetapp.model.Expense;
import com.benoitletondor.easybudgetapp.model.MonthlyExpense;
import com.benoitletondor.easybudgetapp.model.MonthlyExpenseDeleteType;
import com.benoitletondor.easybudgetapp.model.db.DBCache;
import com.benoitletondor.easybudgetapp.view.main.calendar.CalendarFragment;
import com.benoitletondor.easybudgetapp.view.main.ExpensesRecyclerViewAdapter;
import com.benoitletondor.easybudgetapp.view.selectcurrency.SelectCurrencyFragment;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.roomorama.caldroid.CaldroidFragment;
import com.roomorama.caldroid.CaldroidListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Main activity containing Calendar and List of expenses
 *
 * @author Benoit LETONDOR
 */
public class MainActivity extends DBActivity
{
    /**
     * Snackbar with actions must be shown 5s
     */
    private static final int ACTION_SNACKBAR_LENGTH = 5000;

    public static final int ADD_EXPENSE_ACTIVITY_CODE = 101;
    public static final int MANAGE_MONTHLY_EXPENSE_ACTIVITY_CODE = 102;
    public static final int WELCOME_SCREEN_ACTIVITY_CODE = 103;
    public static final String INTENT_EXPENSE_DELETED = "intent.expense.deleted";
    public static final String INTENT_MONTHLY_EXPENSE_DELETED = "intent.expense.monthly.deleted";
    public static final String INTENT_SHOW_WELCOME_SCREEN = "intent.welcomscreen.show";

    public final static String ANIMATE_TRANSITION_KEY = "animate";
    public final static String CENTER_X_KEY           = "centerX";
    public final static String CENTER_Y_KEY           = "centerY";

    private static final String CALENDAR_SAVED_STATE = "calendar_saved_state";
    private static final String RECYCLE_VIEW_SAVED_DATE = "recycleViewSavedDate";

    private BroadcastReceiver receiver;

    private CalendarFragment            calendarFragment;
    private ExpensesRecyclerViewAdapter expensesViewAdapter;
    private CoordinatorLayout           coordinatorLayout;

    private TextView budgetLine;
    @Nullable
    private Date lastStopDate;

// ------------------------------------------>

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        // Launch welcome screen if needed
        if( Parameters.getInstance(this).getInt(ParameterKeys.ONBOARDING_STEP, -1) != WelcomeActivity.STEP_COMPLETED )
        {
            Intent startIntent = new Intent(this, WelcomeActivity.class);
            ActivityCompat.startActivityForResult(this, startIntent, WELCOME_SCREEN_ACTIVITY_CODE, null);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        budgetLine = (TextView) findViewById(R.id.budgetLine);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);

        initCalendarFragment(savedInstanceState);
        initRecyclerView(savedInstanceState);

        // Register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_EXPENSE_DELETED);
        filter.addAction(INTENT_MONTHLY_EXPENSE_DELETED);
        filter.addAction(SelectCurrencyFragment.CURRENCY_SELECTED_INTENT);
        filter.addAction(INTENT_SHOW_WELCOME_SCREEN);

        receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if( INTENT_EXPENSE_DELETED.equals(intent.getAction()) )
                {
                    final Expense expense = (Expense) intent.getSerializableExtra("expense");

                    if( db.deleteExpense(expense) )
                    {
                        final int position = expensesViewAdapter.removeExpense(expense);
                        updateBalanceDisplayForDay(expensesViewAdapter.getDate());
                        calendarFragment.refreshView();

                        Snackbar snackbar = Snackbar.make(coordinatorLayout, expense.isRevenue() ? R.string.income_delete_snackbar_text : R.string.expense_delete_snackbar_text, Snackbar.LENGTH_LONG);
                        snackbar.setAction(R.string.undo, new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View v)
                            {
                                db.persistExpense(expense, true);

                                if( calendarFragment.getSelectedDate().equals(expense.getDate()) )
                                {
                                    expensesViewAdapter.addExpense(expense, position);
                                }

                                updateBalanceDisplayForDay(calendarFragment.getSelectedDate());
                                calendarFragment.refreshView();
                            }
                        });
                        snackbar.setActionTextColor(ContextCompat.getColor(MainActivity.this, R.color.snackbar_action_undo));
                        //noinspection ResourceType
                        snackbar.setDuration(ACTION_SNACKBAR_LENGTH);
                        snackbar.show();
                    }
                    else
                    {
                        // TODO warn user of error
                    }

                }
                else if( INTENT_MONTHLY_EXPENSE_DELETED.equals(intent.getAction()) )
                {
                    final Expense expense = (Expense) intent.getSerializableExtra("expense");
                    final MonthlyExpenseDeleteType deleteType = MonthlyExpenseDeleteType.fromValue(intent.getIntExtra("deleteType", MonthlyExpenseDeleteType.ALL.getValue()));
                    final MonthlyExpense monthlyExpense = db.findMonthlyExpenseForId(expense.getMonthlyId());

                    if( deleteType == null )
                    {
                        showGenericMonthlyDeleteErrorDialog();
                        Logger.error("INTENT_MONTHLY_EXPENSE_DELETED came with null delete type");

                        return;
                    }

                    if( monthlyExpense == null )
                    {
                        showGenericMonthlyDeleteErrorDialog();
                        Logger.error("INTENT_MONTHLY_EXPENSE_DELETED: Unable to retrieve monthly expense");

                        return;
                    }

                    // Check that if the user wants to delete series before this one, there are actually series to delete
                    if( deleteType == MonthlyExpenseDeleteType.TO && !db.hasExpensesForMonthlyExpenseBeforeDate(monthlyExpense, expense.getDate()) )
                    {
                        new AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.monthly_expense_delete_first_error_title)
                            .setMessage(getResources().getString(R.string.monthly_expense_delete_first_error_message))
                            .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    dialog.dismiss();
                                }
                            })
                            .show();

                        return;
                    }

                    new DeleteMonthlyExpenseTask(monthlyExpense, expense, deleteType).execute();
                }
                else if( SelectCurrencyFragment.CURRENCY_SELECTED_INTENT.equals(intent.getAction()) )
                {
                    refreshAllForDate(expensesViewAdapter.getDate());
                }
                else if( INTENT_SHOW_WELCOME_SCREEN.equals(intent.getAction()) )
                {
                    Intent startIntent = new Intent(MainActivity.this, WelcomeActivity.class);
                    ActivityCompat.startActivityForResult(MainActivity.this, startIntent, WELCOME_SCREEN_ACTIVITY_CODE, null);
                }
            }
        };

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, filter);
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        // If the last stop happened yesterday (or another day), set and refresh to the current date
        if( lastStopDate != null )
        {
            Calendar cal = Calendar.getInstance();
            int currentDay = cal.get(Calendar.DAY_OF_YEAR);

            cal.setTime(lastStopDate);
            int lastStopDay = cal.get(Calendar.DAY_OF_YEAR);

            if( currentDay != lastStopDay )
            {
                refreshAllForDate(new Date());
            }

            lastStopDate = null;
        }
    }

    @Override
    protected void onStop()
    {
        lastStopDate = new Date();

        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(receiver);

        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        if (calendarFragment != null)
        {
            calendarFragment.saveStatesToKey(outState, CALENDAR_SAVED_STATE);
        }

        if( expensesViewAdapter != null  )
        {
            outState.putSerializable(RECYCLE_VIEW_SAVED_DATE, expensesViewAdapter.getDate());
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if( requestCode == ADD_EXPENSE_ACTIVITY_CODE || requestCode == MANAGE_MONTHLY_EXPENSE_ACTIVITY_CODE )
        {
            if( resultCode == RESULT_OK )
            {
                refreshAllForDate(calendarFragment.getSelectedDate());
            }
        }
        else if( requestCode == WELCOME_SCREEN_ACTIVITY_CODE )
        {
            if( resultCode == RESULT_OK )
            {
                refreshAllForDate(calendarFragment.getSelectedDate());
            }
            else if( resultCode == RESULT_CANCELED )
            {
                finish(); // Finish activity if welcome screen is finish via back button
            }
        }
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
        if ( id == R.id.action_settings)
        {
            Intent startIntent = new Intent(this, SettingsActivity.class);
            ActivityCompat.startActivity(MainActivity.this, startIntent, null);

            return true;
        }
        else if( id == R.id.action_balance )
        {
            final int currentBalance = -db.getBalanceForDay(new Date());

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_adjust_balance, null);
            final EditText amountEditText = (EditText) dialogView.findViewById(R.id.balance_amount);
            amountEditText.setText(String.valueOf(currentBalance));
            amountEditText.setSelection(amountEditText.getText().length()); // Put focus at the end of the text

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.adjust_balance_title);
            builder.setMessage(R.string.adjust_balance_message);
            builder.setView(dialogView);
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            });
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    // Ajust balance
                    int newBalance = Integer.valueOf(amountEditText.getText().toString());

                    if( newBalance == currentBalance )
                    {
                        // Nothing to do, balance hasn't change
                        return;
                    }

                    final int diff = newBalance - currentBalance;

                    String balanceExpenseTitle = getResources().getString(R.string.adjust_balance_expense_title);

                    // Look for an existing balance for the day
                    Expense expense = null;
                    List<Expense> expensesForDay = db.getExpensesForDay(new Date());
                    for(Expense expenseOfDay : expensesForDay)
                    {
                        if( expenseOfDay.getTitle().equals(balanceExpenseTitle) )
                        {
                            expense = expenseOfDay;
                            break;
                        }
                    }

                    View.OnClickListener listener;

                    // If the adjust balance exists, just add the diff and persist it
                    if( expense != null )
                    {
                        final Expense persistedExpense = expense;

                        persistedExpense.setAmount(persistedExpense.getAmount() - diff);
                        db.persistExpense(persistedExpense);

                        // On cancel, remove the diff and persist
                        listener = new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View v)
                            {
                                persistedExpense.setAmount(persistedExpense.getAmount() + diff);
                                db.persistExpense(persistedExpense);

                                refreshAllForDate(expensesViewAdapter.getDate());
                            }
                        };
                    }
                    else // If no adjust balance yet, create a new one
                    {
                        final Expense persistedExpense = new Expense(getResources().getString(R.string.adjust_balance_expense_title), -diff, new Date());
                        db.persistExpense(persistedExpense);

                        // On cancel, just delete the inserted balance
                        listener = new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View v)
                            {
                                db.deleteExpense(persistedExpense);

                                refreshAllForDate(expensesViewAdapter.getDate());
                            }
                        };
                    }

                    refreshAllForDate(expensesViewAdapter.getDate());
                    dialog.dismiss();

                    //Show snackbar
                    Snackbar snackbar = Snackbar.make(coordinatorLayout, getResources().getString(R.string.adjust_balance_snackbar_text, CurrencyHelper.getFormattedCurrencyString(MainActivity.this, newBalance)), Snackbar.LENGTH_LONG);
                    snackbar.setAction(R.string.undo, listener);
                    snackbar.setActionTextColor(ContextCompat.getColor(MainActivity.this, R.color.snackbar_action_undo));
                    //noinspection ResourceType
                    snackbar.setDuration(ACTION_SNACKBAR_LENGTH);
                    snackbar.show();
                }
            });

            final Dialog dialog = builder.show();

            // Directly show keyboard when the dialog pops
            amountEditText.setOnFocusChangeListener(new View.OnFocusChangeListener()
            {
                @Override
                public void onFocusChange(View v, boolean hasFocus)
                {
                    if (hasFocus && getResources().getConfiguration().keyboard == Configuration.KEYBOARD_NOKEYS ) // Check if the device doesn't have a physical keyboard
                    {
                        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    }
                }
            });

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

// ------------------------------------------>

    /**
     * Update the balance for the given day
     * FIXME optim
     *
     * @param day
     */
    private void updateBalanceDisplayForDay(@NonNull Date day)
    {
        int balance = - db.getBalanceForDay(day);

        SimpleDateFormat format = new SimpleDateFormat(getResources().getString(R.string.account_balance_date_format), Locale.getDefault());
        budgetLine.setText(getResources().getString(R.string.account_balance_format, format.format(day), CurrencyHelper.getFormattedCurrencyString(this, balance)));

        if( balance <= 0 )
        {
            budgetLine.setBackgroundResource(R.color.budget_red);
        }
        else if( balance < Parameters.getInstance(getApplicationContext()).getInt(ParameterKeys.LOW_MONEY_WARNING_AMOUNT, EasyBudget.DEFAULT_LOW_MONEY_WARNING_AMOUNT) )
        {
            budgetLine.setBackgroundResource(R.color.budget_orange);
        }
        else
        {
            budgetLine.setBackgroundResource(R.color.budget_green);
        }
    }

// ------------------------------------------>

    private void initCalendarFragment(Bundle savedInstanceState)
    {
        calendarFragment = new CalendarFragment();

        if( savedInstanceState != null && savedInstanceState.containsKey(CALENDAR_SAVED_STATE) && savedInstanceState.containsKey(RECYCLE_VIEW_SAVED_DATE))
        {
            calendarFragment.restoreStatesFromKey(savedInstanceState, CALENDAR_SAVED_STATE);

            Date selectedDate = (Date) savedInstanceState.getSerializable(RECYCLE_VIEW_SAVED_DATE);
            calendarFragment.setSelectedDates(selectedDate, selectedDate);
            lastStopDate = selectedDate; // Set last stop date that will be check on next onStart call
        }
        else
        {
            Bundle args = new Bundle();
            Calendar cal = Calendar.getInstance();
            args.putInt(CaldroidFragment.MONTH, cal.get(Calendar.MONTH) + 1);
            args.putInt(CaldroidFragment.YEAR, cal.get(Calendar.YEAR));
            args.putBoolean(CaldroidFragment.ENABLE_SWIPE, true);
            args.putBoolean(CaldroidFragment.SIX_WEEKS_IN_CALENDAR, false);
            args.putInt(CalendarFragment.START_DAY_OF_WEEK, CalendarFragment.MONDAY);
            args.putBoolean(CalendarFragment.ENABLE_CLICK_ON_DISABLED_DATES, false);
            args.putInt(CaldroidFragment.THEME_RESOURCE, R.style.caldroid_style);

            calendarFragment.setArguments(args);
            calendarFragment.setSelectedDates(new Date(), new Date());

            Date minDate = new Date(Parameters.getInstance(this).getLong(ParameterKeys.INIT_DATE, new Date().getTime()));
            calendarFragment.setMinDate(minDate);
        }

        final CaldroidListener listener = new CaldroidListener()
        {
            @Override
            public void onSelectDate(Date date, View view)
            {
                refreshAllForDate(date);
            }

            @Override
            public void onLongClickDate(Date date, View view) // Add expense on long press
            {
                Intent startIntent = new Intent(MainActivity.this, ExpenseEditActivity.class);
                startIntent.putExtra("date", date);

                if( UIHelper.areAnimationsEnabled(MainActivity.this) )
                {
                    // Get the absolute location on window for Y value
                    int viewLocation[] = new int[2];
                    view.getLocationInWindow(viewLocation);

                    startIntent.putExtra(ANIMATE_TRANSITION_KEY, true);
                    startIntent.putExtra(CENTER_X_KEY, (int) view.getX() + view.getWidth() / 2);
                    startIntent.putExtra(CENTER_Y_KEY, viewLocation[1] + view.getHeight() / 2);
                }

                ActivityCompat.startActivityForResult(MainActivity.this, startIntent, ADD_EXPENSE_ACTIVITY_CODE, null);
            }

            @Override
            public void onChangeMonth(int month, int year)
            {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.MONTH, month);
                cal.set(Calendar.YEAR, year);

                DBCache.getInstance(MainActivity.this).loadMonth(cal.getTime());
            }

            @Override
            public void onCaldroidViewCreated()
            {
                Button leftButton = calendarFragment.getLeftArrowButton();
                Button rightButton = calendarFragment.getRightArrowButton();
                TextView textView = calendarFragment.getMonthTitleTextView();
                GridView weekDayGreedView = calendarFragment.getWeekdayGridView();
                LinearLayout topLayout = (LinearLayout) MainActivity.this.findViewById(com.caldroid.R.id.calendar_title_view);

                LinearLayout.LayoutParams  params = (LinearLayout.LayoutParams)textView.getLayoutParams();
                params.gravity = Gravity.TOP;
                params.setMargins(0, 0, 0, MainActivity.this.getResources().getDimensionPixelSize(R.dimen.calendar_month_text_padding_bottom));
                textView.setLayoutParams(params);

                topLayout.setPadding(0, MainActivity.this.getResources().getDimensionPixelSize(R.dimen.calendar_month_padding_top), 0, MainActivity.this.getResources().getDimensionPixelSize(R.dimen.calendar_month_padding_bottom));

                LinearLayout.LayoutParams leftButtonParams = (LinearLayout.LayoutParams) leftButton.getLayoutParams();
                leftButtonParams.setMargins(MainActivity.this.getResources().getDimensionPixelSize(R.dimen.calendar_month_buttons_margin), 0, 0, 0);
                leftButton.setLayoutParams(leftButtonParams);

                LinearLayout.LayoutParams rightButtonParams = (LinearLayout.LayoutParams) rightButton.getLayoutParams();
                rightButtonParams.setMargins(0, 0, MainActivity.this.getResources().getDimensionPixelSize(R.dimen.calendar_month_buttons_margin), 0);
                rightButton.setLayoutParams(rightButtonParams);

                textView.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.white));
                topLayout.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.primary_dark));

                leftButton.setText("<");
                leftButton.setTextSize(25);
                leftButton.setGravity(Gravity.CENTER);
                leftButton.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.white));
                leftButton.setBackgroundResource(R.drawable.calendar_month_switcher_button_drawable);

                rightButton.setText(">");
                rightButton.setTextSize(25);
                rightButton.setGravity(Gravity.CENTER);
                rightButton.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.white));
                rightButton.setBackgroundResource(R.drawable.calendar_month_switcher_button_drawable);

                weekDayGreedView.setPadding(0, MainActivity.this.getResources().getDimensionPixelSize(R.dimen.calendar_weekdays_padding_top), 0, MainActivity.this.getResources().getDimensionPixelSize(R.dimen.calendar_weekdays_padding_bottom));

                // Remove border on lollipop
                UIHelper.removeButtonBorder(leftButton);
                UIHelper.removeButtonBorder(rightButton);
            }
        };

        calendarFragment.setCaldroidListener(listener);

        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        t.replace(R.id.calendarView, calendarFragment);
        t.commit();
    }

    private void initRecyclerView(Bundle savedInstanceState)
    {
        /*
         * FAB
         */
        final FloatingActionsMenu menu = (FloatingActionsMenu) findViewById(R.id.fab_choices);

        final View background = MainActivity.this.findViewById(R.id.fab_choices_background);
        final float backgroundAlpha = 0.8f;
        final long backgroundAnimationDuration = 200;

        background.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                menu.collapse();
            }
        });

        menu.setOnFloatingActionsMenuUpdateListener(new FloatingActionsMenu.OnFloatingActionsMenuUpdateListener()
        {
            @Override
            public void onMenuExpanded()
            {
                AlphaAnimation fadeInAnimation = new AlphaAnimation(0.0f, backgroundAlpha);
                fadeInAnimation.setDuration(backgroundAnimationDuration);
                fadeInAnimation.setFillAfter(true);
                fadeInAnimation.setAnimationListener(new Animation.AnimationListener()
                {
                    @Override
                    public void onAnimationStart(Animation animation)
                    {
                        background.setVisibility(View.VISIBLE);
                        background.setClickable(true);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation)
                    {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation)
                    {

                    }
                });

                background.startAnimation(fadeInAnimation);
            }

            @Override
            public void onMenuCollapsed()
            {
                AlphaAnimation fadeOutAnimation = new AlphaAnimation(backgroundAlpha, 0.0f);
                fadeOutAnimation.setDuration(backgroundAnimationDuration);
                fadeOutAnimation.setFillAfter(true);
                fadeOutAnimation.setAnimationListener(new Animation.AnimationListener()
                {
                    @Override
                    public void onAnimationStart(Animation animation)
                    {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation)
                    {
                        background.setVisibility(View.GONE);
                        background.setClickable(false);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation)
                    {

                    }
                });

                background.startAnimation(fadeOutAnimation);
            }
        });

        FloatingActionButton fabNewExpense = (FloatingActionButton) findViewById(R.id.fab_new_expense);
        fabNewExpense.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent startIntent = new Intent(MainActivity.this, ExpenseEditActivity.class);
                startIntent.putExtra("date", calendarFragment.getSelectedDate());

                if( UIHelper.areAnimationsEnabled(MainActivity.this) )
                {
                    startIntent.putExtra(ANIMATE_TRANSITION_KEY, true);
                    startIntent.putExtra(CENTER_X_KEY, (int) menu.getX() + (int) ((float) menu.getWidth() / 1.2f));
                    startIntent.putExtra(CENTER_Y_KEY, (int) menu.getY() + (int) ((float) menu.getHeight() / 1.2f));
                }

                ActivityCompat.startActivityForResult(MainActivity.this, startIntent, ADD_EXPENSE_ACTIVITY_CODE, null);

                menu.collapse();
            }
        });

        FloatingActionButton fabNewMonthlyExpense = (FloatingActionButton) findViewById(R.id.fab_new_monthly_expense);
        fabNewMonthlyExpense.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent startIntent = new Intent(MainActivity.this, MonthlyExpenseEditActivity.class);
                startIntent.putExtra("dateStart", calendarFragment.getSelectedDate());

                if( UIHelper.areAnimationsEnabled(MainActivity.this) )
                {
                    startIntent.putExtra(ANIMATE_TRANSITION_KEY, true);
                    startIntent.putExtra(CENTER_X_KEY, (int) menu.getX() + (int) ((float) menu.getWidth() / 1.2f));
                    startIntent.putExtra(CENTER_Y_KEY, (int) menu.getY() + (int) ((float) menu.getHeight() / 1.2f));
                }

                ActivityCompat.startActivityForResult(MainActivity.this, startIntent, ADD_EXPENSE_ACTIVITY_CODE, null);

                menu.collapse();
            }
        });

        /*
         * Expense Recycler view
         */
        RecyclerView expensesRecyclerView = (RecyclerView) findViewById(R.id.expensesRecyclerView);
        expensesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        Date date = new Date();
        if( savedInstanceState != null && savedInstanceState.containsKey(RECYCLE_VIEW_SAVED_DATE) )
        {
            Date savedDate = (Date) savedInstanceState.getSerializable(RECYCLE_VIEW_SAVED_DATE);
            if ( savedDate != null )
            {
                date = savedDate;
            }
        }

        expensesViewAdapter = new ExpensesRecyclerViewAdapter(this, db, date);
        expensesRecyclerView.setAdapter(expensesViewAdapter);

        updateBalanceDisplayForDay(date);
    }

    private void refreshRecyclerViewForDate(@NonNull Date date)
    {
        expensesViewAdapter.setDate(date, db);
    }

    private void refreshAllForDate(@NonNull Date date)
    {
        refreshRecyclerViewForDate(date);
        updateBalanceDisplayForDay(date);
        calendarFragment.setSelectedDates(date, date);
        calendarFragment.refreshView();
    }

    /**
     * Show a generic alert dialog telling the user an error occured while deleting monthly expense
     */
    private void showGenericMonthlyDeleteErrorDialog()
    {
        new AlertDialog.Builder(MainActivity.this)
            .setTitle(R.string.monthly_expense_delete_error_title)
            .setMessage(getResources().getString(R.string.monthly_expense_delete_error_message))
            .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            })
            .show();
    }

// ---------------------------------------->

    /**
     * An asynctask to delete a monthly expense from DB
     */
    private class DeleteMonthlyExpenseTask extends AsyncTask<Void, Integer, Boolean>
    {
        /**
         * Dialog used to display loading to the user
         */
        private ProgressDialog dialog;

        /**
         * The expense deleted by the user
         */
        private final Expense                  expense;
        /**
         * The monthly expense associated with the expense deleted by the user
         */
        private final MonthlyExpense monthlyExpense;
        /**
         * Type of delete
         */
        private final MonthlyExpenseDeleteType deleteType;

        /**
         * Expenses to restore if delete is successful and user cancels it
         */
        @Nullable
        private List<Expense> expensesToRestore;
        /**
         * Monthly expense to restore if delete is successful and user cancels it
         */
        @Nullable
        private MonthlyExpense monthlyExpenseToRestore;

        // ------------------------------------------->

        DeleteMonthlyExpenseTask(@NonNull MonthlyExpense monthlyExpense, @NonNull Expense expense, @NonNull MonthlyExpenseDeleteType deleteType)
        {
            this.monthlyExpense = monthlyExpense;
            this.expense = expense;
            this.deleteType = deleteType;
        }

        // ------------------------------------------->

        @Override
        protected Boolean doInBackground(Void... nothing)
        {
            switch (deleteType)
            {
                case ALL:
                {
                    monthlyExpenseToRestore = monthlyExpense;
                    expensesToRestore = db.getAllExpenseForMonthlyExpense(monthlyExpense);

                    boolean expensesDeleted = db.deleteAllExpenseForMonthlyExpense(monthlyExpense);
                    if( !expensesDeleted )
                    {
                        //TODO log error
                        return false;
                    }

                    boolean monthlyExpenseDeleted = db.deleteMonthlyExpense(monthlyExpense);
                    if( !monthlyExpenseDeleted )
                    {
                        //TODO log error
                        return false;
                    }

                    break;
                }
                case FROM:
                {
                    expensesToRestore = db.getAllExpensesForMonthlyExpenseFromDate(monthlyExpense, expense.getDate());

                    boolean expensesDeleted = db.deleteAllExpenseForMonthlyExpenseFromDate(monthlyExpense, expense.getDate());
                    if( !expensesDeleted )
                    {
                        //TODO log error
                        return false;
                    }

                    break;
                }
                case TO:
                {
                    expensesToRestore = db.getAllExpensesForMonthlyExpenseBeforeDate(monthlyExpense, expense.getDate());

                    boolean expensesDeleted = db.deleteAllExpenseForMonthlyExpenseBeforeDate(monthlyExpense, expense.getDate());
                    if( !expensesDeleted )
                    {
                        //TODO log error
                        return false;
                    }

                    break;
                }
                case ONE:
                {
                    expensesToRestore = new ArrayList<>(1);
                    expensesToRestore.add(expense);

                    boolean expenseDeleted = db.deleteExpense(expense);
                    if( !expenseDeleted )
                    {
                        //TODO log error
                        return false;
                    }

                    break;
                }
            }

            return true;
        }

        @Override
        protected void onPreExecute()
        {
            // Show a ProgressDialog
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setIndeterminate(true);
            dialog.setTitle(R.string.monthly_expense_delete_loading_title);
            dialog.setMessage(getResources().getString(R.string.monthly_expense_delete_loading_message));
            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            // Dismiss the dialog
            dialog.dismiss();

            if (result)
            {
                // Refresh and show confirm snackbar
                refreshAllForDate(expensesViewAdapter.getDate());
                Snackbar snackbar = Snackbar.make(coordinatorLayout, R.string.monthly_expense_delete_success_message, Snackbar.LENGTH_LONG);

                if( expensesToRestore != null ) // just in case..
                {
                    snackbar.setAction(R.string.undo, new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            new CancelDeleteMonthlyExpenseTask(expensesToRestore, monthlyExpenseToRestore).execute();
                        }
                    });
                }

                snackbar.setActionTextColor(ContextCompat.getColor(MainActivity.this, R.color.snackbar_action_undo));

                //noinspection ResourceType
                snackbar.setDuration(ACTION_SNACKBAR_LENGTH);
                snackbar.show();
            }
            else
            {
                showGenericMonthlyDeleteErrorDialog();
            }
        }


    }

    /**
     * An asynctask to restore deleted monthly expense from DB
     */
    private class CancelDeleteMonthlyExpenseTask extends AsyncTask<Void, Void, Boolean>
    {
        /**
         * Dialog used to display loading to the user
         */
        private ProgressDialog dialog;

        /**
         * List of expenses to restore
         */
        private final List<Expense> expensesToRestore;
        /**
         * Monthly expense to restore (will be null if delete type != ALL)
         */
        private final MonthlyExpense monthlyExpenseToRestore;

        // ------------------------------------------->

        /**
         *
         * @param expensesToRestore The deleted expenses to restore
         * @param monthlyExpenseToRestore the deleted monthly expense to restore
         */
        private CancelDeleteMonthlyExpenseTask(@NonNull List<Expense> expensesToRestore, @Nullable MonthlyExpense monthlyExpenseToRestore)
        {
            this.expensesToRestore = expensesToRestore;
            this.monthlyExpenseToRestore = monthlyExpenseToRestore;
        }

        // ------------------------------------------->

        @Override
        protected void onPreExecute()
        {
            // Show a ProgressDialog
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setIndeterminate(true);
            dialog.setTitle(R.string.monthly_expense_restoring_loading_title);
            dialog.setMessage(getResources().getString(R.string.monthly_expense_restoring_loading_message));
            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... params)
        {
            if( monthlyExpenseToRestore != null )
            {
                if( !db.addMonthlyExpense(monthlyExpenseToRestore) )
                {
                    return false;
                }
            }

            for(Expense expense: expensesToRestore)
            {
                if( !db.persistExpense(expense, true) )
                {
                    return false;
                }
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            // Dismiss the dialog
            dialog.dismiss();

            if (result)
            {
                // Refresh and show confirm snackbar
                refreshAllForDate(expensesViewAdapter.getDate());
                Snackbar.make(coordinatorLayout, R.string.monthly_expense_restored_success_message, Snackbar.LENGTH_LONG).show();
            }
            else
            {
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.monthly_expense_restore_error_title)
                    .setMessage(getResources().getString(R.string.monthly_expense_restore_error_message))
                    .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            dialog.dismiss();
                        }
                    })
                    .show();
            }
        }
    }
}