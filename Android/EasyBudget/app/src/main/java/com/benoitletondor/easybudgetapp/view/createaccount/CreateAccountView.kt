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
package com.benoitletondor.easybudgetapp.view.createaccount

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.auth.CurrentUser
import com.benoitletondor.easybudgetapp.compose.AppTheme
import com.benoitletondor.easybudgetapp.compose.AppWithTopAppBarScaffold
import com.benoitletondor.easybudgetapp.compose.BackButtonBehavior
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
object CreateAccountDestination

@Composable
fun CreateAccountView(
    viewModel: CreateAccountViewModel = hiltViewModel(),
    navigateUp: () -> Unit,
    finish: () -> Unit,
) {
    CreateAccountView(
        stateFlow = viewModel.stateFlow,
        eventFlow = viewModel.eventFlow,
        navigateUp = navigateUp,
        finish = finish,
        onFinishButtonClicked = viewModel::onFinishButtonClicked,
        onCreateAccountClicked = viewModel::onCreateAccountButtonPressed,
    )
}

@Composable
private fun CreateAccountView(
    stateFlow: StateFlow<CreateAccountViewModel.State>,
    eventFlow: Flow<CreateAccountViewModel.Event>,
    navigateUp: () -> Unit,
    finish: () -> Unit,
    onFinishButtonClicked: () -> Unit,
    onCreateAccountClicked: (String) -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(key1 = "eventsListener") {
        launchCollect(eventFlow) { event ->
            when(event) {
                CreateAccountViewModel.Event.Finish -> finish()
                is CreateAccountViewModel.Event.ErrorWhileCreatingAccount -> {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.account_creation_success_error_title)
                        .setMessage(context.getString(R.string.account_creation_success_error_message, event.error.localizedMessage))
                        .setPositiveButton(R.string.ok) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
                CreateAccountViewModel.Event.SuccessCreatingAccount -> Toast.makeText(
                    context,
                    R.string.account_creation_success_toast,
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    AppWithTopAppBarScaffold(
        title = stringResource(R.string.title_activity_create_account),
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
                    is CreateAccountViewModel.State.Creating -> LoadingView(isCreating = true)
                    CreateAccountViewModel.State.Loading -> LoadingView(isCreating = false)
                    CreateAccountViewModel.State.NotAuthenticatedError -> NotAuthenticatedView(
                        onFinishButtonClicked = onFinishButtonClicked,
                    )
                    is CreateAccountViewModel.State.Ready -> CreateAccountView(
                        initialNameValue = currentState.initialNameValue,
                        onCreateAccountClicked = onCreateAccountClicked,
                    )
                }
            }
        },
    )
}

@Composable
private fun CreateAccountView(
    initialNameValue: String,
    onCreateAccountClicked: (String) -> Unit,
) {
    var accountName by remember { mutableStateOf(initialNameValue) }
    val isValid = accountName.length in 1..49
    val shouldDisplayError = accountName.length >= 50

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.create_account_account_name_title),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(10.dp))

        TextField(
            value = accountName,
            onValueChange = { newValue ->
                accountName = newValue
            },
            placeholder = {
                Text(stringResource(R.string.create_account_account_name_placeholder))
            },
            isError = shouldDisplayError,
            supportingText = {
                if (shouldDisplayError) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.create_account_account_name_too_large_error),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier.align(Alignment.End),
            onClick = {
                if (isValid) {
                    onCreateAccountClicked(accountName)
                }
            },
            enabled = isValid,
        ) {
            Text(stringResource(R.string.create_account_create_cta))
        }
    }
}

@Composable
private fun NotAuthenticatedView(
    onFinishButtonClicked: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 20.dp),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = stringResource(R.string.create_account_auth_error_title),
            fontSize = 18.sp,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = stringResource(R.string.create_account_auth_error_desc),
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onFinishButtonClicked,
        ) {
            Text(stringResource(R.string.create_account_go_back_cta))
        }
    }
}

@Composable
private fun LoadingView(
    isCreating: Boolean,
) {
    com.benoitletondor.easybudgetapp.compose.components.LoadingView(
        loadingText = if (isCreating) stringResource(R.string.create_account_creating_placeholder) else null
    )
}

@Composable
@Preview(name = "Loading state preview", showSystemUi = true)
private fun LoadingStatePreview() {
    AppTheme {
        CreateAccountView(
            stateFlow = MutableStateFlow(CreateAccountViewModel.State.Loading),
            eventFlow = MutableSharedFlow(),
            navigateUp = {},
            finish = {},
            onFinishButtonClicked = {},
            onCreateAccountClicked = {},
        )
    }
}

@Composable
@Preview(name = "Creating state preview", showSystemUi = true)
private fun CreatingStatePreview() {
    AppTheme {
        CreateAccountView(
            stateFlow = MutableStateFlow(CreateAccountViewModel.State.Creating(currentUser = CurrentUser("", "", ""))),
            eventFlow = MutableSharedFlow(),
            navigateUp = {},
            finish = {},
            onFinishButtonClicked = {},
            onCreateAccountClicked = {},
        )
    }
}

@Composable
@Preview(name = "Not authenticated preview", showSystemUi = true)
private fun NotAuthenticatedStatePreview() {
    AppTheme {
        CreateAccountView(
            stateFlow = MutableStateFlow(CreateAccountViewModel.State.NotAuthenticatedError),
            eventFlow = MutableSharedFlow(),
            navigateUp = {},
            finish = {},
            onFinishButtonClicked = {},
            onCreateAccountClicked = {},
        )
    }
}

@Composable
@Preview(name = "Ready preview", showSystemUi = true)
private fun ReadyStatePreview() {
    AppTheme {
        CreateAccountView(
            stateFlow = MutableStateFlow(CreateAccountViewModel.State.Ready(
                initialNameValue = "",
                currentUser = CurrentUser("", "", ""),
            )),
            eventFlow = MutableSharedFlow(),
            navigateUp = {},
            finish = {},
            onFinishButtonClicked = {},
            onCreateAccountClicked = {},
        )
    }
}