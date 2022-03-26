/*
 *   Copyright 2021 Benoit LETONDOR
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

package com.benoitletondor.easybudgetapp.view.settings.backup

import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.databinding.ActivityBackupSettingsBinding
import com.benoitletondor.easybudgetapp.helper.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import kotlin.system.exitProcess

@AndroidEntryPoint
class BackupSettingsActivity : BaseActivity<ActivityBackupSettingsBinding>() {
    private val viewModel: BackupSettingsViewModel by viewModels()

    override fun createBinding(): ActivityBackupSettingsBinding = ActivityBackupSettingsBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup_settings)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel.cloudBackupStateStream.observe(this) { cloudBackupState ->
            when (cloudBackupState) {
                BackupCloudStorageState.NotAuthenticated -> {
                    binding.backupSettingsCloudStorageNotAuthenticatedState.visibility = View.VISIBLE
                    binding.backupSettingsCloudStorageAuthenticatingState.visibility = View.GONE
                    binding.backupSettingsCloudStorageNotActivatedState.visibility = View.GONE
                }
                BackupCloudStorageState.Authenticating -> {
                    binding.backupSettingsCloudStorageNotAuthenticatedState.visibility = View.GONE
                    binding.backupSettingsCloudStorageAuthenticatingState.visibility = View.VISIBLE
                    binding.backupSettingsCloudStorageNotActivatedState.visibility = View.GONE
                }
                is BackupCloudStorageState.NotActivated -> {
                    binding.backupSettingsCloudStorageNotAuthenticatedState.visibility = View.GONE
                    binding.backupSettingsCloudStorageAuthenticatingState.visibility = View.GONE
                    binding.backupSettingsCloudStorageNotActivatedState.visibility = View.VISIBLE

                    binding.backupSettingsCloudStorageEmail.text = cloudBackupState.currentUser.email
                    binding.backupSettingsCloudStorageLogoutButton.visibility = View.VISIBLE
                    binding.backupSettingsCloudStorageBackupSwitch.visibility = View.VISIBLE
                    binding.backupSettingsCloudStorageBackupSwitchDescription.visibility = View.VISIBLE
                    binding.backupSettingsCloudStorageBackupSwitchDescription.text = getString(
                        R.string.backup_settings_cloud_backup_status,
                        getString(R.string.backup_settings_cloud_backup_status_disabled),
                    )
                    binding.backupSettingsCloudStorageBackupSwitch.isChecked = false
                    binding.backupSettingsCloudStorageActivatedDescription.visibility = View.GONE
                    binding.backupSettingsCloudLastUpdate.visibility = View.GONE
                    binding.backupSettingsCloudBackupCta.visibility = View.GONE
                    binding.backupSettingsCloudRestoreCta.visibility = View.GONE
                    binding.backupSettingsCloudStorageRestoreDescription.visibility = View.GONE
                    binding.backupSettingsCloudStorageRestoreExplanation.visibility = View.GONE
                    binding.backupSettingsCloudStorageDeleteTitle.visibility = View.GONE
                    binding.backupSettingsCloudStorageDeleteExplanation.visibility = View.GONE
                    binding.backupSettingsCloudDeleteCta.visibility = View.GONE
                    binding.backupSettingsCloudBackupLoadingProgress.visibility = View.GONE
                }
                is BackupCloudStorageState.Activated -> {
                    binding.backupSettingsCloudStorageNotAuthenticatedState.visibility = View.GONE
                    binding.backupSettingsCloudStorageAuthenticatingState.visibility = View.GONE
                    binding.backupSettingsCloudStorageNotActivatedState.visibility = View.VISIBLE

                    binding.backupSettingsCloudStorageEmail.text = cloudBackupState.currentUser.email
                    binding.backupSettingsCloudStorageLogoutButton.visibility = View.VISIBLE
                    binding.backupSettingsCloudStorageBackupSwitchDescription.visibility = View.VISIBLE
                    binding.backupSettingsCloudStorageBackupSwitchDescription.text = getString(
                        R.string.backup_settings_cloud_backup_status,
                        getString(R.string.backup_settings_cloud_backup_status_activated),
                    )
                    binding.backupSettingsCloudStorageBackupSwitch.visibility = View.VISIBLE
                    binding.backupSettingsCloudStorageBackupSwitch.isChecked = true
                    binding.backupSettingsCloudStorageActivatedDescription.visibility = View.VISIBLE
                    showLastUpdateDate(cloudBackupState.lastBackupDate)
                    binding.backupSettingsCloudLastUpdate.visibility = View.VISIBLE

                    if (cloudBackupState.backupNowAvailable) {
                        binding.backupSettingsCloudBackupCta.visibility = View.VISIBLE
                    } else {
                        binding.backupSettingsCloudBackupCta.visibility = View.GONE
                    }

                    if (cloudBackupState.restoreAvailable) {
                        binding.backupSettingsCloudStorageRestoreDescription.visibility = View.VISIBLE
                        binding.backupSettingsCloudStorageRestoreExplanation.visibility = View.VISIBLE
                        binding.backupSettingsCloudRestoreCta.visibility = View.VISIBLE

                        binding.backupSettingsCloudStorageDeleteTitle.visibility = View.VISIBLE
                        binding.backupSettingsCloudStorageDeleteExplanation.visibility = View.VISIBLE
                        binding.backupSettingsCloudDeleteCta.visibility = View.VISIBLE
                    } else {
                        binding.backupSettingsCloudRestoreCta.visibility = View.GONE
                        binding.backupSettingsCloudStorageRestoreDescription.visibility = View.GONE
                        binding.backupSettingsCloudStorageRestoreExplanation.visibility = View.GONE

                        binding.backupSettingsCloudStorageDeleteTitle.visibility = View.GONE
                        binding.backupSettingsCloudStorageDeleteExplanation.visibility = View.GONE
                        binding.backupSettingsCloudDeleteCta.visibility = View.GONE
                    }

                    binding.backupSettingsCloudBackupLoadingProgress.visibility = View.GONE
                }
                is BackupCloudStorageState.BackupInProgress -> {
                    binding.backupSettingsCloudStorageNotAuthenticatedState.visibility = View.GONE
                    binding.backupSettingsCloudStorageAuthenticatingState.visibility = View.GONE
                    binding.backupSettingsCloudStorageNotActivatedState.visibility = View.VISIBLE

                    binding.backupSettingsCloudStorageEmail.text = cloudBackupState.currentUser.email
                    binding.backupSettingsCloudStorageLogoutButton.visibility = View.GONE
                    binding.backupSettingsCloudStorageBackupSwitch.visibility = View.GONE
                    binding.backupSettingsCloudStorageBackupSwitchDescription.visibility = View.GONE
                    binding.backupSettingsCloudStorageActivatedDescription.visibility = View.GONE
                    binding.backupSettingsCloudLastUpdate.visibility = View.GONE
                    binding.backupSettingsCloudBackupCta.visibility = View.GONE
                    binding.backupSettingsCloudRestoreCta.visibility = View.GONE
                    binding.backupSettingsCloudStorageRestoreDescription.visibility = View.GONE
                    binding.backupSettingsCloudStorageRestoreExplanation.visibility = View.GONE
                    binding.backupSettingsCloudStorageDeleteTitle.visibility = View.GONE
                    binding.backupSettingsCloudStorageDeleteExplanation.visibility = View.GONE
                    binding.backupSettingsCloudDeleteCta.visibility = View.GONE
                    binding.backupSettingsCloudBackupLoadingProgress.visibility = View.VISIBLE
                }
                is BackupCloudStorageState.RestorationInProgress -> {
                    binding.backupSettingsCloudStorageNotAuthenticatedState.visibility = View.GONE
                    binding.backupSettingsCloudStorageAuthenticatingState.visibility = View.GONE
                    binding.backupSettingsCloudStorageNotActivatedState.visibility = View.VISIBLE

                    binding.backupSettingsCloudStorageEmail.text = cloudBackupState.currentUser.email
                    binding.backupSettingsCloudStorageLogoutButton.visibility = View.GONE
                    binding.backupSettingsCloudStorageBackupSwitch.visibility = View.GONE
                    binding.backupSettingsCloudStorageBackupSwitchDescription.visibility = View.GONE
                    binding.backupSettingsCloudStorageActivatedDescription.visibility = View.GONE
                    binding.backupSettingsCloudLastUpdate.visibility = View.GONE
                    binding.backupSettingsCloudBackupCta.visibility = View.GONE
                    binding.backupSettingsCloudRestoreCta.visibility = View.GONE
                    binding.backupSettingsCloudStorageRestoreDescription.visibility = View.GONE
                    binding.backupSettingsCloudStorageRestoreExplanation.visibility = View.GONE
                    binding.backupSettingsCloudStorageDeleteTitle.visibility = View.GONE
                    binding.backupSettingsCloudStorageDeleteExplanation.visibility = View.GONE
                    binding.backupSettingsCloudDeleteCta.visibility = View.GONE
                    binding.backupSettingsCloudBackupLoadingProgress.visibility = View.VISIBLE
                }
                is BackupCloudStorageState.DeletionInProgress -> {
                    binding.backupSettingsCloudStorageNotAuthenticatedState.visibility = View.GONE
                    binding.backupSettingsCloudStorageAuthenticatingState.visibility = View.GONE
                    binding.backupSettingsCloudStorageNotActivatedState.visibility = View.VISIBLE

                    binding.backupSettingsCloudStorageEmail.text = cloudBackupState.currentUser.email
                    binding.backupSettingsCloudStorageLogoutButton.visibility = View.GONE
                    binding.backupSettingsCloudStorageBackupSwitch.visibility = View.GONE
                    binding.backupSettingsCloudStorageBackupSwitchDescription.visibility = View.GONE
                    binding.backupSettingsCloudStorageActivatedDescription.visibility = View.GONE
                    binding.backupSettingsCloudLastUpdate.visibility = View.GONE
                    binding.backupSettingsCloudBackupCta.visibility = View.GONE
                    binding.backupSettingsCloudRestoreCta.visibility = View.GONE
                    binding.backupSettingsCloudStorageRestoreDescription.visibility = View.GONE
                    binding.backupSettingsCloudStorageRestoreExplanation.visibility = View.GONE
                    binding.backupSettingsCloudStorageDeleteTitle.visibility = View.GONE
                    binding.backupSettingsCloudStorageDeleteExplanation.visibility = View.GONE
                    binding.backupSettingsCloudDeleteCta.visibility = View.GONE
                    binding.backupSettingsCloudBackupLoadingProgress.visibility = View.VISIBLE
                }
            }
        }

        viewModel.backupNowErrorEvent.observe(this) {
            AlertDialog.Builder(this)
                .setTitle(R.string.backup_now_error_title)
                .setMessage(R.string.backup_now_error_message)
                .setPositiveButton(android.R.string.ok, null)
        }

        viewModel.previousBackupAvailableEvent.observe(this) { lastBackupDate ->
            AlertDialog.Builder(this)
                .setTitle(R.string.backup_already_exist_title)
                .setMessage(
                    getString(
                        R.string.backup_already_exist_message,
                        lastBackupDate.formatLastBackupDate()
                    )
                )
                .setPositiveButton(R.string.backup_already_exist_positive_cta) { _, _ ->
                    viewModel.onRestorePreviousBackupButtonPressed()
                }
                .setNegativeButton(R.string.backup_already_exist_negative_cta) { _, _ ->
                    viewModel.onIgnorePreviousBackupButtonPressed()
                }
                .show()
        }

        viewModel.restorationErrorEvent.observe(this) {
            AlertDialog.Builder(this)
                .setTitle(R.string.backup_restore_error_title)
                .setMessage(R.string.backup_restore_error_message)
                .setPositiveButton(android.R.string.ok, null)
        }

        viewModel.appRestartEvent.observe(this) {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            finishAffinity()
            startActivity(intent)
            exitProcess(0)
        }

        viewModel.restoreConfirmationDisplayEvent.observe(this) { lastBackupDate ->
            AlertDialog.Builder(this)
                .setTitle(R.string.backup_restore_confirmation_title)
                .setMessage(
                    getString(
                        R.string.backup_restore_confirmation_message,
                        lastBackupDate.formatLastBackupDate()
                    )
                )
                .setPositiveButton(R.string.backup_restore_confirmation_positive_cta) { _, _ ->
                    viewModel.onRestoreBackupConfirmationConfirmed()
                }
                .setNegativeButton(R.string.backup_restore_confirmation_negative_cta) { _, _ ->
                    viewModel.onRestoreBackupConfirmationCancelled()
                }
                .show()
        }

        viewModel.authenticationConfirmationDisplayEvent.observe(this) {
            AlertDialog.Builder(this)
                .setTitle(R.string.backup_settings_not_authenticated_privacy_title)
                .setMessage(R.string.backup_settings_not_authenticated_privacy_message)
                .setPositiveButton(R.string.backup_settings_not_authenticated_privacy_positive_cta) { _, _ ->
                    viewModel.onAuthenticationConfirmationConfirmed(this)
                }
                .setNegativeButton(R.string.backup_settings_not_authenticated_privacy_negative_cta) { _, _ ->
                    viewModel.onAuthenticationConfirmationCancelled()
                }
                .show()
        }

        viewModel.deleteConfirmationDisplayEvent.observe(this) {
            AlertDialog.Builder(this)
                .setTitle(R.string.backup_wipe_data_confirmation_title)
                .setMessage(R.string.backup_wipe_data_confirmation_message)
                .setPositiveButton(R.string.backup_wipe_data_confirmation_positive_cta) { _, _ ->
                    viewModel.onDeleteBackupConfirmationConfirmed()
                }
                .setNegativeButton(R.string.backup_wipe_data_confirmation_negative_cta) { _, _ ->
                    viewModel.onDeleteBackupConfirmationCancelled()
                }
                .show()
        }

        viewModel.backupDeletionErrorEvent.observe(this) {
            AlertDialog.Builder(this)
                .setTitle(R.string.backup_wipe_data_error_title)
                .setMessage(R.string.backup_wipe_data_error_message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }

        binding.backupSettingsCloudStorageAuthenticateButton.setOnClickListener {
            viewModel.onAuthenticateButtonPressed()
        }

        binding.backupSettingsCloudStorageLogoutButton.setOnClickListener {
            viewModel.onLogoutButtonPressed()
        }

        binding.backupSettingsCloudStorageBackupSwitch.setOnCheckedChangeListener { _, checked ->
            if( checked ) {
                viewModel.onBackupActivated()
            } else {
                viewModel.onBackupDeactivated()
            }
        }

        binding.backupSettingsCloudBackupCta.setOnClickListener {
            viewModel.onBackupNowButtonPressed()
        }

        binding.backupSettingsCloudRestoreCta.setOnClickListener {
            viewModel.onRestoreButtonPressed()
        }

        binding.backupSettingsCloudDeleteCta.setOnClickListener {
            viewModel.onDeleteBackupButtonPressed()
        }
    }

    private fun showLastUpdateDate(lastBackupDate: Date?) {
        binding.backupSettingsCloudLastUpdate.text = if( lastBackupDate != null ) {
            val timeFormatted = DateUtils.getRelativeDateTimeString(
                this,
                lastBackupDate.time,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                DateUtils.FORMAT_SHOW_TIME
            )

            getString(R.string.backup_last_update_date, timeFormatted)
        } else {
            getString(R.string.backup_last_update_date, getString(R.string.backup_last_update_date_never))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        viewModel.handleActivityResult(requestCode, resultCode, data)
    }

    private fun Date.formatLastBackupDate(): String {
        return DateUtils.formatDateTime(
            this@BackupSettingsActivity,
            this.time,
            DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
        )
    }
}
