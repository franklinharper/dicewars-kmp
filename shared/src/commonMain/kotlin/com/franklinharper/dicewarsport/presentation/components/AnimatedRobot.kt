package com.franklinharper.dicewarsport.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

@Composable
fun AnimatedRobot(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "robot")

    // Right arm fist pump (raised and celebrating)
    val fistPumpAngle by infiniteTransition.animateFloat(
        initialValue = -60f,
        targetValue = -80f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "fistPump",
    )

    // Antenna sway
    val antennaSway by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "antennaSway",
    )

    // Eye blink
    val blink by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, delayMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blink",
    )

    // Star particles time
    val starTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
        ),
        label = "starTime",
    )

    val heartPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
        ),
        label = "heartPulse",
    )

    // Entrance
    var visible by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { visible = true }
    val entranceScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500),
        label = "entrance",
    )

    val bodyColor = Color(0xFF78909C)
    val bodyDark = Color(0xFF546E7A)
    val headColor = Color(0xFF90A4AE)
    val accentColor = Color(0xFF42A5F5)
    val eyeColor = Color(0xFF1769AA)
    val cheekColor = Color(0xFFEF9A9A)
    val antennaColor = Color(0xFFFF7043)

    // Star data: angle offsets and speeds for particles bursting from fist
    val starSeeds = remember {
        List(12) { i ->
            StarSeed(
                angle = (i * 30f + 15f) * (kotlin.math.PI / 180f).toFloat(),
                speed = 0.6f + (i % 3) * 0.2f,
                delay = (i % 4) * 0.08f,
                colorIndex = i % 5,
            )
        }
    }
    val starColors = remember {
        listOf(
            Color(0xFFFFD54F), // gold
            Color(0xFFFF7043), // orange
            Color(0xFF42A5F5), // blue
            Color(0xFF66BB6A), // green
            Color(0xFFEF5350), // red
        )
    }

    Canvas(
        modifier = modifier.scale(entranceScale),
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val sc = minOf(w, h) / 300f

        fun s(v: Float) = v * sc
        fun s(v: Int) = v * sc

        val bodyTop = h * 0.42f + s(10)
        val bodyLeft = cx - s(60)
        val bodyW = s(120)
        val bodyH = s(100)
        val shoulderWidth = s(20) // how far arms extend beyond body edge

        // --- Stars (drawn behind robot so they appear to emerge from fist) ---
        // Compute fist position first to know where stars originate
        val rightArmPivotX = bodyLeft + bodyW + shoulderWidth
        val rightArmPivotY = bodyTop + s(15)
        // Fist is at end of arm rotated by fistPumpAngle
        val armLen = s(60)
        val fistAngleRad = (fistPumpAngle - 90f) * (kotlin.math.PI / 180f).toFloat() // -90 so 0 = pointing right
        // Actually the arm rotates around pivot, so fist position:
        val fistDirRad = fistPumpAngle * (kotlin.math.PI / 180f).toFloat()
        val fistX = rightArmPivotX + armLen * kotlin.math.sin(fistDirRad) // negative angle = left
        val fistY = rightArmPivotY - armLen * kotlin.math.cos(fistDirRad) // negative = up

        for (seed in starSeeds) {
            val t = (starTime - seed.delay).coerceIn(0f, 1f) / (1f - seed.delay) // normalized 0..1
            if (t <= 0f) continue
            val dist = t * sc * 180f * seed.speed
            val shrink = 1f - t * 0.7f // stars shrink to 30% as they travel
            if (shrink <= 0f) continue
            val starSize = s(6) * shrink
            val x = fistX + dist * kotlin.math.cos(seed.angle)
            val y = fistY - dist * kotlin.math.sin(seed.angle) // negative = upward
            val alpha = shrink // fade as they shrink

            // 4-pointed star shape
            val starPath = Path().apply {
                moveTo(x, y - starSize)
                lineTo(x + starSize * 0.3f, y)
                lineTo(x, y + starSize)
                lineTo(x - starSize * 0.3f, y)
                close()
            }
            drawPath(starPath, starColors[seed.colorIndex].copy(alpha = alpha))
            // Cross
            drawLine(
                color = starColors[seed.colorIndex].copy(alpha = alpha),
                start = Offset(x - starSize, y),
                end = Offset(x + starSize, y),
                strokeWidth = starSize * 0.3f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = starColors[seed.colorIndex].copy(alpha = alpha),
                start = Offset(x, y - starSize),
                end = Offset(x, y + starSize),
                strokeWidth = starSize * 0.3f,
                cap = StrokeCap.Round,
            )
        }

        // --- Robot body ---

        // Left arm (down by side, sticks out from shoulder)
        val leftArmPivotX = bodyLeft - shoulderWidth
        val leftArmPivotY = bodyTop + s(15)
        rotate(-15f, Offset(leftArmPivotX, leftArmPivotY)) {
            // Upper arm (horizontal from shoulder outward)
            drawRoundRect(
                color = bodyDark,
                topLeft = Offset(leftArmPivotX - s(20), leftArmPivotY),
                size = androidx.compose.ui.geometry.Size(s(20), s(18)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(8)),
            )
            // Forearm (hangs down)
            drawRoundRect(
                color = bodyDark,
                topLeft = Offset(leftArmPivotX - s(38), leftArmPivotY + s(8)),
                size = androidx.compose.ui.geometry.Size(s(20), s(45)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(8)),
            )
            // Hand
            drawCircle(
                color = headColor,
                radius = s(10),
                center = Offset(leftArmPivotX - s(28), leftArmPivotY + s(55)),
            )
        }

        // Legs
        drawRoundRect(
            color = bodyDark,
            topLeft = Offset(cx - s(30), bodyTop + bodyH),
            size = androidx.compose.ui.geometry.Size(s(22), s(40)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(8)),
        )
        drawRoundRect(
            color = bodyDark,
            topLeft = Offset(cx + s(8), bodyTop + bodyH),
            size = androidx.compose.ui.geometry.Size(s(22), s(40)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(8)),
        )

        // Feet
        drawRoundRect(
            color = Color(0xFF455A64),
            topLeft = Offset(cx - s(38), bodyTop + bodyH + s(28)),
            size = androidx.compose.ui.geometry.Size(s(35), s(14)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(6)),
        )
        drawRoundRect(
            color = Color(0xFF455A64),
            topLeft = Offset(cx + s(3), bodyTop + bodyH + s(28)),
            size = androidx.compose.ui.geometry.Size(s(35), s(14)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(6)),
        )

        // Body
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(bodyLeft, bodyTop),
            size = androidx.compose.ui.geometry.Size(bodyW, bodyH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(16)),
        )
        // Body panel
        drawRoundRect(
            color = bodyDark,
            topLeft = Offset(cx - s(20), bodyTop + s(20)),
            size = androidx.compose.ui.geometry.Size(s(40), s(30)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(8)),
        )
        // Panel light
        val heartAlpha = 0.5f + 0.5f * kotlin.math.sin(heartPulse * 2f * kotlin.math.PI.toFloat())
        drawCircle(
            color = accentColor.copy(alpha = heartAlpha),
            radius = s(8),
            center = Offset(cx, bodyTop + s(35)),
        )
        drawCircle(color = Color(0xFF66BB6A), radius = s(4), center = Offset(cx - s(10), bodyTop + s(25)))
        drawCircle(color = Color(0xFFFFA726), radius = s(4), center = Offset(cx + s(10), bodyTop + s(25)))

        // Shoulder connectors (visual bridge from body to arms)
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(bodyLeft, bodyTop + s(8)),
            size = androidx.compose.ui.geometry.Size(s(15), s(20)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(6)),
        )
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(bodyLeft + bodyW - s(15), bodyTop + s(8)),
            size = androidx.compose.ui.geometry.Size(s(15) + shoulderWidth, s(20)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(6)),
        )

        // Right arm (raised fist!)
        rotate(fistPumpAngle, Offset(rightArmPivotX, rightArmPivotY)) {
            drawRoundRect(
                color = bodyDark,
                topLeft = Offset(rightArmPivotX - s(9), rightArmPivotY - armLen),
                size = androidx.compose.ui.geometry.Size(s(18), armLen),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(8)),
            )
            // Clenched fist
            drawRoundRect(
                color = headColor,
                topLeft = Offset(rightArmPivotX - s(10), rightArmPivotY - armLen - s(14)),
                size = androidx.compose.ui.geometry.Size(s(24), s(18)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(6)),
            )
            // Fist lines (knuckle detail)
            drawLine(
                color = bodyDark,
                start = Offset(rightArmPivotX - s(5), rightArmPivotY - armLen - s(12)),
                end = Offset(rightArmPivotX + s(10), rightArmPivotY - armLen - s(12)),
                strokeWidth = s(2),
            )
            drawLine(
                color = bodyDark,
                start = Offset(rightArmPivotX - s(5), rightArmPivotY - armLen - s(7)),
                end = Offset(rightArmPivotX + s(10), rightArmPivotY - armLen - s(7)),
                strokeWidth = s(2),
            )
        }

        // Neck
        drawRoundRect(
            color = bodyDark,
            topLeft = Offset(cx - s(12), bodyTop - s(12)),
            size = androidx.compose.ui.geometry.Size(s(24), s(18)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(6)),
        )

        // Head
        val headCenterY = bodyTop - s(55)
        drawRoundRect(
            color = headColor,
            topLeft = Offset(cx - s(50), headCenterY - s(40)),
            size = androidx.compose.ui.geometry.Size(s(100), s(80)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(20)),
        )

        // Face plate
        drawRoundRect(
            color = Color(0xFFECEFF1),
            topLeft = Offset(cx - s(40), headCenterY - s(28)),
            size = androidx.compose.ui.geometry.Size(s(80), s(52)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(14)),
        )

        // Eyes
        val eyeY = headCenterY - s(8)
        val leftEyeX = cx - s(18)
        val rightEyeX = cx + s(18)
        drawCircle(color = Color.White, radius = s(12), center = Offset(leftEyeX, eyeY))
        drawCircle(color = Color.White, radius = s(12), center = Offset(rightEyeX, eyeY))
        drawOval(
            color = eyeColor,
            topLeft = Offset(leftEyeX - s(6), eyeY - s(8 * blink)),
            size = androidx.compose.ui.geometry.Size(s(12), s(16 * blink)),
        )
        drawOval(
            color = eyeColor,
            topLeft = Offset(rightEyeX - s(6), eyeY - s(8 * blink)),
            size = androidx.compose.ui.geometry.Size(s(12), s(16 * blink)),
        )
        drawCircle(color = Color.White, radius = s(3), center = Offset(leftEyeX + s(3), eyeY - s(3)))
        drawCircle(color = Color.White, radius = s(3), center = Offset(rightEyeX + s(3), eyeY - s(3)))

        // Big open smile
        val smilePath = Path().apply {
            moveTo(cx - s(18), headCenterY + s(8))
            quadraticTo(cx, headCenterY + s(26), cx + s(18), headCenterY + s(8))
            close()
        }
        drawPath(smilePath, eyeColor)
        // Tongue
        drawOval(
            color = Color(0xFFEF9A9A),
            topLeft = Offset(cx - s(5), headCenterY + s(16)),
            size = androidx.compose.ui.geometry.Size(s(10), s(8)),
        )

        // Cheeks
        drawCircle(
            color = cheekColor.copy(alpha = 0.6f),
            radius = s(8),
            center = Offset(cx - s(32), headCenterY + s(6)),
        )
        drawCircle(
            color = cheekColor.copy(alpha = 0.6f),
            radius = s(8),
            center = Offset(cx + s(32), headCenterY + s(6)),
        )

        // Ears
        drawCircle(color = bodyColor, radius = s(12), center = Offset(cx - s(50), headCenterY - s(5)))
        drawCircle(color = bodyColor, radius = s(12), center = Offset(cx + s(50), headCenterY - s(5)))

        // Antenna
        drawLine(
            color = bodyDark,
            start = Offset(cx, headCenterY - s(40)),
            end = Offset(cx + antennaSway * sc * 3, headCenterY - s(60)),
            strokeWidth = s(5),
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = antennaColor,
            radius = s(10),
            center = Offset(cx + antennaSway * sc * 3, headCenterY - s(65)),
        )
        drawCircle(
            color = antennaColor.copy(alpha = 0.3f),
            radius = s(16),
            center = Offset(cx + antennaSway * sc * 3, headCenterY - s(65)),
        )
    }
}

internal data class StarSeed(
    val angle: Float,
    val speed: Float,
    val delay: Float,
    val colorIndex: Int,
)
