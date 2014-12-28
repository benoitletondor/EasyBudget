package com.benoitletondor.easybudget.view;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.benoitletondor.easybudget.R;

/**
 * Activity to add a new expense
 *
 * @author Benoit LETONDOR
 */
public class AddExpenseActivity extends ActionBarActivity
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
     * Is the new expense a revenue
     */
    private boolean isRevenue = false;

// -------------------------------------->

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setUpButtons();
        setUpTextFields();
    }

// ----------------------------------->

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_expense, menu);
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
                // TODO save
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
                if( hasFocus )
                {
                    descriptionTextView.setTextColor(getResources().getColor(R.color.accent));
                }
                else
                {
                    descriptionTextView.setTextColor(getResources().getColor(R.color.secondary_text));
                }
            }
        });

        amountEditText = (EditText) findViewById(R.id.amount_edittext);
        amountEditText.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (hasFocus)
                {
                    amountTextView.setTextColor(getResources().getColor(R.color.accent));
                }
                else
                {
                    amountTextView.setTextColor(getResources().getColor(R.color.secondary_text));
                }
            }
        });
    }
}
