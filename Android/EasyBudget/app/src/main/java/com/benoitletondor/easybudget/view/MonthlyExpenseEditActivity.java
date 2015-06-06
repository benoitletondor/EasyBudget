package com.benoitletondor.easybudget.view;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.benoitletondor.easybudget.R;
import com.benoitletondor.easybudget.model.MonthlyExpense;

import java.util.Date;

public class MonthlyExpenseEditActivity extends DBActivity
{
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
            //TODO
            return true;
        }
        else if( id == android.R.id.home ) // Back button of the actionbar
        {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
