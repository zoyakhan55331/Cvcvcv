package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.model.FizaSessionState
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanBright
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonViolet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FizaVisualizerOrb(
    state: FizaSessionState,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_anim")

    // Slow organic breathing
    val breathProgress by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    // Orbital rotation
    val rotationDegrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Counter rotation for secondary ring
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_rotation"
    )

    // Wave ripple phase
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    // Smooth amplitude transition
    val animatedAmplitude by animateFloatAsState(
        targetValue = amplitude.coerceIn(0f, 1f),
        animationSpec = tween(80),
        label = "smooth_amp"
    )

    // Dynamic primary/secondary colors based on state
    val (primaryColor, secondaryColor, accentColor) = when (state) {
        FizaSessionState.IDLE -> Triple(NeonMagenta, NeonViolet, NeonCyan)
        FizaSessionState.CONNECTING -> Triple(NeonCyan, NeonCyanBright, NeonMagenta)
        FizaSessionState.LISTENING -> Triple(NeonCyan, NeonEmerald, NeonCyanBright)
        FizaSessionState.THINKING -> Triple(NeonPurple, NeonAmber, NeonMagenta)
        FizaSessionState.SPEAKING -> Triple(NeonMagenta, NeonPink, NeonCyan)
    }

    Box(
        modifier = modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 3.4f) * (if (state == FizaSessionState.SPEAKING || state == FizaSessionState.LISTENING) 1f + animatedAmplitude * 0.35f else breathProgress)

            // 1. Outermost Ambient Glow Aura
            val auraRadius = baseRadius * 1.7f + (animatedAmplitude * 40f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = if (state == FizaSessionState.IDLE) 0.18f else 0.38f),
                        secondaryColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = auraRadius
                ),
                radius = auraRadius,
                center = center
            )

            // 2. Cyber Rotating Arcs & Orbit Particles
            val ringRadius = baseRadius * 1.25f + (animatedAmplitude * 20f)
            val radAngle = Math.toRadians(rotationDegrees.toDouble())
            val counterRad = Math.toRadians(counterRotation.toDouble())

            // Draw segmented sci-fi orbital rings
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(primaryColor, secondaryColor, accentColor, primaryColor),
                    center = center
                ),
                startAngle = rotationDegrees,
                sweepAngle = 140f,
                useCenter = false,
                style = Stroke(width = 2.5f, cap = StrokeCap.Round),
                topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                size = androidx.compose.ui.geometry.Size(ringRadius * 2, ringRadius * 2)
            )

            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(accentColor, primaryColor, secondaryColor),
                    center = center
                ),
                startAngle = counterRotation,
                sweepAngle = 90f,
                useCenter = false,
                style = Stroke(width = 1.8f, cap = StrokeCap.Round),
                topLeft = Offset(center.x - (ringRadius * 0.9f), center.y - (ringRadius * 0.9f)),
                size = androidx.compose.ui.geometry.Size(ringRadius * 1.8f, ringRadius * 1.8f)
            )

            // 3. Orbiting Spark Particles
            for (i in 0 until 6) {
                val particleAngle = radAngle + (i * PI / 3.0)
                val distance = ringRadius + sin(wavePhase + i) * 14f
                val px = center.x + (cos(particleAngle) * distance).toFloat()
                val py = center.y + (sin(particleAngle) * distance).toFloat()
                drawCircle(
                    color = if (i % 2 == 0) accentColor else primaryColor,
                    radius = if (state == FizaSessionState.SPEAKING) 3.5f + (animatedAmplitude * 3f) else 2.5f,
                    center = Offset(px, py)
                )
            }

            // 4. Equalizer Wave Rays during Listening or Speaking
            if (state == FizaSessionState.SPEAKING || state == FizaSessionState.LISTENING) {
                val numBars = 36
                for (i in 0 until numBars) {
                    val angle = (i.toFloat() / numBars) * 2 * PI
                    val freqMod = sin(angle * 4 + wavePhase).toFloat()
                    val barHeight = 12f + (animatedAmplitude * 48f * (0.5f + 0.5f * kotlin.math.abs(freqMod)))
                    val innerR = baseRadius * 1.05f
                    val outerR = innerR + barHeight

                    val startX = center.x + (cos(angle) * innerR).toFloat()
                    val startY = center.y + (sin(angle) * innerR).toFloat()
                    val endX = center.x + (cos(angle) * outerR).toFloat()
                    val endY = center.y + (sin(angle) * outerR).toFloat()

                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.6f), accentColor),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY)
                        ),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 5. Morphing Harmonic Sine Wave Shape in Orb
            val wavePath = Path()
            val points = 48
            for (i in 0..points) {
                val theta = (i.toFloat() / points) * 2 * PI
                val modulation = if (state == FizaSessionState.SPEAKING || state == FizaSessionState.LISTENING) {
                    sin(theta * 3 + wavePhase) * (animatedAmplitude * 18f)
                } else {
                    sin(theta * 2 + wavePhase) * 6f
                }
                val r = baseRadius + modulation.toFloat()
                val x = center.x + (cos(theta) * r).toFloat()
                val y = center.y + (sin(theta) * r).toFloat()
                if (i == 0) {
                    wavePath.moveTo(x, y)
                } else {
                    wavePath.lineTo(x, y)
                }
            }
            wavePath.close()

            // Draw morphing harmonic core
            drawPath(
                path = wavePath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.95f),
                        secondaryColor.copy(alpha = 0.75f),
                        accentColor.copy(alpha = 0.4f)
                    ),
                    center = center,
                    radius = baseRadius
                )
            )

            // Inner holographic core glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (state == FizaSessionState.SPEAKING) 0.85f else 0.5f),
                        primaryColor.copy(alpha = 0.4f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 0.55f
                ),
                radius = baseRadius * 0.55f,
                center = center
            )
        }
    }
}
