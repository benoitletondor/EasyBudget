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

package com.benoitletondor.easybudgetapp.theme

import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.benoitletondor.easybudgetapp.R
import com.google.accompanist.themeadapter.material3.Mdc3Theme

val easyBudgetGreenColor = Color(0xFF00897B)
val easyBudgetGreenDarkColor = Color(0xFF00695C)

@Composable
fun AppTheme(
    content: @Composable () -> Unit,
) {
    return Mdc3Theme(
        context = ContextThemeWrapper(LocalContext.current, R.style.AppTheme),
        content = content,
    )
}