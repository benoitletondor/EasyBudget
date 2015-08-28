package com.benoitletondor.easybudget.helper;

/**
 * List of keys to query {@link com.benoitletondor.easybudget.helper.Parameters}
 *
 * @author Benoit LETONDOR
 */
public class ParameterKeys
{
    /**
     * Date of the base balance set-up (long)
     */
    public static final String INIT_DATE       = "init_date";
    /**
     * Local identifier of the device (generated on first launch) (string)
     */
    public static final String LOCAL_ID        = "local_id";
    /**
     * The chosen ISO code of the currency (string)
     */
    public static final String CURRENCY_ISO    = "currency_iso";
    /**
     * The current onboarding step (int)
     */
    public static final String ONBOARDING_STEP = "onboarding_step";
}
