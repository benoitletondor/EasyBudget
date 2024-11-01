package com.benoitletondor.easybudgetapp.view.main.subviews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.compose.AppTheme

@Composable
fun AlertMessageView(
    message: String,
) {
    var dismissed by remember { mutableStateOf(false) }

    if (!dismissed) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.budget_red))
                .padding(start = 16.dp, end = 5.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .weight(1f),
                text = message,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = { dismissed = true },
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White,
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        AlertMessageView(
            message = "This is a global alert message",
        )
    }
}

@Preview
@Composable
private fun LongPreview() {
    AppTheme {
        AlertMessageView(
            message = "This is a global alert message that is very long and very very long to see how it wraps with a lot of text that is overflowing 1 line",
        )
    }
}