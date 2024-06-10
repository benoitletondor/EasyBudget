package com.benoitletondor.easybudgetapp.view.main.subviews

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.view.main.MainViewModel
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SelectedAccountHeader(
    selectedAccount: MainViewModel.SelectedAccount.Selected,
    hasPendingInvitationsFlow: StateFlow<Boolean>,
    onCurrentAccountTapped: () -> Unit,
) {
    val hasPendingInvitations by hasPendingInvitationsFlow.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.status_bar_color))
            .padding(bottom = 8.dp)
            .clickable(onClick = onCurrentAccountTapped)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.main_account_name) + " ",
                        fontWeight = FontWeight.SemiBold,
                        color = colorResource(R.color.action_bar_text_color),
                    )

                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = when(selectedAccount) {
                            MainViewModel.SelectedAccount.Selected.Offline -> stringResource(R.string.main_account_default_name)
                            is MainViewModel.SelectedAccount.Selected.Online -> stringResource(R.string.main_account_online_name, selectedAccount.name)
                        },
                        maxLines = 1,
                        color = colorResource(R.color.action_bar_text_color),
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (hasPendingInvitations) {
                Box(
                    modifier = Modifier.padding(start = 16.dp, end = 6.dp),
                ){
                    Image(
                        painter = painterResource(id = R.drawable.ic_baseline_notifications_24),
                        colorFilter = ColorFilter.tint(colorResource(R.color.action_bar_text_color)),
                        contentDescription = stringResource(R.string.account_pending_invitation_description),
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(colorResource(R.color.budget_red))
                            .align(Alignment.TopEnd)
                    )
                }
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_baseline_arrow_drop_down_24),
                    colorFilter = ColorFilter.tint(colorResource(R.color.action_bar_text_color)),
                    contentDescription = null,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }

    }
}