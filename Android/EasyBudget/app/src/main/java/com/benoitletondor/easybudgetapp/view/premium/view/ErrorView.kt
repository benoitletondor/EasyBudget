package com.benoitletondor.easybudgetapp.view.premium.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.theme.AppTheme
import com.benoitletondor.easybudgetapp.theme.easyBudgetGreenColor

@Composable
fun ErrorView(
    onRetryButtonPressed: () -> Unit,
    onCloseButtonPressed: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.premium_screen_error_loading_title),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            fontSize = 16.sp,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.premium_screen_error_loading_message),
            color = Color.White,
            fontSize = 16.sp,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = onRetryButtonPressed,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
        ) {
            Text(stringResource(R.string.manage_account_error_cta))
        }

        Spacer(modifier = Modifier.height(50.dp))

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = onCloseButtonPressed,
        ) {
            Text(stringResource(R.string.premium_screen_error_close_cta))
        }
    }
}

@Composable
@Preview(showSystemUi = true)
private fun Preview() {
    AppTheme {
        Box(
            modifier = Modifier
                .background(easyBudgetGreenColor)
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            ErrorView(
                onRetryButtonPressed = {},
                onCloseButtonPressed = {},
            )
        }
    }
}