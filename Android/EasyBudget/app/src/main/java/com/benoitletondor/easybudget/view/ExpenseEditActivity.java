package com.benoitletondor.easybudget.view;

import android.app.DatePickerDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Activity to add a new expense
 *
 * @author Benoit LETONDOR
 */
public class ExpenseEditActivity extends DBActivity
{
    /**
     * Edit text that contains the description
     */
    private EditText descriptionEditText;
    /**
     * Edit text that contains the amount
     */
    private EditText amountEditText;
    /**
     * Button for date selection
     */
    private Button   dateButton;

    /**
     * Expense that is being edited (will be null if it's a new one)
     */
    private Expense expense;
    /**
     * The date of the expense
     */
    private Date    date;
    /**
     * Is the new expense a revenue
     */
    private boolean isRevenue = false;


// -------------------------------------->

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_edit);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        date = (Date) getIntent().getSerializableExtra("date");

        if (getIntent().hasExtra("expense"))
        {
            expense = (Expense) getIntent().getSerializableExtra("expense");
            isRevenue = expense.getAmount() < 0;

            setTitle(R.string.title_activity_edit_expense);
        }

        setUpButtons();
        setUpTextFields();
        setUpDateButton();

        setResult(RESULT_CANCELED);
    }

// ----------------------------------->

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_expense_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save)
        {
            if( validateInputs() )
            {
                int value = Integer.parseInt(amountEditText.getText().toString());

                Expense expense = new Expense(descriptionEditText.getText().toString(), isRevenue? -value : value, date);
                db.addExpense(expense);

                setResult(RESULT_OK);
                finish();
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
                    descriptionTextView.setTextColor(ContextCompat.getColor(ExpenseEditActivity.this, R.color.accent));
                    descriptionTextView.setTypeface(null, Typeface.BOLD);
                }
                else
                {
                    descriptionTextView.setTextColor(ContextCompat.getColor(ExpenseEditActivity.this, R.color.secondary_text));
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
                    amountTextView.setTextColor(ContextCompat.getColor(ExpenseEditActivity.this, R.color.accent));
                    amountTextView.setTypeface(null, Typeface.BOLD);
                }
                else
                {
                    amountTextView.setTextColor(ContextCompat.getColor(ExpenseEditActivity.this, R.color.secondary_text));
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
                DatePickerDialogFragment fragment = new DatePickerDialogFragment(date, new DatePickerDialog.OnDateSetListener()
                {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
                    {
                        Calendar cal = Calendar.getInstance();

                        cal.set(Calendar.YEAR, year);
                        cal.set(Calendar.MONTH, monthOfYear);
                        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        date = cal.getTime();
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
        dateButton.setText(formatter.format(date));
    }
}
