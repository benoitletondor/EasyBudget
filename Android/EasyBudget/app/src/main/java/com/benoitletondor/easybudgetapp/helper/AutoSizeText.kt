/*
 *   Copyright 2023 Benoit LETONDOR
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

package com.benoitletondor.easybudgetapp.helper

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.InternalFoundationTextApi
import androidx.compose.foundation.text.TextDelegate
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.reflect.KProperty

// Taken from https://gist.github.com/inidamleader/b594d35362ebcf3cedf81055df519300
/**
 * Composable function that automatically adjusts the text size to fit within given constraints, considering the ratio of line spacing to text size.
 *
 * Features:
 *  1. Best performance: Utilizes a dichotomous binary search algorithm for swift and optimal text size determination without unnecessary iterations.
 *  2. Alignment support: Supports six possible alignment values via the Alignment interface.
 *  3. Material Design 3 support.
 *  4. Font scaling support: User-initiated font scaling doesn't affect the visual rendering output.
 *  5. Multiline Support with maxLines Parameter.
 *
 * Limitations:
 *  1. MinLine is set to 1 under the hood and cannot be changed.
 *
 * @param text The text to be displayed.
 * @param modifier The modifier for the text composable.
 * @param suggestedFontSizes The suggested font sizes to choose from.
 * @param minTextSize The minimum text size allowed.
 * @param maxTextSize The maximum text size allowed.
 * @param stepGranularityTextSize The step size for adjusting the text size.
 * @param alignment The alignment of the text within its container.
 * @param color The color of the text.
 * @param fontStyle The font style of the text.
 * @param fontWeight The font weight of the text.
 * @param fontFamily The font family of the text.
 * @param letterSpacing The letter spacing of the text.
 * @param textDecoration The text decoration style.
 * @param lineSpacingRatio The ratio of line spacing to text size.
 * @param maxLines The maximum number of lines for the text.
 * @param onTextLayout Callback invoked when the text layout is available.
 * @param style The base style to apply to the text.
 * @author Reda El Madini - For support, contact gladiatorkilo@gmail.com
 */
@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    suggestedFontSizes: ImmutableWrapper<List<TextUnit>> = emptyList<TextUnit>().toImmutableWrapper(),
    minTextSize: TextUnit = TextUnit.Unspecified,
    maxTextSize: TextUnit = TextUnit.Unspecified,
    stepGranularityTextSize: TextUnit = TextUnit.Unspecified,
    alignment: Alignment = Alignment.TopStart,
    color: Color = Color.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    lineSpacingRatio: Float = 0.1F,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
) {
    AutoSizeText(
        text = AnnotatedString(text),
        modifier = modifier,
        suggestedFontSizes = suggestedFontSizes,
        minTextSize = minTextSize,
        maxTextSize = maxTextSize,
        stepGranularityTextSize = stepGranularityTextSize,
        alignment = alignment,
        color = color,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        lineSpacingRatio = lineSpacingRatio,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = style,
    )
}

/**
 * Composable function that automatically adjusts the text size to fit within given constraints using AnnotatedString, considering the ratio of line spacing to text size.
 *
 * Features:
 *  Similar to AutoSizeText(String), with support for AnnotatedString.
 *
 * @see AutoSizeText
 */
