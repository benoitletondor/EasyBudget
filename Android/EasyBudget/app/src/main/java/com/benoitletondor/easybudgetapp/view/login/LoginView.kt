package com.benoitletondor.easybudgetapp.view.login

import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.auth.CurrentUser
import com.benoitletondor.easybudgetapp.compose.AppTheme
import com.benoitletondor.easybudgetapp.compose.AppWithTopAppBarScaffold
import com.benoitletondor.easybudgetapp.compose.BackButtonBehavior
import com.benoitletondor.easybudgetapp.compose.components.LoadingView
import com.benoitletondor.easybudgetapp.helper.launchCollect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class LoginDestination(val shouldDismissAfterAuth: Boolean)

@Composable
fun LoginView(
    viewModel: LoginViewModel,
    navigateUp: () -> Unit,
    finish: () -> Unit,
) {
    LoginView(
        stateFlow = viewModel.stateFlow,
        eventFlow = viewModel.eventFlow,
        navigateUp = navigateUp,
        onLogoutButtonPressed = viewModel::onLogoutButtonClicked,
        onFinishButtonPressed = viewModel::onFinishButtonPressed,
        onLoginButtonPressed = viewModel::onAuthenticatedButtonClicked,
        onAuthActivityResult = {
            viewModel.handleAuthActivityResult(it.resultCode, it.data)
        },
        finish = finish,
    )
}

@Composable
private fun LoginView(
    stateFlow: StateFlow<LoginViewModel.State>,
    eventFlow: Flow<LoginViewModel.Event>,
    navigateUp: () -> Unit,
    onLogoutButtonPressed: () -> Unit,
    onFinishButtonPressed: () -> Unit,
    onLoginButtonPressed: (ManagedActivityResultLauncher<Intent, ActivityResult>) -> Unit,
    onAuthActivityResult: (ActivityResult) -> Unit,
    finish: () -> Unit,
) {
    val authActivityLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        onAuthActivityResult(result)
    }

    LaunchedEffect(key1 = "eventsListener") {
        launchCollect(eventFlow) { event ->
            when(event) {
                LoginViewModel.Event.Finish -> finish()
            }
        }
    }

    AppWithTopAppBarScaffold(
        title = stringResource(R.string.title_activity_login),
        backButtonBehavior = BackButtonBehavior.NavigateBack(
            onBackButtonPressed = navigateUp,
        ),
        content = { contentPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                val state by stateFlow.collectAsState()

                when(val currentState = state) {
                    is LoginViewModel.State.Authenticated -> AuthenticatedView(
                        currentUser = currentState.user,
                        onLogoutButtonPressed = onLogoutButtonPressed,
                        onFinishButtonPressed = onFinishButtonPressed,
                    )
                    LoginViewModel.State.Loading -> LoadingView()
                    LoginViewModel.State.NotAuthenticated -> NotAuthenticatedView(
                        onLoginButtonPressed = {
                            onLoginButtonPressed(authActivityLauncher)
                        },
                    )
                }
            }
        },
    )
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

@Preview(name = "Loading preview", showSystemUi = true)
@Composable
private fun LoadingPreview() {
    AppTheme {
        LoginView(
            stateFlow = MutableStateFlow(LoginViewModel.State.Loading),
            eventFlow = MutableSharedFlow(),
            navigateUp = {},
            onLogoutButtonPressed = {},
            onFinishButtonPressed = {},
            onLoginButtonPressed = {},
            onAuthActivityResult = {},
            finish = {},
        )
    }
}

@Preview(name = "Authenticated preview", showSystemUi = true)
@Composable
private fun AuthenticatedPreview() {
    AppTheme {
        LoginView(
            stateFlow = MutableStateFlow(LoginViewModel.State.Authenticated(
                user = CurrentUser(
                    id = "",
                    email = "test@login.com",
                    token = "",
                )
            )),
            eventFlow = MutableSharedFlow(),
            navigateUp = {},
            onLogoutButtonPressed = {},
            onFinishButtonPressed = {},
            onLoginButtonPressed = {},
            onAuthActivityResult = {},
            finish = {},
        )
    }
}

@Preview(name = "Not authenticated preview", showSystemUi = true)
@Composable
private fun NotAuthenticatedPreview() {
    AppTheme {
        LoginView(
            stateFlow = MutableStateFlow(LoginViewModel.State.NotAuthenticated),
            eventFlow = MutableSharedFlow(),
            navigateUp = {},
            onLogoutButtonPressed = {},
            onFinishButtonPressed = {},
            onLoginButtonPressed = {},
            onAuthActivityResult = {},
            finish = {},
        )
    }
}