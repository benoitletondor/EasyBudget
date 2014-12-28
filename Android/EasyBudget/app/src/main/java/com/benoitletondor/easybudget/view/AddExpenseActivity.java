package com.benoitletondor.easybudget.view;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.benoitletondor.easybudget.R;

public class AddExpenseActivity extends ActionBarActivity
{
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
            finish(); //FIXME
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
                if(isRevenue)
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
                if( !isRevenue)
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

        ((EditText) findViewById(R.id.description_edittext)).setOnFocusChangeListener(new View.OnFocusChangeListener()
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

        ((EditText) findViewById(R.id.amount_edittext)).setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if( hasFocus )
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
