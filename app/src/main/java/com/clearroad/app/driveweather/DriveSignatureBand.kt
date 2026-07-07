package com.clearroad.app.ui.driveweather

import android.graphics.PathMeasure
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Drive Signature band — trip fingerprint inside the Hero card. */
@Composable
internal fun DriveSignatureBand(
    signature: DriveSignature,
    animationKey: Any,
    lineColor: Color,
    startLabel: String,
    endLabel: String,
    labelColor: Color,
    modifier: Modifier = Modifier,
) {
    val spec = remember(signature) { DriveSignatureSpec.forSignature(signature) }
    val progress = remember(animationKey, signature) { Animatable(0f) }

    LaunchedEffect(animationKey, signature) {
        progress.snapTo(0f)
        delay(300)
        progress.animateTo(
            targetValue = 1f,
            animationSpec =
                tween(
                    durationMillis = spec.drawDurationMs,
                    easing = spec.drawEasing,
                ),
        )
    }

    Box(modifier = modifier.fillMaxWidth().height(76.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val strokeWidth = 1.75f
            val lineAlpha = 0.24f

            when (spec.drawMode) {
                SignatureDrawMode.CONTINUOUS -> {
                    val path = spec.buildContinuous(w, h)
                    val revealed = path.trimmedToProgress(progress.value)
                    drawSignatureStroke(
                        path = revealed,
                        color = lineColor,
                        alpha = lineAlpha,
                        width = strokeWidth,
                    )
                    if (progress.value > 0.02f) {
                        drawEndpoint(
                            center = spec.startPoint(w, h),
                            color = lineColor,
                            alpha = 0.30f,
                            radius = 2.5f,
                        )
                    }
                    if (progress.value > 0.96f) {
                        drawEndpoint(
                            center = spec.endPoint(w, h),
                            color = lineColor,
                            alpha = 0.38f,
                            radius = 3f,
                        )
                    }
                }
                SignatureDrawMode.PHASED -> {
                    val phaseSplit = spec.phaseSplit ?: 0.45f
                    val phaseProgress =
                        when {
                            progress.value <= phaseSplit ->
                                (progress.value / phaseSplit).coerceIn(0f, 1f)
                            else -> 1f
                        }
                    val secondProgress =
                        when {
                            progress.value <= phaseSplit -> 0f
                            else -> ((progress.value - phaseSplit) / (1f - phaseSplit)).coerceIn(0f, 1f)
                        }
                    spec.buildPhases(w, h).forEachIndexed { index, phasePath ->
                        val phaseFraction = if (index == 0) phaseProgress else secondProgress
                        if (phaseFraction > 0f) {
                            drawSignatureStroke(
                                path = phasePath.trimmedToProgress(phaseFraction),
                                color = lineColor,
                                alpha = lineAlpha,
                                width = strokeWidth,
                            )
                        }
                    }
                    if (progress.value > 0.02f) {
                        drawEndpoint(spec.startPoint(w, h), lineColor, 0.30f, 2.5f)
                    }
                    if (progress.value > 0.96f) {
                        drawEndpoint(spec.endPoint(w, h), lineColor, 0.38f, 3f)
                    }
                }
                SignatureDrawMode.SEGMENTED -> {
                    val segments = spec.buildSegments(w, h)
                    val visibleCount =
                        (progress.value * segments.size)
                            .toInt()
                            .coerceIn(0, segments.size)
                    segments.take(visibleCount).forEach { segment ->
                        drawSignatureStroke(
                            path = segment,
                            color = lineColor,
                            alpha = lineAlpha,
                            width = strokeWidth,
                        )
                    }
                    if (visibleCount > 0) {
                        drawEndpoint(spec.startPoint(w, h), lineColor, 0.30f, 2.5f)
                    }
                    if (visibleCount == segments.size) {
                        drawEndpoint(spec.endPoint(w, h), lineColor, 0.38f, 3f)
                    }
                }
            }
        }
        Text(
            text = startLabel,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 4.dp, bottom = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = labelColor.copy(alpha = 0.36f),
        )
        Text(
            text = endLabel,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = labelColor.copy(alpha = 0.36f),
        )
    }
}

private enum class SignatureDrawMode {
    CONTINUOUS,
    PHASED,
    SEGMENTED,
}

