/*
 *   Copyright 2022 Benoit LETONDOR
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

package com.benoitletondor.easybudgetapp.view.premium

import android.app.Activity
import android.app.ProgressDialog
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.databinding.ActivityPremiumBinding
import com.benoitletondor.easybudgetapp.helper.BaseActivity
import com.benoitletondor.easybudgetapp.helper.setStatusBarColor
import com.benoitletondor.easybudgetapp.iab.PremiumPurchaseFlowResult
import dagger.hilt.android.AndroidEntryPoint
import java.lang.IllegalStateException

/**
 * Activity that contains the premium onboarding screen. This activity should return with a
 * [Activity.RESULT_OK] if user has successfully purchased premium.
 *
 * @author Benoit LETONDOR
 */
@AndroidEntryPoint
class PremiumActivity : BaseActivity<ActivityPremiumBinding>() {
    private val viewModel: PremiumViewModel by viewModels()

    override fun createBinding(): ActivityPremiumBinding = ActivityPremiumBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cancelled by default
        setResult(Activity.RESULT_CANCELED)

        binding.premiumViewPager.adapter = object : FragmentPagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getItem(position: Int): Fragment {
                when (position) {
                    0 -> return Premium1Fragment()
                    1 -> return Premium2Fragment()
                    2 -> return Premium3Fragment()
                    3 -> return Premium4Fragment()
                    4 -> return Premium5Fragment()
                    5 -> return Premium6Fragment()
                }

                throw IllegalStateException()
            }

            override fun getCount(): Int = 6
        }
        binding.premiumViewPager.offscreenPageLimit = binding.premiumViewPager.adapter!!.count // preload all fragments for transitions smoothness

        // Circle indicator
        binding.premiumViewPagerIndicator.setViewPager(binding.premiumViewPager)

        binding.premiumNotNowButton.setOnClickListener {
            finish()
        }

        binding.premiumCtaButton.setOnClickListener {
            viewModel.onBuyPremiumClicked(this)
        }

        var loadingProgressDialog: ProgressDialog? = null
        viewModel.premiumFlowErrorEventStream.observe(this) { status ->
            when (status) {
                PremiumPurchaseFlowResult.Cancelled -> {
                    loadingProgressDialog?.dismiss()
                    loadingProgressDialog = null
                }
                is PremiumPurchaseFlowResult.Error -> {
                    loadingProgressDialog?.dismiss()
                    loadingProgressDialog = null

                    AlertDialog.Builder(this)
                        .setTitle(R.string.iab_purchase_error_title)
                        .setMessage(getString(R.string.iab_purchase_error_message, status.reason))
                        .setPositiveButton(R.string.ok) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
                PremiumPurchaseFlowResult.Success -> Unit
            }
        }

        viewModel.premiumFlowStatusLiveData.observe(this) { status ->
            when (status) {
                PremiumFlowStatus.NOT_STARTED -> {
                    loadingProgressDialog?.dismiss()
                    loadingProgressDialog = null
                }
                PremiumFlowStatus.LOADING -> {
                    loadingProgressDialog = ProgressDialog.show(
                        this@PremiumActivity,
                        resources.getString(R.string.iab_purchase_wait_title),
                        resources.getString(R.string.iab_purchase_wait_message),
                        true, false
                    )
                }
                PremiumFlowStatus.DONE -> {
                    loadingProgressDialog?.dismiss()
                    loadingProgressDialog = null

                    setResult(Activity.RESULT_OK)
                    finish()
                }
                null -> {}
            }
        }

        setStatusBarColor(R.color.easy_budget_green)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags = window.decorView.systemUiVisibility
            flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()

            window.decorView.systemUiVisibility = flags
        }
    }

    override fun onBackPressed() {
        if (binding.premiumViewPager.currentItem > 0) {
            binding.premiumViewPager.setCurrentItem(binding.premiumViewPager.currentItem - 1, true)
            return
        }

        super.onBackPressed()
    }
}
