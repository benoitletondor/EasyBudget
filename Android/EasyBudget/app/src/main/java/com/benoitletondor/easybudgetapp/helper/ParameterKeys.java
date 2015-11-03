package com.benoitletondor.easybudgetapp.helper;

/**
 * List of keys to query {@link com.benoitletondor.easybudgetapp.helper.Parameters}
 *
 * @author Benoit LETONDOR
 */
public class ParameterKeys
{
    /**
     * App version stored to detect updates (int)
     */
    public static final String APP_VERSION = "appversion";
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
     * Warning limit for low money on account (int)
     */
    public static final String LOW_MONEY_WARNING_AMOUNT = "low_money_warning_amount";
    /**
     * The current onboarding step (int)
     */
    public static final String ONBOARDING_STEP = "onboarding_step";
    /**
     * Are animations enabled (boolean)
     */
    public static final String ANIMATIONS_ENABLED = "animation_enabled";
    /**
     * Number of invitations sent by the user (int)
     */
    public static final String NUMBER_OF_INVITATIONS = "number_of_invitations";
    /**
     * ID of the invitation the user came with (String)
     */
    public static final String INVITATION_ID = "invitation_id";
    /**
     * Source of the installation (String)
     */
    public static final String INSTALLATION_SOURCE = "installation_source";
    /**
     * Referrer of the installation (String)
     */
    public static final String INSTALLATION_REFERRER = "installation_referrer";
}
