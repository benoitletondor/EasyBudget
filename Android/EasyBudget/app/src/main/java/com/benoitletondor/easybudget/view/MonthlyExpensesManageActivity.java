package com.benoitletondor.easybudget.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.benoitletondor.easybudget.R;
import com.benoitletondor.easybudget.view.monthly.MonthlyRecyclerViewAdapter;

public class MonthlyExpensesManageActivity extends DBActivity
{
    private static final int EDIT_MONTHLY_EXPENSE_ACTIVITY_CODE = 1000;

    private RecyclerView expensesRecyclerView;
    private MonthlyRecyclerViewAdapter expensesViewAdapter;

// ----------------------------------------->

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_expenses_manage);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initRecyclerView(savedInstanceState);

        setResult(RESULT_CANCELED);
    }

    @Override
    protected void onDestroy()
    {
        expensesRecyclerView = null;
        expensesViewAdapter = null;

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_monthly_expenses_manage, menu);
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
        if( id == android.R.id.home ) // Back button of the actionbar
        {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if( resultCode == RESULT_OK ) // If an edit or a creation happened set result to OK to pass it to MainActivity
        {
            setResult(RESULT_OK);
        }
    }

// ------------------------------------------>

    private void initRecyclerView(Bundle savedInstanceState)
    {
        expensesRecyclerView = (RecyclerView) findViewById(R.id.monthlyExpensesRecyclerView);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.monthlyFab);
        fab.setRippleColor(getResources().getColor(R.color.accent));
        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent startIntent = new Intent(MonthlyExpensesManageActivity.this, MonthlyExpenseEditActivity.class);

                ActivityCompat.startActivityForResult(MonthlyExpensesManageActivity.this, startIntent, EDIT_MONTHLY_EXPENSE_ACTIVITY_CODE, null);
            }
        });

        expensesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        expensesViewAdapter = new MonthlyRecyclerViewAdapter(db);
        expensesRecyclerView.setAdapter(expensesViewAdapter);
    }
}
