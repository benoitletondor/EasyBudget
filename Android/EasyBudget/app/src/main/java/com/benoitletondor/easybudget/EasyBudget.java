package com.benoitletondor.easybudget;

import android.app.Application;

import com.benoitletondor.easybudget.model.MonthlyExpense;
import com.benoitletondor.easybudget.model.OneTimeExpense;
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
        db.addOneTimeExpense(new OneTimeExpense("One Time", 30, new Date()));
        db.addMonthlyExpense(new MonthlyExpense("Monthly", 10, new Date()));
    }
}
