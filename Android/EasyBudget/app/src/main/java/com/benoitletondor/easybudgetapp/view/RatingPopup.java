/*
 *   Copyright 2015 Benoit LETONDOR
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.benoitletondor.easybudgetapp.view;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import android.widget.Toast;

import com.benoitletondor.easybudgetapp.R;
import com.benoitletondor.easybudgetapp.helper.Logger;
import com.benoitletondor.easybudgetapp.helper.ParameterKeys;
import com.benoitletondor.easybudgetapp.helper.Parameters;
import com.benoitletondor.easybudgetapp.helper.UIHelper;
import com.benoitletondor.easybudgetapp.helper.UserHelper;

/**
 * Rating popup that ask user for feedback and redirect them to the PlayStore
 * TODO send data to GA
 *
 * @author Benoit LETONDOR
 */
public class RatingPopup
{
    /**
     * Stored context (beware of leaks !)
     */
    @NonNull
    private final Context context;
    @NonNull
    private final Parameters parameters;

    public RatingPopup(@NonNull Activity context, @NonNull Parameters parameters)
    {
        this.context = context;
        this.parameters = parameters;
    }

// ------------------------------------------>

    /**
     * Show the rating popup to the user
     *
     * @param forceShow force show even if the user already completed it. If not forced, the user
     *                  will have a button to asked not to be asked again
     * @return true if the popup has been shown, false otherwise
     */
    public boolean show(boolean forceShow)
    {
        if( !forceShow && UserHelper.hasUserCompleteRating(parameters) )
        {
            Logger.debug("Not showing rating cause user already completed it");
            return false;
        }

        setRatingPopupStep(parameters, RatingPopupStep.STEP_SHOWN);

        AlertDialog dialog = buildStep1(!forceShow);
        dialog.show();

        if( !forceShow )
        {
            // Center buttons
            UIHelper.centerDialogButtons(dialog);
        }

        return true;
    }

    /**
     * Build the first step of rating asking the user what he thinks of the app
     *
     * @param includeDontAskMeAgainButton boolean that indicates of the don't ask me again button
     *                                    should be included
     * @return A ready to be shown {@link AlertDialog}
     */
    private AlertDialog buildStep1(boolean includeDontAskMeAgainButton)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
            .setTitle(R.string.rating_popup_question_title)
            .setMessage(R.string.rating_popup_question_message)
            .setNegativeButton(R.string.rating_popup_question_cta_negative, (dialog, which) -> {
                setRatingPopupStep(parameters, RatingPopupStep.STEP_DISLIKE);
                buildNegativeStep().show();
            })
            .setPositiveButton(R.string.rating_popup_question_cta_positive, (dialog, which) -> {
                setRatingPopupStep(parameters, RatingPopupStep.STEP_LIKE);
                buildPositiveStep().show();
            });

        if( includeDontAskMeAgainButton )
        {
            builder.setNeutralButton(R.string.rating_popup_question_cta_dont_ask_again, (dialog, which) -> {
                setRatingPopupStep(parameters, RatingPopupStep.STEP_NOT_ASK_ME_AGAIN);
                UserHelper.setUserHasCompleteRating(parameters);
                dialog.dismiss();
            });
        }

