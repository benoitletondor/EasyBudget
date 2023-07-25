package com.benoitletondor.easybudgetapp.view.main.createaccount

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.databinding.ActivityCreateAccountBinding
import com.benoitletondor.easybudgetapp.helper.BaseActivity
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.theme.AppTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateAccountActivity : BaseActivity<ActivityCreateAccountBinding>() {
    private val viewModel: CreateAccountViewModel by viewModels()

    override fun createBinding(): ActivityCreateAccountBinding = ActivityCreateAccountBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.createAccountComposeView.setContent {
            AppTheme {
                val state by viewModel.stateFlow.collectAsState()

                ContentView(
                    state = state,
                )
            }
        }

        lifecycleScope.launchCollect(viewModel.eventFlow) { event ->
            when(event) {
                CreateAccountViewModel.Event.Finish -> finish()
                is CreateAccountViewModel.Event.ErrorWhileCreatingAccount -> {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.account_creation_success_error_title)
                        .setMessage(getString(R.string.account_creation_success_error_message, event.error.localizedMessage))
                        .setPositiveButton(R.string.ok) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
                CreateAccountViewModel.Event.SuccessCreatingAccount -> Toast.makeText(
                    this,
                    R.string.account_creation_success_toast,
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}

@Composable
private fun ContentView(
    state: CreateAccountViewModel.State,
) {

}
