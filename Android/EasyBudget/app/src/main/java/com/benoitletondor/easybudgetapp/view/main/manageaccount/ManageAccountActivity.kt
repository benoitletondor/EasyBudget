package com.benoitletondor.easybudgetapp.view.main.manageaccount

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.ContentView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.lifecycleScope
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.accounts.model.Invitation
import com.benoitletondor.easybudgetapp.accounts.model.InvitationStatus
import com.benoitletondor.easybudgetapp.auth.CurrentUser
import com.benoitletondor.easybudgetapp.databinding.ActivityCreateAccountBinding
import com.benoitletondor.easybudgetapp.helper.BaseActivity
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.theme.AppTheme
import com.benoitletondor.easybudgetapp.view.main.MainViewModel
import com.benoitletondor.easybudgetapp.view.main.manageaccount.view.ContentView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ManageAccountActivity : BaseActivity<ActivityCreateAccountBinding>() {
    private val viewModel: ManageAccountViewModel by viewModels()

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
                    onUpdateAccountNameClicked = viewModel::onUpdateAccountNameClicked,
                    onInvitationDeleteConfirmed = viewModel::onInvitationDeleteConfirmed,
                    onRetryButtonClicked = viewModel::onRetryButtonClicked,
                    onLeaveAccountConfirmed = viewModel::onLeaveAccountConfirmed,
                )
            }
        }

        lifecycleScope.launchCollect(viewModel.eventFlow) { event ->

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

    companion object {
        const val SELECTED_ACCOUNT_EXTRA = "selectedAccount"
        
        fun newIntent(
            context: Context,
            selectedAccount: MainViewModel.SelectedAccount.Selected.Online,
        ): Intent {
            return Intent(context, ManageAccountActivity::class.java).apply {
                putExtra(SELECTED_ACCOUNT_EXTRA, selectedAccount)
            }
        }
    }
}

enum class LoadingKind {
    LOADING_DATA,
    DELETING_INVITATION,
    SENDING_INVITATION,
    UPDATING_NAME,
}

