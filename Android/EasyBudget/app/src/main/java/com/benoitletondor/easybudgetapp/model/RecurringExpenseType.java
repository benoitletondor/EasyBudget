package com.benoitletondor.easybudgetapp.model;

/**
 * Type of recurring expense
 *
 * @author Benoit LETONDOR
 */
public enum RecurringExpenseType
{
    /**
     * An expense that occurs every week
     */
    WEEKLY,

    /**
     * An expense that occurs every 2 weeks
     */
    BI_WEEKLY,

    /**
     * An expense that occurs every month
     */
    MONTHLY,

    /**
     * An expense that occurs once a year
     */
    YEARLY
}
