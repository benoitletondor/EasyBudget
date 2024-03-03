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
package com.benoitletondor.easybudgetapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.benoitletondor.easybudgetapp.R

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
            imageVector = Icons.Default.MoreVert,
            contentDescription = "3 dots menu",
            tint = colorResource(R.color.action_bar_text_color),
        )
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            content = content,
        )
    }
}