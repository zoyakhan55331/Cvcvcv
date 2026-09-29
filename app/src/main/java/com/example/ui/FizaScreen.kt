package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.FizaSessionState
import com.example.ui.components.ConversationHistorySheet
import com.example.ui.components.FizaCharacterAvatar
import com.example.ui.components.FizaVisualizerOrb
import com.example.ui.components.PersonalityPickerSheet
import com.example.ui.components.PhoneControlPanel
import com.example.ui.components.ServerConnectDialog
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.FizaViewModel
import com.example.viewmodel.ThemeDisplayMode

@Composable
fun FizaScreen(
    viewModel: FizaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sessionState by viewModel.sessionState.collectAsState()
    val audioAmplitude by viewModel.audioAmplitude.collectAsState()
    val liveTranscript by viewModel.liveTranscript.collectAsState()
    val fizaSpokenText by viewModel.fizaSpokenText.collectAsState()
    val personalityMode by viewModel.personalityMode.collectAsState()
    val history by viewModel.conversationHistory.collectAsState()
    val activeToolNotification by viewModel.activeToolNotification.collectAsState()
    val serverOrKey by viewModel.serverOrKey.collectAsState()
    val isServerConnected by viewModel.isServerConnected.collectAsState()
    val isConnectingServer by viewModel.isConnectingServer.collectAsState()
    val serverConnectionMessage by viewModel.serverConnectionMessage.collectAsState()
    val displayThemeMode by viewModel.displayThemeMode.collectAsState()

    var showPersonalitySheet by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showPhoneControls by remember { mutableStateOf(false) }

    // Audio & Camera permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (recordGranted) {
            viewModel.onMicClicked()
        } else {
            Toast.makeText(
                context,
                "Microphone access needed to chat with Fiza by voice!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Clear active tool notification after 4 seconds
    LaunchedEffect(activeToolNotification) {
        if (activeToolNotification != null) {
            kotlinx.coroutines.delay(4000)
            viewModel.clearNotification()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CyberBlack,
                        Color(0xFF160D1E),
                        CyberDark
                    )
                )
            )
            .padding(top = statusBarPadding, bottom = navBarPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR: Brand Logo, Server IP Key Button, Phone Access, Action Icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brand Title & Personality Mode
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showPersonalitySheet = true }
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(NeonPink, NeonCyan))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "F", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Fiza AI",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = personalityMode.emoji, fontSize = 14.sp)
                        }

                        Text(
                            text = personalityMode.displayName,
                            fontSize = 11.sp,
                            color = NeonPink,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Server Connect Pill & Quick Phone Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Connect Server / IP Key Pill Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isServerConnected) NeonEmerald.copy(alpha = 0.2f) else CyberSurfaceVariant)
                            .border(
                                1.dp,
                                if (isServerConnected) NeonEmerald else NeonCyan.copy(alpha = 0.5f),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { showServerDialog = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("server_connect_pill")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lan,
                                contentDescription = "Server Connect",
                                tint = if (isServerConnected) NeonEmerald else NeonCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isServerConnected) "Connected 🟢" else "IP Key ⚡",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isServerConnected) NeonEmerald else NeonCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Phone Controls Button
                    IconButton(
                        onClick = { showPhoneControls = true },
                        modifier = Modifier.testTag("phone_controls_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = "Phone Access Hub",
                            tint = NeonPink,
                            modifier = Modifier.size(23.dp)
                        )
                    }

                    // Avatar Theme Toggle Button
                    IconButton(
                        onClick = { viewModel.toggleThemeMode() },
                        modifier = Modifier.testTag("toggle_theme_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = "Toggle Theme Visualizer",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // History Button
                    IconButton(
                        onClick = { showHistorySheet = true },
                        modifier = Modifier.testTag("transcript_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Session History",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // TOOL / SYSTEM NOTIFICATION BANNER
            AnimatedVisibility(
                visible = activeToolNotification != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Surface(
                    color = CyberSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ ${activeToolNotification ?: ""}",
                            fontSize = 12.sp,
                            color = NeonCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // CENTER SECTION: Anime Character Avatar (From Video Theme) OR Sci-Fi Hologram Orb
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Crossfade(targetState = displayThemeMode, label = "theme_switch") { mode ->
                    when (mode) {
                        ThemeDisplayMode.ANIME_AVATAR -> {
                            FizaCharacterAvatar(
                                state = sessionState,
                                amplitude = audioAmplitude,
                                modifier = Modifier.testTag("fiza_anime_avatar")
                            )
                        }
                        ThemeDisplayMode.SCI_FI_ORB -> {
                            FizaVisualizerOrb(
                                state = sessionState,
                                amplitude = audioAmplitude,
                                modifier = Modifier.testTag("fiza_visualizer_orb")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Real-time State Badge
                StateBadge(state = sessionState)

                Spacer(modifier = Modifier.height(14.dp))

                // Dynamic Live Captions / Spoken Subtitle Container
                LiveCaptionContainer(
                    state = sessionState,
                    userLiveText = liveTranscript,
                    fizaSpokenText = fizaSpokenText,
                    onInterrupt = { viewModel.interruptSpeaking() }
                )
            }

            // BOTTOM SECTION: Quick Banter / Phone Action Chips & Central Mic Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Quick Phone & Banter Actions
                QuickActionChips(
                    onSelectPrompt = { prompt ->
                        viewModel.processUserMessage(prompt)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Central Glowing Mic Button
                CentralMicButton(
                    state = sessionState,
                    amplitude = audioAmplitude,
                    onClick = {
                        val hasMic = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        val hasCamera = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasMic && hasCamera) {
                            viewModel.onMicClicked()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.RECORD_AUDIO,
                                    Manifest.permission.CAMERA
                                )
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mic helper hint
                Text(
                    text = when (sessionState) {
                        FizaSessionState.IDLE -> "Tap to speak with Fiza • Full Phone Access"
                        FizaSessionState.CONNECTING -> "Connecting..."
                        FizaSessionState.LISTENING -> "Listening • Tap to stop"
                        FizaSessionState.THINKING -> "Thinking of a witty reply..."
                        FizaSessionState.SPEAKING -> "Tap to interrupt & talk"
                    },
                    fontSize = 12.sp,
                    color = TextTertiary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // Modal Sheets & Dialogs
    if (showPersonalitySheet) {
        PersonalityPickerSheet(
            currentMode = personalityMode,
            onModeSelected = { mode -> viewModel.selectPersonality(mode) },
            onDismiss = { showPersonalitySheet = false }
        )
    }

    if (showServerDialog) {
        ServerConnectDialog(
            currentServerOrKey = serverOrKey,
            isConnected = isServerConnected,
            isConnecting = isConnectingServer,
            connectionMessage = serverConnectionMessage,
            onConnect = { ipOrKey -> viewModel.connectServer(ipOrKey) },
            onDismiss = { showServerDialog = false }
        )
    }

    if (showPhoneControls) {
        PhoneControlPanel(
            onTriggerAction = { command -> viewModel.processUserMessage(command) },
            onDismiss = { showPhoneControls = false }
        )
    }

    if (showHistorySheet) {
        ConversationHistorySheet(
            history = history,
            onSpeakMessage = { text -> viewModel.processUserMessage("Repeat this: $text") },
            onClearHistory = { viewModel.clearHistory() },
            onDismiss = { showHistorySheet = false }
        )
    }
}

@Composable
fun StateBadge(state: FizaSessionState) {
    val (label, dotColor) = when (state) {
        FizaSessionState.IDLE -> Pair("Ready to chat", NeonPink)
        FizaSessionState.CONNECTING -> Pair("Connecting...", NeonCyan)
        FizaSessionState.LISTENING -> Pair("Listening to you...", NeonEmerald)
        FizaSessionState.THINKING -> Pair("Crafting comeback...", NeonAmber)
        FizaSessionState.SPEAKING -> Pair("Fiza is speaking...", NeonMagenta)
    }

    val animatedDotColor by animateColorAsState(targetValue = dotColor, label = "dot_color")

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CyberSurface)
            .border(1.dp, animatedDotColor.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("session_state_badge")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(animatedDotColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun LiveCaptionContainer(
    state: FizaSessionState,
    userLiveText: String,
    fizaSpokenText: String,
    onInterrupt: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 500.dp)
            .padding(horizontal = 8.dp)
            .testTag("caption_card"),
        colors = CardDefaults.cardColors(containerColor = CyberSurface.copy(alpha = 0.88f)),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (state == FizaSessionState.SPEAKING) NeonPink.copy(alpha = 0.5f) else Color(0x22FFFFFF)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state == FizaSessionState.LISTENING && userLiveText.isNotBlank()) {
                Text(
                    text = "You: \"$userLiveText\"",
                    fontSize = 15.sp,
                    color = NeonCyan,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 21.sp
                )
            } else {
                Text(
                    text = "\"$fizaSpokenText\"",
                    fontSize = 15.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            if (state == FizaSessionState.SPEAKING) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberSurfaceVariant)
                        .clickable { onInterrupt() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Interrupt",
                        tint = NeonPink,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tap to interrupt",
                        fontSize = 11.sp,
                        color = NeonPink
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionChips(
    onSelectPrompt: (String) -> Unit
) {
    val prompts = listOf(
        "🔦 Turn on flashlight",
        "📸 Open camera",
        "💬 Open WhatsApp",
        "🔋 Battery status",
        "📳 Vibrate phone",
        "🔊 Volume up",
        "😏 Roast me",
        "🎬 Open YouTube",
        "🖼️ Open gallery",
        "✨ Tell me a secret"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        prompts.forEach { prompt ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberSurface)
                    .border(1.dp, NeonPink.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .clickable { onSelectPrompt(prompt) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("action_chip_${prompt.take(6).trim().lowercase()}")
            ) {
                Text(
                    text = prompt,
                    fontSize = 12.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun CentralMicButton(
    state: FizaSessionState,
    amplitude: Float,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_mic")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val isListening = state == FizaSessionState.LISTENING
    val isSpeaking = state == FizaSessionState.SPEAKING

    val buttonBrush = when (state) {
        FizaSessionState.IDLE -> Brush.linearGradient(listOf(NeonPink, NeonViolet))
        FizaSessionState.CONNECTING -> Brush.linearGradient(listOf(NeonCyan, NeonPink))
        FizaSessionState.LISTENING -> Brush.linearGradient(listOf(NeonCyan, NeonEmerald))
        FizaSessionState.THINKING -> Brush.linearGradient(listOf(NeonAmber, NeonPink))
        FizaSessionState.SPEAKING -> Brush.linearGradient(listOf(NeonPink, NeonMagenta))
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(96.dp)
    ) {
        // Outer pulsing aura
        if (isListening || isSpeaking) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(if (isListening) 1f + amplitude * 0.45f else pulseScale)
                    .clip(CircleShape)
                    .background(
                        (if (isListening) NeonCyan else NeonPink).copy(alpha = 0.25f)
                    )
            )
        }

        // Inner main interactive button
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(buttonBrush)
                .clickable { onClick() }
                .testTag("central_mic_button"),
            contentAlignment = Alignment.Center
        ) {
            val icon = when (state) {
                FizaSessionState.SPEAKING -> Icons.Default.Stop
                FizaSessionState.LISTENING -> Icons.Default.Mic
                else -> Icons.Default.Mic
            }
            Icon(
                imageVector = icon,
                contentDescription = "Microphone",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}
