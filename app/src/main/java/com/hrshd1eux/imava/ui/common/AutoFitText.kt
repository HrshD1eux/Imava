package com.hrshd1eux.imava.ui.common

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/**
 * High-performance, zero-flicker auto-scaling text component.
 * Dynamically scales down font size until the text fits within its layout constraints,
 * falling back to ellipsis only when [minFontSize] is reached.
 */
@Composable
fun AutoFitText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = 1,
    minFontSize: TextUnit = 9.sp,
    stepSize: TextUnit = 0.5.sp,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val mergedStyle = if (fontWeight != null) style.copy(fontWeight = fontWeight) else style

    BoxWithConstraints(modifier = modifier) {
        val targetFontSize = if (mergedStyle.fontSize.isSpecified) mergedStyle.fontSize else 14.sp
        val maxPxWidth = with(density) { maxWidth.toPx().toInt() }
        val maxPxHeight = with(density) { maxHeight.toPx().toInt() }

        val optimalFontSize = remember(text, mergedStyle, maxPxWidth, maxPxHeight, targetFontSize, minFontSize, stepSize) {
            if (maxPxWidth <= 0) return@remember targetFontSize

            var currentSp = targetFontSize.value
            val minSp = minFontSize.value
            val step = stepSize.value.coerceAtLeast(0.25f)

            while (currentSp > minSp) {
                val candidateStyle = mergedStyle.copy(fontSize = currentSp.sp)
                val measureConstraints = Constraints(
                    maxWidth = maxPxWidth,
                    maxHeight = if (maxPxHeight > 0) maxPxHeight else Constraints.Infinity
                )
                val layoutResult = textMeasurer.measure(
                    text = text,
                    style = candidateStyle,
                    maxLines = maxLines,
                    overflow = overflow,
                    constraints = measureConstraints
                )

                if (!layoutResult.hasVisualOverflow) {
                    return@remember currentSp.sp
                }
                currentSp -= step
            }
            minSp.sp
        }

        Text(
            text = text,
            style = mergedStyle.copy(fontSize = optimalFontSize),
            color = color,
            maxLines = maxLines,
            softWrap = maxLines > 1,
            textAlign = textAlign,
            overflow = overflow
        )
    }
}
