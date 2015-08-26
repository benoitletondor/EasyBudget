package com.benoitletondor.easybudget.view;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.benoitletondor.easybudget.R;
import com.benoitletondor.easybudget.helper.CompatHelper;
import com.benoitletondor.easybudget.model.Expense;
import com.benoitletondor.easybudget.model.MonthlyExpense;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MonthlyExpenseEditActivity extends DBActivity
{
    /**
     * Edit text that contains the description
     */
    private EditText       descriptionEditText;
    /**
     * Edit text that contains the amount
     */
    private EditText       amountEditText;
    /**
     * Button for date selection
     */
    private Button         dateButton;

    /**
     * Expense that is being edited (will be null if it's a new one)
     */
    private MonthlyExpense expense;
    /**
     * The start date of the expense
     */
    private Date           dateStart;
    /**
     * The end date of the expense
     */
    private Date           dateEnd;
    /**
     * Is the new expense a revenue
     */
    private boolean isRevenue = false;


// ------------------------------------------->

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_expense_edit);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dateStart = (Date) getIntent().getSerializableExtra("dateStart");
        dateEnd = (Date) getIntent().getSerializableExtra("dateEnd");

        if (getIntent().hasExtra("expense"))
        {
            expense = (MonthlyExpense) getIntent().getSerializableExtra("expense");

            setTitle(R.string.title_activity_monthly_expense_edit);
        }

        setUpButtons();
        setUpTextFields();
        setUpDateButton();

        setResult(RESULT_CANCELED);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_monthly_expense_edit, menu);
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
        if (id == R.id.action_save)
        {
            if( validateInputs() )
            {
                int value = Integer.parseInt(amountEditText.getText().toString());

                MonthlyExpense expense = new MonthlyExpense(descriptionEditText.getText().toString(), isRevenue? -value : value, dateStart);

                new SaveMonthlyExpenseTask().execute(expense);
            }

            return true;
        }
        else if( id == android.R.id.home ) // Back button of the actionbar
        {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

// ----------------------------------->

    /**
     * Validate user inputs
     *
     * @return true if user inputs are ok, false otherwise
     */
    private boolean validateInputs()
    {
        boolean ok = true;

        String description = descriptionEditText.getText().toString();
        if( description.trim().isEmpty() )
        {
            descriptionEditText.setError("Enter a description"); //TODO translate
            ok = false;
        }

        String amount = amountEditText.getText().toString();
        if( amount.trim().isEmpty() )
        {
            amountEditText.setError("Enter an amount"); //TODO translate
            ok = false;
        }
        else
        {
            try
            {
                int value = Integer.parseInt(amount);
                if( value <= 0 )
                {
                    amountEditText.setError("Amount should be greater than 0"); //TODO
                    ok = false;
                }
            }
            catch(Exception e)
            {
                amountEditText.setError("Not a valid amount"); //TODO
                ok = false;
            }
        }

        return ok;
    }

    /**
     * Set-up revenue and payment buttons
     */
    private void setUpButtons()
    {
        final ImageView paymentCheckboxImageview = (ImageView) findViewById(R.id.payment_checkbox_imageview);
        final ImageView revenueCheckboxImageview = (ImageView) findViewById(R.id.revenue_checkbox_imageview);

        findViewById(R.id.payment_button_view).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
            if( isRevenue )
            {
                isRevenue = false;
                paymentCheckboxImageview.setImageResource(R.drawable.ic_radio_button_on);
                revenueCheckboxImageview.setImageResource(R.drawable.ic_radio_button_off);
            }
            }
        });

        findViewById(R.id.revenue_button_view).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
            if( !isRevenue )
            {
                isRevenue = true;
                paymentCheckboxImageview.setImageResource(R.drawable.ic_radio_button_off);
                revenueCheckboxImageview.setImageResource(R.drawable.ic_radio_button_on);
            }
            }
        });
    }

    /**
     * Set up text field focus behavior
     */
    private void setUpTextFields()
    {
        final TextView descriptionTextView = (TextView) findViewById(R.id.description_descriptor);
        final TextView amountTextView = (TextView) findViewById(R.id.amount_descriptor);

        descriptionEditText = (EditText) findViewById(R.id.description_edittext);
        descriptionEditText.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
            if (hasFocus)
            {
                descriptionTextView.setTextColor(ContextCompat.getColor(MonthlyExpenseEditActivity.this, R.color.accent));
                descriptionTextView.setTypeface(null, Typeface.BOLD);
            }
            else
            {
                descriptionTextView.setTextColor(ContextCompat.getColor(MonthlyExpenseEditActivity.this, R.color.secondary_text));
                descriptionTextView.setTypeface(null, Typeface.NORMAL);
            }
            }
        });

        if( expense != null )
        {
            descriptionEditText.setText(expense.getTitle());
        }

        amountEditText = (EditText) findViewById(R.id.amount_edittext);
        amountEditText.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
            if (hasFocus)
            {
                amountTextView.setTextColor(ContextCompat.getColor(MonthlyExpenseEditActivity.this, R.color.accent));
                amountTextView.setTypeface(null, Typeface.BOLD);
            }
            else
            {
                amountTextView.setTextColor(ContextCompat.getColor(MonthlyExpenseEditActivity.this, R.color.secondary_text));
                amountTextView.setTypeface(null, Typeface.NORMAL);
            }
            }
        });

        if( expense != null )
        {
            amountEditText.setText(String.valueOf(expense.getAmount()));
        }
    }

    /**
     * Set up the date button
     */
    private void setUpDateButton()
    {
        dateButton = (Button) findViewById(R.id.date_button);
        CompatHelper.removeButtonBorder(dateButton); // Remove border on lollipop

        updateDateButtonDisplay();

        dateButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
            DatePickerDialogFragment fragment = new DatePickerDialogFragment(dateStart, new DatePickerDialog.OnDateSetListener()
            {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
                {
                Calendar cal = Calendar.getInstance();

                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.MONTH, monthOfYear);
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                dateStart = cal.getTime();
                updateDateButtonDisplay();
                }
            });

            fragment.show(getSupportFragmentManager(), "datePicker");
            }
        });
    }

    private void updateDateButtonDisplay()
    {
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd yyyy", Locale.US);
        dateButton.setText(formatter.format(dateStart));
    }

