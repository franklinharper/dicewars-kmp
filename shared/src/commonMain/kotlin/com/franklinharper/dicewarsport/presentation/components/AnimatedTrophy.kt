package com.franklinharper.dicewarsport.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

@Composable
fun AnimatedTrophy(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "trophy")

    // Gentle pulsing scale
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "trophyScale",
    )

    // Sparkle rotation
    val sparkleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
        ),
        label = "sparkleRotation",
    )

    val sparklePhases = remember { List(8) { it * (kotlin.math.PI.toFloat() / 4f) } }
    val twinkleTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * kotlin.math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
        ),
        label = "twinkle",
    )

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { visible = true }
    val entranceScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600),
        label = "entrance",
    )

    val gold = Color(0xFFFFD700)
    val goldLight = Color(0xFFFFEC80)
    val goldDark = Color(0xFFDAA520)
    val goldDarker = Color(0xFFB8860B)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        // Sparkle ring
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(entranceScale),
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = minOf(size.width, size.height) * 0.42f

            rotate(sparkleRotation, center) {
                for (i in sparklePhases.indices) {
                    val angle = sparklePhases[i]
                    val twinkle = (kotlin.math.sin((twinkleTime + angle).toDouble()) * 0.5 + 0.5).toFloat()
                    val sparkleAlpha = 0.3f + twinkle * 0.7f
                    val sparkleSize = 4.dp.toPx() + twinkle * 8.dp.toPx()
                    val x = center.x + radius * kotlin.math.cos(angle)
                    val y = center.y + radius * kotlin.math.sin(angle)

                    drawLine(
                        color = gold.copy(alpha = sparkleAlpha),
                        start = Offset(x - sparkleSize, y),
                        end = Offset(x + sparkleSize, y),
                        strokeWidth = sparkleSize * 0.3f,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = gold.copy(alpha = sparkleAlpha),
                        start = Offset(x, y - sparkleSize),
                        end = Offset(x, y + sparkleSize),
                        strokeWidth = sparkleSize * 0.3f,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }

        // Trophy + glow
        Canvas(
            modifier = Modifier
                .scale(scale * entranceScale)
                .fillMaxSize(0.6f),
        ) {
            val w = size.width
            val h = size.height
            val cx = w / 2f

            // Golden glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x55FFD700), Color(0x00FFD700)),
                    center = Offset(cx, h * 0.4f),
                    radius = w * 0.5f,
                ),
                center = Offset(cx, h * 0.4f),
                radius = w * 0.5f,
            )

            // Cup body
            val cupTop = h * 0.08f
            val cupBottom = h * 0.52f
            val cupLeftTop = cx - w * 0.35f
            val cupRightTop = cx + w * 0.35f
            val cupLeftBottom = cx - w * 0.18f
            val cupRightBottom = cx + w * 0.18f

            val cupPath = Path().apply {
                moveTo(cupLeftTop, cupTop)
                lineTo(cupRightTop, cupTop)
                lineTo(cupRightBottom, cupBottom)
                // Rounded bottom
                quadraticTo(cx, cupBottom + h * 0.06f, cupLeftBottom, cupBottom)
                close()
            }
            drawPath(cupPath, gold)

            // Cup highlight (left side)
            val highlightPath = Path().apply {
                moveTo(cupLeftTop + w * 0.06f, cupTop)
                lineTo(cupLeftTop + w * 0.14f, cupTop)
                lineTo(cupLeftBottom + w * 0.06f, cupBottom - h * 0.04f)
                quadraticTo(cx - w * 0.15f, cupBottom + h * 0.02f, cupLeftBottom + w * 0.02f, cupBottom - h * 0.02f)
                close()
            }
            drawPath(highlightPath, goldLight)

            // Cup shadow (right side)
            val shadowPath = Path().apply {
                moveTo(cupRightTop - w * 0.1f, cupTop)
                lineTo(cupRightTop, cupTop)
                lineTo(cupRightBottom, cupBottom)
                quadraticTo(cx + w * 0.05f, cupBottom + h * 0.04f, cx + w * 0.08f, cupBottom - h * 0.02f)
                close()
            }
            drawPath(shadowPath, goldDark)

            // Left handle
            val handlePath = Path().apply {
                moveTo(cupLeftTop, cupTop + h * 0.06f)
                cubicTo(
                    cx - w * 0.55f, cupTop + h * 0.06f,
                    cx - w * 0.55f, cupTop + h * 0.32f,
                    cupLeftTop, cupTop + h * 0.32f,
                )
            }
            drawPath(
                handlePath,
                gold,
                style = Stroke(width = w * 0.045f, cap = StrokeCap.Round),
            )

            // Right handle
            val handleRPath = Path().apply {
                moveTo(cupRightTop, cupTop + h * 0.06f)
                cubicTo(
                    cx + w * 0.55f, cupTop + h * 0.06f,
                    cx + w * 0.55f, cupTop + h * 0.32f,
                    cupRightTop, cupTop + h * 0.32f,
                )
            }
            drawPath(
                handleRPath,
                gold,
                style = Stroke(width = w * 0.045f, cap = StrokeCap.Round),
            )

            // Rim at top of cup
            drawLine(
                color = goldLight,
                start = Offset(cupLeftTop - w * 0.02f, cupTop),
                end = Offset(cupRightTop + w * 0.02f, cupTop),
                strokeWidth = h * 0.025f,
                cap = StrokeCap.Round,
            )

            // Star on cup
            val starCx = cx
            val starCy = cupTop + (cupBottom - cupTop) * 0.4f
            val starOuter = w * 0.1f
            val starInner = w * 0.04f
            val starPath = Path().apply {
                for (i in 0 until 5) {
                    val outerAngle = (-90f + i * 72f) * (kotlin.math.PI.toFloat() / 180f)
                    val innerAngle = (-90f + 36f + i * 72f) * (kotlin.math.PI.toFloat() / 180f)
                    val ox = starCx + starOuter * kotlin.math.cos(outerAngle)
                    val oy = starCy + starOuter * kotlin.math.sin(outerAngle)
                    val ix = starCx + starInner * kotlin.math.cos(innerAngle)
                    val iy = starCy + starInner * kotlin.math.sin(innerAngle)
                    if (i == 0) moveTo(ox, oy) else lineTo(ox, oy)
                    lineTo(ix, iy)
                }
                close()
            }
            drawPath(starPath, Color(0xFFFFFFF0))

            // Stem
            val stemTop = cupBottom + h * 0.01f
            val stemBottom = cupBottom + h * 0.16f
            drawRect(
                color = goldDark,
                topLeft = Offset(cx - w * 0.035f, stemTop),
                size = androidx.compose.ui.geometry.Size(w * 0.07f, stemBottom - stemTop),
            )
            // Stem highlight
            drawRect(
                color = gold,
                topLeft = Offset(cx - w * 0.035f, stemTop),
                size = androidx.compose.ui.geometry.Size(w * 0.035f, stemBottom - stemTop),
            )

            // Base top
            val baseTop = stemBottom
            val baseBottom = baseTop + h * 0.04f
            drawRect(
                color = goldDark,
                topLeft = Offset(cx - w * 0.2f, baseTop),
                size = androidx.compose.ui.geometry.Size(w * 0.4f, baseBottom - baseTop),
            )
            // Base highlight strip
            drawRect(
                color = gold,
                topLeft = Offset(cx - w * 0.2f, baseTop),
                size = androidx.compose.ui.geometry.Size(w * 0.4f, (baseBottom - baseTop) * 0.4f),
            )
            // Base bottom
            drawRect(
                color = goldDarker,
                topLeft = Offset(cx - w * 0.24f, baseBottom),
                size = androidx.compose.ui.geometry.Size(w * 0.48f, h * 0.025f),
            )
        }
    }
}
