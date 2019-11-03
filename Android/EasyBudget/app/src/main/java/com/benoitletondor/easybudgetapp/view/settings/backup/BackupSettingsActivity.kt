package com.benoitletondor.easybudgetapp.view.settings.backup

import android.os.Bundle
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.BaseActivity
import org.koin.android.viewmodel.ext.android.viewModel

class BackupSettingsActivity : BaseActivity() {

    private val viewModel: BackupSettingsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup_settings)
    }
}