@Composable
fun AutoSizeText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    suggestedFontSizes: ImmutableWrapper<List<TextUnit>> = emptyList<TextUnit>().toImmutableWrapper(),
    minTextSize: TextUnit = TextUnit.Unspecified,
    maxTextSize: TextUnit = TextUnit.Unspecified,
    stepGranularityTextSize: TextUnit = TextUnit.Unspecified,
    alignment: Alignment = Alignment.TopStart,
    color: Color = Color.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    lineSpacingRatio: Float = 0.1F,
    maxLines: Int = Int.MAX_VALUE,
    inlineContent: ImmutableWrapper<Map<String, InlineTextContent>> = mapOf<String, InlineTextContent>().toImmutableWrapper(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
) {
    val permittedTextUnitTypes = remember { listOf(TextUnitType.Unspecified, TextUnitType.Sp) }
    check(minTextSize.type in permittedTextUnitTypes)
    check(maxTextSize.type in permittedTextUnitTypes)
    check(stepGranularityTextSize.type in permittedTextUnitTypes)

    val density = LocalDensity.current
    // Change font scale to 1F
    CompositionLocalProvider(
        LocalDensity provides Density(density = density.density, fontScale = 1F)
    ) {
        BoxWithConstraints(
            modifier = modifier,
            contentAlignment = alignment,
        ) {
            val step = remember(stepGranularityTextSize) {
                // (density.fontScale / density.density).sp represents 1px when font scale equals 1
                (density.fontScale / density.density).let {
                    if (stepGranularityTextSize.isUnspecified) it
                    else stepGranularityTextSize.value.coerceAtLeast(minimumValue = it)
                }
            }

            val max = remember(maxWidth, maxHeight, maxTextSize) {
                min(maxWidth, maxHeight).value.let {
                    if (maxTextSize.isUnspecified) it
                    else maxTextSize.value.coerceAtMost(maximumValue = it)
                }
            }

            val min = remember(minTextSize, step, max) {
                if (minTextSize.isUnspecified) step
                else minTextSize.value.coerceIn(step..max)
            }

            val possibleFontSizes = remember(suggestedFontSizes, min, max, step) {
                suggestedFontSizes.value.filter {
                    it.isSp && it.value in min..max
                }.sortedByDescending { it.value }.takeIf { it.isNotEmpty() } ?: kotlin.run {
                    val firstIndex = ceil(min / step).toInt()
                    val lastIndex = floor(max / step).toInt()
                    MutableList(size = lastIndex - firstIndex + 1) { index ->
                        (step * (lastIndex - index)).sp
                    }
                }
            }

            var combinedTextStyle = (LocalTextStyle.current + style).copy(
                platformStyle = PlatformTextStyle(
                    includeFontPadding = true,
                ),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both,
                ),
            )

            if (possibleFontSizes.isNotEmpty()) {
                // Para-Dichotomous binary search
                var low = 0
                var high = possibleFontSizes.lastIndex
                while (low <= high) {
                    val mid = low + (high - low) / 2
                    val fontSize = possibleFontSizes[mid]
                    val shouldShrink = shouldShrink(
                        text = text,
                        textStyle = combinedTextStyle.copy(
                            fontSize = fontSize,
                            lineHeight = fontSize * (1 + lineSpacingRatio),
                        ),
                        maxLines = maxLines,
                    )

                    if (shouldShrink) low = mid + 1
                    else high = mid - 1
                }
                val electedFontSize = possibleFontSizes[low.coerceIn(possibleFontSizes.indices)]
                combinedTextStyle = combinedTextStyle.copy(
                    fontSize = electedFontSize,
                    lineHeight = electedFontSize * (1 + lineSpacingRatio),
                )
            }

            Text(
                text = text,
                modifier = Modifier,
                color = color,
                fontSize = TextUnit.Unspecified,
                fontStyle = fontStyle,
                fontWeight = fontWeight,
                fontFamily = fontFamily,
                letterSpacing = letterSpacing,
                textDecoration = textDecoration,
                textAlign = when (alignment) {
                    Alignment.TopStart, Alignment.CenterStart, Alignment.BottomStart -> TextAlign.Start
                    Alignment.TopCenter, Alignment.Center, Alignment.BottomCenter -> TextAlign.Center
                    else -> TextAlign.End // Same as:
                    // Alignment.TopEnd, Alignment.CenterEnd, Alignment.BottomEnd -> TextAlign.End
                },
                overflow = TextOverflow.Visible,
                maxLines = maxLines,
                minLines = 1,
                inlineContent = inlineContent.value,
                onTextLayout = onTextLayout,
                style = combinedTextStyle,
                softWrap = true,
            )
        }
    }
}

@OptIn(InternalFoundationTextApi::class)
@Composable
private fun BoxWithConstraintsScope.shouldShrink(
    text: AnnotatedString,
    textStyle: TextStyle,
    maxLines: Int,
): Boolean {
    val textDelegate = TextDelegate(
        text = text,
        style = textStyle,
        maxLines = maxLines,
        minLines = 1,
        softWrap = true,
        overflow = TextOverflow.Visible,
        density = LocalDensity.current,
        fontFamilyResolver = LocalFontFamilyResolver.current,
    )

    val textLayoutResult = textDelegate.layout(
        constraints = constraints,
        layoutDirection = LocalLayoutDirection.current,
    )

    return textLayoutResult.hasVisualOverflow
}

@Immutable
data class ImmutableWrapper<T>(val value: T)

/**
 * May hold null value
 */
fun <T> T.toImmutableWrapper() = ImmutableWrapper(this)

operator fun <T> ImmutableWrapper<T>.getValue(thisRef: Any?, property: KProperty<*>) = value

@Preview(widthDp = 200, heightDp = 100)
@Preview(widthDp = 200, heightDp = 30)
@Preview(widthDp = 60, heightDp = 30)
@Composable
fun PreviewAutoSizeText1() {
    MaterialTheme {
        Surface {
            AutoSizeText(
                text = "OvTracker Android App",
                modifier = Modifier.fillMaxSize(),
                alignment = Alignment.CenterStart,
            )
        }
    }
}

@Preview(widthDp = 200, heightDp = 100)
@Preview(widthDp = 200, heightDp = 30)
@Preview(widthDp = 60, heightDp = 30)
@Composable
fun PreviewAutoSizeText2() {
    MaterialTheme {
        Surface {
            AutoSizeText(
                text = "OvTracker Android App",
                modifier = Modifier.fillMaxSize(),
                suggestedFontSizes = listOf(
                    1.sp, 4.sp, 5.sp, 2.sp, 10.sp, 12.sp, 14.sp, 18.sp
                ).toImmutableWrapper(),
                alignment = Alignment.CenterStart,
                maxLines = 1,
            )
        }
    }
}