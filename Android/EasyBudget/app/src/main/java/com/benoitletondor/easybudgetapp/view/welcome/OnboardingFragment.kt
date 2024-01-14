/*
 *   Copyright 2024 Benoit LETONDOR
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

package com.benoitletondor.easybudgetapp.view.welcome

import android.content.Intent
import android.view.View
import androidx.annotation.ColorRes
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewbinding.ViewBinding
import com.benoitletondor.easybudgetapp.helper.BaseFragment

/**
 * Abstract fragment that contains common methods of all onboarding fragments
 *
 * @author Benoit LETONDOR
 */
abstract class OnboardingFragment<V: ViewBinding> : BaseFragment<V>() {
    /**
     * Get the status bar color that should be used for this fragment
     *
     * @return the wanted color of the status bar
     */
    @get:ColorRes
    abstract val statusBarColor: Int

    /**
     * Go to the next onboarding step without animation
     */
    protected operator fun next() {
        val intent = Intent(WelcomeActivity.PAGER_NEXT_INTENT)
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
    }

    /**
     * Go to the next onboarding step with a reveal animation starting from the given center
     *
     * @param animationCenter center of the reveal animation
     */
    protected fun next(animationCenter: View) {
        val intent = Intent(WelcomeActivity.PAGER_NEXT_INTENT)
        intent.putExtra(WelcomeActivity.ANIMATE_TRANSITION_KEY, true)
        intent.putExtra(WelcomeActivity.CENTER_X_KEY, animationCenter.x.toInt() + animationCenter.width / 2)
        intent.putExtra(WelcomeActivity.CENTER_Y_KEY, animationCenter.y.toInt() + animationCenter.height / 2)
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
    }

    /**
     * Finish the onboarding flow
     */
    protected fun done() {
        val intent = Intent(WelcomeActivity.PAGER_DONE_INTENT)
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
    }
}
