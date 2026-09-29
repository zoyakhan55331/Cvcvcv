package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.FizaSessionState
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet

@Composable
fun FizaCharacterAvatar(
    state: FizaSessionState,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "anime_avatar_anim")

    // Breathing pulse
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatar_breath"
    )

    // Gentle float
    val floatY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatar_float"
    )

    // Smooth amplitude for speaking reaction
    val animatedAmp by animateFloatAsState(
        targetValue = amplitude.coerceIn(0f, 1f),
        animationSpec = tween(90),
        label = "avatar_amp"
    )

    val (glowStart, glowEnd) = when (state) {
        FizaSessionState.IDLE -> Pair(NeonPink, NeonViolet)
        FizaSessionState.CONNECTING -> Pair(NeonCyan, NeonPink)
        FizaSessionState.LISTENING -> Pair(NeonCyan, NeonEmerald)
        FizaSessionState.THINKING -> Pair(NeonAmber, NeonPink)
        FizaSessionState.SPEAKING -> Pair(NeonMagenta, NeonPink)
    }

    val dynamicScale = breathingScale + (if (state == FizaSessionState.SPEAKING || state == FizaSessionState.LISTENING) animatedAmp * 0.12f else 0f)

    Box(
        modifier = modifier
            .size(270.dp)
            .offset(y = floatY.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer animated glow rings
        Box(
            modifier = Modifier
                .size(270.dp)
                .scale(dynamicScale * 1.05f)
                .clip(CircleShape)
                .shadow(
                    elevation = 28.dp,
                    shape = CircleShape,
                    ambientColor = glowStart,
                    spotColor = glowEnd
                )
                .border(
                    width = (3f + animatedAmp * 4f).dp,
                    brush = Brush.sweepGradient(
                        listOf(glowStart, glowEnd, NeonCyan, glowStart)
                    ),
                    shape = CircleShape
                )
        )

        // Inner glowing border
        Box(
            modifier = Modifier
                .size(244.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.5f),
                    shape = CircleShape
                )
        ) {
            // Character portrait from video theme
            Image(
                painter = painterResource(id = R.drawable.fiza_anime_theme),
                contentDescription = "Fiza Anime Theme Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.12f)
            )
        }
    }
}
