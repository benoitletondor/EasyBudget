/*
 *   Copyright 2025 Benoit Letondor
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
package com.benoitletondor.easybudgetapp.view.settings.subviews

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.AppTheme

@Composable
fun ThemePickerDialog(
    contentPadding: PaddingValues,
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onDismissRequest: () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(R.color.window_background),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(top = 14.dp, bottom = 10.dp),
                    text = stringResource(R.string.setting_category_theme_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.primary_text),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onThemeSelected(AppTheme.LIGHT) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ){
                    RadioButton(
                        selected = currentTheme == AppTheme.LIGHT,
                        onClick = {
                            onThemeSelected(AppTheme.LIGHT)
                        },
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.setting_theme_light),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onThemeSelected(AppTheme.DARK) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ){
                    RadioButton(
                        selected = currentTheme == AppTheme.DARK,
                        onClick = {
                            onThemeSelected(AppTheme.DARK)
                        },
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.setting_theme_dark),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onThemeSelected(AppTheme.PLATFORM_DEFAULT) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ){
                    RadioButton(
                        selected = currentTheme == AppTheme.PLATFORM_DEFAULT,
                        onClick = {
                            onThemeSelected(AppTheme.PLATFORM_DEFAULT)
                        },
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.setting_theme_platform),
                    )
                }
            }
        }
    }
}