package com.benoitletondor.easybudget;

import android.app.Application;

import com.benoitletondor.easybudget.model.Expense;
import com.benoitletondor.easybudget.model.MonthlyExpense;
import com.benoitletondor.easybudget.model.db.DB;

import java.util.Date;

/**
 * EasyBudget application
 *
 * @author Benoit LETONDOR
 */
public class EasyBudget extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();

        DB db = new DB(getApplicationContext());
        db.clearDB();

        long monthlyID = db.addMonthlyExpense(new MonthlyExpense("Monthly", 10, new Date()));
        db.addExpense(new Expense("Monthly", 10, new Date(), monthlyID));

        db.addExpense(new Expense("Daily", 30, new Date()));
    }
}
