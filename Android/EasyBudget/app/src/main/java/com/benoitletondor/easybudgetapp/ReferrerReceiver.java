package com.benoitletondor.easybudgetapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;

import com.benoitletondor.easybudgetapp.view.MainActivity;
import com.google.android.gms.analytics.CampaignTrackingReceiver;
import com.google.android.gms.appinvite.AppInviteReferral;

/**
 * A receiver for referrer intents
 *
 * @author Benoit LETONDOR
 */
public class ReferrerReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        // Forward to Google analytics
        new CampaignTrackingReceiver().onReceive(context, intent);

        // Create deep link intent with correct action and add play store referral information
        Intent refIntent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(MainActivity.buildAppInvitesReferrerDeeplink(context)));

        Intent deepLinkIntent = AppInviteReferral.addPlayStoreReferrerToIntent(intent, refIntent);
        LocalBroadcastManager.getInstance(context).sendBroadcast(deepLinkIntent);
    }
}
