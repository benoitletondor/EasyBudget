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

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager.widget.ViewPager
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils

import com.benoitletondor.easybudgetapp.databinding.ActivityWelcomeBinding
import com.benoitletondor.easybudgetapp.helper.BaseActivity
import com.benoitletondor.easybudgetapp.helper.setNavigationBarColored
import com.benoitletondor.easybudgetapp.helper.setStatusBarColor
import com.benoitletondor.easybudgetapp.parameters.Parameters
import dagger.hilt.android.AndroidEntryPoint

import java.lang.IllegalStateException
import javax.inject.Inject
import kotlin.math.max

/**
 * Welcome screen activity
 *
 * @author Benoit LETONDOR
 */
@AndroidEntryPoint
class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>() {
    /**
     * Broadcast receiver for intent sent by fragments
     */
    private lateinit var receiver: BroadcastReceiver

    @Inject lateinit var parameters: Parameters

// ------------------------------------>

    private var step: Int
        get() = parameters.getOnboardingStep()
        set(step) = parameters.setOnboardingStep(step)

// ------------------------------------------>

    override fun createBinding(): ActivityWelcomeBinding = ActivityWelcomeBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isAndroid13OrMore = Build.VERSION.SDK_INT >= 33

        // Reinit step to 0 if already completed
        if (step == STEP_COMPLETED) {
            step = 0
        }

        binding.welcomeViewPager.adapter = object : FragmentStatePagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getItem(position: Int): Fragment {
                if (isAndroid13OrMore) {
                    when (position) {
                        0 -> return Onboarding1Fragment()
                        1 -> return Onboarding2Fragment()
                        2 -> return Onboarding3Fragment()
                        3 -> return OnboardingPushPermissionFragment()
                        4 -> return Onboarding4Fragment()
                    }
                } else {
                    when (position) {
                        0 -> return Onboarding1Fragment()
                        1 -> return Onboarding2Fragment()
                        2 -> return Onboarding3Fragment()
                        3 -> return Onboarding4Fragment()
                    }
                }
                throw IllegalStateException("unknown position $position")
            }

            override fun getCount(): Int = if (isAndroid13OrMore) 5 else 4
        }
        binding.welcomeViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                ((binding.welcomeViewPager.adapter as? FragmentStatePagerAdapter)?.getItem(position) as? OnboardingFragment<*>)?.let { fragment ->
                    setStatusBarColor(fragment.statusBarColor)
                }

                step = position
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
        binding.welcomeViewPager.offscreenPageLimit = binding.welcomeViewPager.adapter?.count ?: 0 // preload all fragments for transitions smoothness

        // Circle indicator
        binding.welcomeViewPagerIndicator.setViewPager(binding.welcomeViewPager)

        val filter = IntentFilter()
        filter.addAction(PAGER_NEXT_INTENT)
        filter.addAction(PAGER_PREVIOUS_INTENT)
        filter.addAction(PAGER_DONE_INTENT)

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val pager = binding.welcomeViewPager
                val pagerAdapter = pager.adapter ?: return

                if (PAGER_NEXT_INTENT == intent.action && pager.currentItem < pagerAdapter.count - 1) {
                    if (intent.getBooleanExtra(ANIMATE_TRANSITION_KEY, false)) {
                        // get the center for the clipping circle
                        val cx = intent.getIntExtra(CENTER_X_KEY, pager.x.toInt() + pager.width / 2)
                        val cy = intent.getIntExtra(CENTER_Y_KEY, pager.y.toInt() + pager.height / 2)

                        // get the final radius for the clipping circle
                        val finalRadius = max(pager.width, pager.height)

                        // create the animator for this view (the start radius is zero)
                        val anim = ViewAnimationUtils.createCircularReveal(pager, cx, cy, 0f, finalRadius.toFloat())

                        // make the view visible and start the animation
                        pager.setCurrentItem(pager.currentItem + 1, false)
                        anim.start()
                    } else {
                        pager.setCurrentItem(pager.currentItem + 1, true)
                    }
                } else if (PAGER_PREVIOUS_INTENT == intent.action && pager.currentItem > 0) {
                    pager.setCurrentItem(pager.currentItem - 1, true)
                } else if (PAGER_DONE_INTENT == intent.action) {
                    step = STEP_COMPLETED
                    this@WelcomeActivity.setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter)

        val initialStep = step

        // Init pager at the current step
        binding.welcomeViewPager.setCurrentItem(initialStep, false)

        // Set status bar color
        (((binding.welcomeViewPager.adapter) as? FragmentStatePagerAdapter)?.getItem(initialStep) as? OnboardingFragment<*>)?.let { fragment ->
            setStatusBarColor(fragment.statusBarColor)
        }

        setNavigationBarColored()
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)

        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.welcomeViewPager.currentItem > 0) {
            binding.welcomeViewPager.setCurrentItem(binding.welcomeViewPager.currentItem - 1, true)
            return
        }

        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    companion object {
        const val STEP_COMPLETED = Integer.MAX_VALUE

        const val ANIMATE_TRANSITION_KEY = "animate"
        const val CENTER_X_KEY = "centerX"
        const val CENTER_Y_KEY = "centerY"

        /**
         * Intent broadcasted by pager fragments to go next
         */
        const val PAGER_NEXT_INTENT = "welcome.pager.next"
        /**
         * Intent broadcasted by pager fragments to go previous
         */
        const val PAGER_PREVIOUS_INTENT = "welcome.pager.previous"
        /**
         * Intent broadcasted by pager fragments when welcome onboarding is done
         */
        const val PAGER_DONE_INTENT = "welcome.pager.done"
    }
}

/**
 * The current onboarding step (int)
 */
private const val ONBOARDING_STEP_PARAMETERS_KEY = "onboarding_step"

fun Parameters.getOnboardingStep(): Int {
    return getInt(ONBOARDING_STEP_PARAMETERS_KEY, 0)
}

private fun Parameters.setOnboardingStep(step: Int) {
    putInt(ONBOARDING_STEP_PARAMETERS_KEY, step)
}