// ------------------------------------------->

    /**
     * An asynctask to save monthly expense to DB
     */
    private class SaveMonthlyExpenseTask extends AsyncTask<MonthlyExpense, Integer, Boolean>
    {
        /**
         * Dialog used to display loading to the user
         */
        private ProgressDialog dialog;

        @Override
        protected Boolean doInBackground(MonthlyExpense... expenses)
        {
            for (MonthlyExpense expense : expenses)
            {
                boolean inserted = db.addMonthlyExpense(expense);
                if( !inserted )
                {
                    // TODO log error
                    return false;
                }

                Calendar cal = Calendar.getInstance();
                cal.setTime(dateStart);

                // Add up to 30 years of expenses
                for (int i = 0; i < 12 * 30; i++)
                {
                    boolean expenseInserted = db.addExpense(new Expense(expense.getTitle(), expense.getAmount(), cal.getTime(), expense.getId()));
                    if (!expenseInserted)
                    {
                        // TODO log error
                        return false;
                    }

                    cal.add(Calendar.MONTH, 1);

                    if (dateEnd != null && cal.getTime().after(dateEnd)) // If we have an end date, stop to that one
                    {
                        break;
                    }
                }
            }

            return true;
        }

        @Override
        protected void onPreExecute()
        {
            // Show a ProgressDialog
            dialog = new ProgressDialog(MonthlyExpenseEditActivity.this);
            dialog.setIndeterminate(true);
            dialog.setTitle(R.string.monthly_expense_add_loading_title);
            dialog.setMessage(getResources().getString(R.string.monthly_expense_add_loading_message));
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
                setResult(RESULT_OK);
                finish();
            }
            else
            {
                new AlertDialog.Builder(MonthlyExpenseEditActivity.this)
                    .setTitle(R.string.monthly_expense_add_error_title)
                    .setMessage(getResources().getString(R.string.monthly_expense_add_error_message))
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