        return builder.create();
    }

    /**
     * Build the step to shown when the user said he doesn't like the app
     *
     * @return A ready to be shown {@link AlertDialog}
     */
    private AlertDialog buildNegativeStep()
    {
        return new AlertDialog.Builder(context)
            .setTitle(R.string.rating_popup_negative_title)
            .setMessage(R.string.rating_popup_negative_message)
            .setNegativeButton(R.string.rating_popup_negative_cta_negative, (dialog, which) -> {
                setRatingPopupStep(parameters, RatingPopupStep.STEP_DISLIKE_NO_FEEDBACK);
                UserHelper.setUserHasCompleteRating(parameters);
            })
            .setPositiveButton(R.string.rating_popup_negative_cta_positive, (dialog, which) -> {
                setRatingPopupStep(parameters, RatingPopupStep.STEP_DISLIKE_FEEDBACK);
                UserHelper.setUserHasCompleteRating(parameters);

                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SENDTO);
                sendIntent.setData(Uri.parse("mailto:")); // only email apps should handle this
                sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{context.getResources().getString(R.string.rating_feedback_email)});
                sendIntent.putExtra(Intent.EXTRA_TEXT, context.getResources().getString(R.string.rating_feedback_send_text));
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, context.getResources().getString(R.string.rating_feedback_send_subject));

                if (sendIntent.resolveActivity(context.getPackageManager()) != null)
                {
                    context.startActivity(sendIntent);
                }
                else
                {
                    Toast.makeText(context, context.getResources().getString(R.string.rating_feedback_send_error), Toast.LENGTH_SHORT).show();
                }
            })
            .create();
    }

    /**
     * Build the step to shown when the user said he likes the app
     *
     * @return A ready to be shown {@link AlertDialog}
     */
    private AlertDialog buildPositiveStep()
    {
        return new AlertDialog.Builder(context)
            .setTitle(R.string.rating_popup_positive_title)
            .setMessage(R.string.rating_popup_positive_message)
            .setNegativeButton(R.string.rating_popup_positive_cta_negative, (dialog, which) -> {
                setRatingPopupStep(parameters, RatingPopupStep.STEP_LIKE_NOT_RATED);
                UserHelper.setUserHasCompleteRating(parameters);
            })
            .setPositiveButton(R.string.rating_popup_positive_cta_positive, (dialog, which) -> {
                setRatingPopupStep(parameters, RatingPopupStep.STEP_LIKE_RATED);
                UserHelper.setUserHasCompleteRating(parameters);

                final String appPackageName = context.getPackageName();

                try
                {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName));

                    context.startActivity(intent);
                }
                catch (ActivityNotFoundException e)
                {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName));

                    context.startActivity(intent);
                }
            })
            .create();
    }

// -------------------------------------------->

    /**
     * Get the current user rating step
     */
    public static RatingPopupStep getUserStep(@NonNull Parameters parameters)
    {
        return RatingPopupStep.fromValue(parameters.getInt(ParameterKeys.RATING_STEP, RatingPopupStep.STEP_NOT_SHOWN.value));
    }

    /**
     * Set the current user rating step
     */
    private static void setRatingPopupStep(@NonNull Parameters parameters, @NonNull RatingPopupStep step)
    {
        parameters.putInt(ParameterKeys.RATING_STEP, step.value);
    }

    /**
     * An enum that define the user step in the rating process
     */
    public enum RatingPopupStep
    {
        /**
         * Rating popup not shown
         */
        STEP_NOT_SHOWN(0),

        /**
         * Rating popup shown
         */
        STEP_SHOWN(1),

        /**
         * User clicked "I like the app"
         */
        STEP_LIKE(2),

        /**
         * User clicked "I don't want to rate the app"
         */
        STEP_LIKE_NOT_RATED(3),

        /**
         * User clicked "I want to rate the app"
         */
        STEP_LIKE_RATED(4),

        /**
         * User clicked "I don't like the app"
         */
        STEP_DISLIKE(5),

        /**
         * User clicked "I don't wanna give my feedback"
         */
        STEP_DISLIKE_NO_FEEDBACK(6),

        /**
         * User clicked "I want to give my feedback"
         */
        STEP_DISLIKE_FEEDBACK(7),

        /**
         * User clicked "Don't ask me again" on the first screen
         */
        STEP_NOT_ASK_ME_AGAIN(8);

        public final int value;

        RatingPopupStep(int value)
        {
            this.value = value;
        }

        /**
         * Retrive the enum for the given value
         *
         * @param value value to find
         * @return the enum if found, null otherwise
         */
        @Nullable
        public static RatingPopupStep fromValue(int value)
        {
            for(RatingPopupStep step : values())
            {
                if( step.value == value )
                {
                    return step;
                }
            }

            return null;
        }
    }
}
