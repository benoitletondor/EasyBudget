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

package com.benoitletondor.easybudgetapp.view.main.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.auth.CurrentUser
import com.benoitletondor.easybudgetapp.databinding.ActivityLoginBinding
import com.benoitletondor.easybudgetapp.helper.BaseActivity
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : BaseActivity<ActivityLoginBinding>() {
    private val viewModel: LoginViewModel by viewModels()

    override fun createBinding(): ActivityLoginBinding = ActivityLoginBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.loginComposeView.setContent {
            AppTheme {
                val state by viewModel.stateFlow.collectAsState()

                ContentView(
                    state = state,
                    onLogoutButtonPressed = viewModel::onLogoutButtonClicked,
                    onFinishButtonPressed = viewModel::onFinishButtonPressed,
                    onLoginButtonPressed = {
                        viewModel.onAuthenticatedButtonClicked(this)
                    }
                )
            }
        }

        lifecycleScope.launchCollect(viewModel.eventFlow) { event ->
            when(event) {
                LoginViewModel.Event.Finish -> finish()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        viewModel.handleActivityResult(requestCode, resultCode, data)
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
        const val SHOULD_DISMISS_AFTER_AUTH_EXTRA = "shouldDismissAfterAuth"

        fun newIntent(context: Context, shouldDismissAfterAuth: Boolean): Intent {
            return Intent(context, LoginActivity::class.java).apply {
                putExtra(SHOULD_DISMISS_AFTER_AUTH_EXTRA, shouldDismissAfterAuth)
            }
        }
    }
}

@Composable
private fun ContentView(
    state: LoginViewModel.State,
    onLogoutButtonPressed: () -> Unit,
    onFinishButtonPressed: () -> Unit,
    onLoginButtonPressed: () -> Unit,
) {
    when(state) {
        is LoginViewModel.State.Authenticated -> AuthenticatedView(
            currentUser = state.user,
            onLogoutButtonPressed = onLogoutButtonPressed,
            onFinishButtonPressed = onFinishButtonPressed,
        )
        LoginViewModel.State.Loading -> LoadingView()
        LoginViewModel.State.NotAuthenticated -> NotAuthenticatedView(
            onLoginButtonPressed = onLoginButtonPressed,
        )
    }
}

@Composable
private fun NotAuthenticatedView(
    onLoginButtonPressed: () -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 20.dp, horizontal = 16.dp),
    ){
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.login_not_auth_title),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.login_not_auth_desc),
            fontSize = 16.sp,
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.login_not_auth_desc_2),
            fontSize = 16.sp,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = onLoginButtonPressed,
        ) {
            Text(stringResource(R.string.login_not_auth_cta))
        }
    }
}

@Composable
private fun AuthenticatedView(
    currentUser: CurrentUser,
    onLogoutButtonPressed: () -> Unit,
    onFinishButtonPressed: () -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 20.dp, horizontal = 16.dp),
    ){
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.login_auth_title),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = currentUser.email,
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.login_auth_desc),
            fontSize = 16.sp,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = onFinishButtonPressed,
        ) {
            Text(stringResource(R.string.ok))
        }

        Spacer(modifier = Modifier.height(60.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.login_auth_logout_title),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.login_auth_logout_desc),
            fontSize = 16.sp,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = onLogoutButtonPressed,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(
                text = stringResource(R.string.login_auth_logout_cta),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun LoadingView() {
    Box {
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Preview(name = "Loading preview", showSystemUi = true)
@Composable
private fun LoadingPreview() {
    AppTheme {
        ContentView(
            state = LoginViewModel.State.Loading,
            onLogoutButtonPressed = {},
            onFinishButtonPressed = {},
            onLoginButtonPressed = {},
        )
    }
}

@Preview(name = "Authenticated preview", showSystemUi = true)
@Composable
private fun AuthenticatedPreview() {
    AppTheme {
        ContentView(
            state = LoginViewModel.State.Authenticated(
                user = CurrentUser(
                    id = "",
                    email = "test@login.com",
                    token = "",
                )
            ),
            onLogoutButtonPressed = {},
            onFinishButtonPressed = {},
            onLoginButtonPressed = {},
        )
    }
}

@Preview(name = "Not authenticated preview", showSystemUi = true)
@Composable
private fun NotAuthenticatedPreview() {
    AppTheme {
        ContentView(
            state = LoginViewModel.State.NotAuthenticated,
            onLogoutButtonPressed = {},
            onFinishButtonPressed = {},
            onLoginButtonPressed = {},
        )
    }
}