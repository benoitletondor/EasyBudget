package com.benoitletondor.easybudgetapp.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.benoitletondor.easybudgetapp.R

sealed class BackButtonBehavior {
    data object Hidden : BackButtonBehavior()
    data class NavigateBack(val onBackButtonPressed: () -> Unit) : BackButtonBehavior()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopAppBar(
    title: String,
    backButtonBehavior: BackButtonBehavior,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text = title,
            )
        },
        actions = actions,
        navigationIcon = {
            if (backButtonBehavior is BackButtonBehavior.NavigateBack) {
                IconButton(onClick = backButtonBehavior.onBackButtonPressed) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Up button",
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colorResource(id = R.color.action_bar_background),
            titleContentColor = colorResource(id = R.color.action_bar_text_color),
            actionIconContentColor = colorResource(id = R.color.action_bar_text_color),
            navigationIconContentColor = colorResource(id = R.color.action_bar_text_color),
        ),
    )
}

@Composable
fun AppTopBarMoreMenuItem(content: @Composable ColumnScope.() -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .clickable { showMenu = true }
            .padding(all = 8.dp),
    ) {
        Icon(
            Icons.Default.MoreVert,
            contentDescription = "Menu",
        )
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            content = content,
        )
    }
}