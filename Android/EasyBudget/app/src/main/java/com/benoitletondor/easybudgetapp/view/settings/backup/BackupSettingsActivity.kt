package com.benoitletondor.easybudgetapp.view.settings.backup

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.BaseActivity
import kotlinx.android.synthetic.main.activity_backup_settings.*
import org.koin.android.viewmodel.ext.android.viewModel

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
                }
            }
        })

        backup_settings_cloud_storage_authenticate_button.setOnClickListener {
            viewModel.onAuthenticateButtonPressed(this)
        }

        backup_settings_cloud_storage_logout_button.setOnClickListener {
            viewModel.onLogoutButtonPressed()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        viewModel.handleActivityResult(requestCode, resultCode, data)
    }
}
