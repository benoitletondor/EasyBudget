package com.benoitletondor.easybudgetapp.view.settings.backup

import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.BaseActivity
import kotlinx.android.synthetic.main.activity_backup_settings.*
import org.koin.android.viewmodel.ext.android.viewModel
import java.util.*

class BackupSettingsActivity : BaseActivity() {

    private val viewModel: BackupSettingsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup_settings)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel.cloudBackupStateStream.observe(this, Observer{ cloudBackupState ->
            when(cloudBackupState) {
                BackupCloudStorageState.NotAuthenticated -> {
                    backup_settings_cloud_storage_not_authenticated_state.visibility = View.VISIBLE
                    backup_settings_cloud_storage_authenticating_state.visibility = View.GONE
                    backup_settings_cloud_storage_not_activated_state.visibility = View.GONE
                }
                BackupCloudStorageState.Authenticating -> {
                    backup_settings_cloud_storage_not_authenticated_state.visibility = View.GONE
                    backup_settings_cloud_storage_authenticating_state.visibility = View.VISIBLE
                    backup_settings_cloud_storage_not_activated_state.visibility = View.GONE
                }
                is BackupCloudStorageState.NotActivated -> {
                    backup_settings_cloud_storage_not_authenticated_state.visibility = View.GONE
                    backup_settings_cloud_storage_authenticating_state.visibility = View.GONE
                    backup_settings_cloud_storage_not_activated_state.visibility = View.VISIBLE

                    backup_settings_cloud_storage_email.text = cloudBackupState.currentUser.email
                    backup_settings_cloud_storage_logout_button.visibility = View.VISIBLE
                    backup_settings_cloud_storage_backup_switch.visibility = View.VISIBLE
                    backup_settings_cloud_storage_backup_switch.isChecked = false
                    backup_settings_cloud_last_update.visibility = View.GONE
                    backup_settings_cloud_backup_cta.visibility = View.GONE
                    backup_settings_cloud_backup_loading_progress.visibility = View.GONE
                }
                is BackupCloudStorageState.Activated -> {
                    backup_settings_cloud_storage_not_authenticated_state.visibility = View.GONE
                    backup_settings_cloud_storage_authenticating_state.visibility = View.GONE
                    backup_settings_cloud_storage_not_activated_state.visibility = View.VISIBLE

                    backup_settings_cloud_storage_email.text = cloudBackupState.currentUser.email
                    backup_settings_cloud_storage_logout_button.visibility = View.VISIBLE
                    backup_settings_cloud_storage_backup_switch.visibility = View.VISIBLE
                    backup_settings_cloud_storage_backup_switch.isChecked = true
                    showLastUpdateDate(cloudBackupState.lastBackupDate)
                    backup_settings_cloud_last_update.visibility = View.VISIBLE

                    if( cloudBackupState.backupNowAvailable ) {
                        backup_settings_cloud_backup_cta.visibility = View.VISIBLE
                    } else {
                        backup_settings_cloud_backup_cta.visibility = View.GONE
                    }
                    backup_settings_cloud_backup_loading_progress.visibility = View.GONE
                }
                is BackupCloudStorageState.BackupInProgress -> {
                    backup_settings_cloud_storage_not_authenticated_state.visibility = View.GONE
                    backup_settings_cloud_storage_authenticating_state.visibility = View.GONE
                    backup_settings_cloud_storage_not_activated_state.visibility = View.VISIBLE

                    backup_settings_cloud_storage_email.text = cloudBackupState.currentUser.email
                    backup_settings_cloud_storage_logout_button.visibility = View.GONE
                    backup_settings_cloud_storage_backup_switch.visibility = View.GONE
                    backup_settings_cloud_last_update.visibility = View.GONE
                    backup_settings_cloud_backup_cta.visibility = View.GONE
                    backup_settings_cloud_backup_loading_progress.visibility = View.VISIBLE
                }
            }
        })

        viewModel.backupNowErrorEvent.observe(this, Observer { backupError ->
            AlertDialog.Builder(this)
                .setTitle(R.string.backup_now_error_title)
                .setMessage(R.string.backup_now_error_message)
                .setPositiveButton(android.R.string.ok, null)
        })

        backup_settings_cloud_storage_authenticate_button.setOnClickListener {
            viewModel.onAuthenticateButtonPressed(this)
        }

        backup_settings_cloud_storage_logout_button.setOnClickListener {
            viewModel.onLogoutButtonPressed()
        }

        backup_settings_cloud_storage_backup_switch.setOnCheckedChangeListener { _, checked ->
            if( checked ) {
                viewModel.onBackupActivated()
            } else {
                viewModel.onBackupDeactivated()
            }
        }

        backup_settings_cloud_backup_cta.setOnClickListener {
            viewModel.onBackupNowButtonPressed()
        }
    }

    private fun showLastUpdateDate(lastBackupDate: Date?) {
        backup_settings_cloud_last_update.text = if( lastBackupDate != null ) {
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
}