private data class DriveSignatureSpec(
    val drawMode: SignatureDrawMode,
    val drawDurationMs: Int,
    val drawEasing: Easing,
    val phaseSplit: Float? = null,
    val buildContinuous: (Float, Float) -> Path = { _, _ -> Path() },
    val buildPhases: (Float, Float) -> List<Path> = { _, _ -> emptyList() },
    val buildSegments: (Float, Float) -> List<Path> = { _, _ -> emptyList() },
    val startPoint: (Float, Float) -> Offset,
    val endPoint: (Float, Float) -> Offset,
) {
    companion object {
        fun forSignature(signature: DriveSignature): DriveSignatureSpec =
            when (signature) {
                DriveSignature.SMOOTH -> smooth()
                DriveSignature.BUSY -> busy()
                DriveSignature.CONGESTED -> congested()
                DriveSignature.STOP_START -> stopStart()
            }

        private fun smooth(): DriveSignatureSpec {
            val y = 0.60f
            return DriveSignatureSpec(
                drawMode = SignatureDrawMode.CONTINUOUS,
                drawDurationMs = 620,
                drawEasing = FastOutSlowInEasing,
                buildContinuous = { w, h ->
                    Path().apply {
                        val base = h * y
                        moveTo(w * 0.05f, base)
                        lineTo(w * 0.80f, base)
                        quadraticTo(w * 0.90f, base, w * 0.95f, h * 0.48f)
                    }
                },
                startPoint = { w, h -> Offset(w * 0.05f, h * y) },
                endPoint = { w, h -> Offset(w * 0.95f, h * 0.48f) },
            )
        }

        private fun busy(): DriveSignatureSpec {
            val yBase = 0.72f
            val yTop = 0.22f
            val xBend = 0.46f
            return DriveSignatureSpec(
                drawMode = SignatureDrawMode.PHASED,
                drawDurationMs = 780,
                drawEasing = LinearEasing,
                phaseSplit = 0.42f,
                buildPhases = { w, h ->
                    listOf(
                        Path().apply {
                            moveTo(w * 0.05f, h * yBase)
                            lineTo(w * xBend, h * yBase)
                        },
                        Path().apply {
                            moveTo(w * xBend, h * yBase)
                            cubicTo(
                                w * (xBend + 0.04f),
                                h * yBase,
                                w * xBend,
                                h * (yTop + 0.08f),
                                w * xBend,
                                h * yTop,
                            )
                            lineTo(w * xBend, h * (yTop + 0.18f))
                            cubicTo(
                                w * xBend,
                                h * (yBase - 0.04f),
                                w * (xBend + 0.10f),
                                h * yBase,
                                w * 0.95f,
                                h * yBase,
                            )
                        },
                    )
                },
                startPoint = { w, h -> Offset(w * 0.05f, h * yBase) },
                endPoint = { w, h -> Offset(w * 0.95f, h * yBase) },
            )
        }

        private fun congested(): DriveSignatureSpec {
            val baseY = 0.58f
            val amp = 0.20f
            return DriveSignatureSpec(
                drawMode = SignatureDrawMode.CONTINUOUS,
                drawDurationMs = 820,
                drawEasing = LinearEasing,
                buildContinuous = { w, h ->
                    Path().apply {
                        val y0 = h * baseY
                        val a = h * amp
                        moveTo(w * 0.04f, y0)
                        cubicTo(w * 0.10f, y0 - a, w * 0.16f, y0 + a, w * 0.22f, y0)
                        cubicTo(w * 0.28f, y0 - a, w * 0.34f, y0 + a, w * 0.40f, y0)
                        cubicTo(w * 0.46f, y0 - a, w * 0.52f, y0 + a, w * 0.58f, y0)
                        cubicTo(w * 0.64f, y0 - a * 0.95f, w * 0.72f, y0 + a * 0.90f, w * 0.80f, y0)
                        cubicTo(w * 0.86f, y0 - a * 0.75f, w * 0.92f, y0 + a * 0.55f, w * 0.96f, y0)
                    }
                },
                startPoint = { w, h -> Offset(w * 0.04f, h * baseY) },
                endPoint = { w, h -> Offset(w * 0.96f, h * baseY) },
            )
        }

        private fun stopStart(): DriveSignatureSpec {
            val dashSpecs =
                listOf(
                    0.05f to 0.13f,
                    0.17f to 0.07f,
                    0.27f to 0.11f,
                    0.41f to 0.06f,
                    0.50f to 0.14f,
                    0.67f to 0.05f,
                    0.75f to 0.09f,
                    0.87f to 0.08f,
                )
            return DriveSignatureSpec(
                drawMode = SignatureDrawMode.SEGMENTED,
                drawDurationMs = 720,
                drawEasing = LinearEasing,
                buildSegments = { w, h ->
                    val y = h * 0.58f
                    dashSpecs.map { (startFrac, lengthFrac) ->
                        Path().apply {
                            val x0 = w * startFrac
                            val x1 = w * (startFrac + lengthFrac)
                            moveTo(x0, y)
                            lineTo(x1, y)
                        }
                    }
                },
                startPoint = { w, h -> Offset(w * 0.05f, h * 0.58f) },
                endPoint = { w, h -> Offset(w * 0.95f, h * 0.58f) },
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSignatureStroke(
    path: Path,
    color: Color,
    alpha: Float,
    width: Float,
) {
    drawPath(
        path = path,
        color = color.copy(alpha = alpha * 0.42f),
        style = Stroke(width = width * 2.4f, cap = StrokeCap.Round),
    )
    drawPath(
        path = path,
        color = color.copy(alpha = alpha),
        style = Stroke(width = width, cap = StrokeCap.Round),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEndpoint(
    center: Offset,
    color: Color,
    alpha: Float,
    radius: Float,
) {
    drawCircle(color = color.copy(alpha = alpha), radius = radius, center = center)
}

private fun Path.trimmedToProgress(fraction: Float): Path {
    if (fraction >= 1f) return this
    if (fraction <= 0f) return Path()
    val androidPath = android.graphics.Path()
    val measure = PathMeasure(asAndroidPath(), false)
    val length = measure.length
    if (length <= 0f) return Path()
    measure.getSegment(0f, length * fraction.coerceIn(0f, 1f), androidPath, true)
    return androidPath.asComposePath()
}
