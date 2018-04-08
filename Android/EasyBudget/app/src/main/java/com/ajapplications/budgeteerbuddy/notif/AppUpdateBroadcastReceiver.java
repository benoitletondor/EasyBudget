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

package com.ajapplications.budgeteerbuddy.notif;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ajapplications.budgeteerbuddy.EasyBudget;

/**
 * Simple Broadcast receiver that is just here to receive events when a package is updated. This is
 * made to awake our application on an update (especially EasyBudget ones) to perform action. Those
 * actions are made on {@link EasyBudget#onUpdate(int, int)}.
 *
 * @author Benoit LETONDOR
 */
public class AppUpdateBroadcastReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {

    }
}
