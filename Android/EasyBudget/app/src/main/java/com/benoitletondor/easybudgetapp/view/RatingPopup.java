package com.benoitletondor.easybudgetapp.view;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.benoitletondor.easybudgetapp.R;

/**
 * Rating popup that ask user for feedback and redirect them to the PlayStore
 * TODO tracking, store user choices
 *
 * @author Benoit LETONDOR
 */
public class RatingPopup
{
    @NonNull
    private Context activity;

    public RatingPopup(@NonNull Activity activity)
    {
        this.activity = activity;
    }

// ------------------------------------------>

    public void show()
    {
        buildStep1().show();
    }

    private AlertDialog buildStep1()
    {
        return new AlertDialog.Builder(activity)
            .setTitle(R.string.rating_popup_question_title)
            .setMessage(R.string.rating_popup_question_message)
            .setNegativeButton(R.string.rating_popup_question_cta_negative, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    buildNegativeStep().show();
                }
            })
            .setPositiveButton(R.string.rating_popup_question_cta_positive, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    buildPositiveStep().show();
                }
            })
            .create();
    }

    private AlertDialog buildNegativeStep()
    {
        return new AlertDialog.Builder(activity)
            .setTitle(R.string.rating_popup_negative_title)
            .setMessage(R.string.rating_popup_negative_message)
            .setNegativeButton(R.string.rating_popup_negative_cta_negative, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    // Nothing to do
                }
            })
            .setPositiveButton(R.string.rating_popup_negative_cta_positive, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SENDTO);
                    sendIntent.setData(Uri.parse("mailto:")); // only email apps should handle this
                    sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{activity.getResources().getString(R.string.rating_feedback_email)});
                    sendIntent.putExtra(Intent.EXTRA_TEXT, activity.getResources().getString(R.string.rating_feedback_send_text));
                    sendIntent.putExtra(Intent.EXTRA_SUBJECT, activity.getResources().getString(R.string.rating_feedback_send_subject));

                    if (sendIntent.resolveActivity(activity.getPackageManager()) != null)
                    {
                        activity.startActivity(sendIntent);
                    }
                    else
                    {
                        Toast.makeText(activity, activity.getResources().getString(R.string.rating_feedback_send_error), Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .create();
    }

    private AlertDialog buildPositiveStep()
    {
        return new AlertDialog.Builder(activity)
            .setTitle(R.string.rating_popup_positive_title)
            .setMessage(R.string.rating_popup_positive_message)
            .setNegativeButton(R.string.rating_popup_positive_cta_negative, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    // Nothing to do
                }
            })
            .setPositiveButton(R.string.rating_popup_positive_cta_positive, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    final String appPackageName = activity.getPackageName();

                    try
                    {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName));

                        activity.startActivity(intent);
                    }
                    catch (ActivityNotFoundException e)
                    {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName));

                        activity.startActivity(intent);
                    }
                }
            })
            .create();
    }
}
