package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ChatMessage
import com.example.model.FizaSessionState
import com.example.model.MessageSender
import com.example.model.PersonalityMode
import com.example.service.AudioEngine
import com.example.service.GeminiService
import com.example.service.ToolExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ThemeDisplayMode {
    ANIME_AVATAR,
    SCI_FI_ORB
}

class FizaViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("fiza_prefs", Context.MODE_PRIVATE)

    private val geminiService = GeminiService()
    private val toolExecutor = ToolExecutor(application.applicationContext)
    private val audioEngine = AudioEngine(application.applicationContext, viewModelScope)

    private val _sessionState = MutableStateFlow(FizaSessionState.IDLE)
    val sessionState: StateFlow<FizaSessionState> = _sessionState.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _fizaSpokenText = MutableStateFlow("Hey there! Tap the mic to talk to me. Don't be shy 😏")
    val fizaSpokenText: StateFlow<String> = _fizaSpokenText.asStateFlow()

    private val _personalityMode = MutableStateFlow(PersonalityMode.SASSY_FLIRTY)
    val personalityMode: StateFlow<PersonalityMode> = _personalityMode.asStateFlow()

    private val _conversationHistory = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = MessageSender.FIZA,
                text = "Hey there! Tap the mic to talk to me. Don't be shy 😏"
            )
        )
    )
    val conversationHistory: StateFlow<List<ChatMessage>> = _conversationHistory.asStateFlow()

    private val _activeToolNotification = MutableStateFlow<String?>(null)
    val activeToolNotification: StateFlow<String?> = _activeToolNotification.asStateFlow()

    private val _serverOrKey = MutableStateFlow(prefs.getString("saved_server_or_key", "") ?: "")
    val serverOrKey: StateFlow<String> = _serverOrKey.asStateFlow()

    private val _isServerConnected = MutableStateFlow(prefs.getBoolean("is_server_connected", false))
    val isServerConnected: StateFlow<Boolean> = _isServerConnected.asStateFlow()

    private val _isConnectingServer = MutableStateFlow(false)
    val isConnectingServer: StateFlow<Boolean> = _isConnectingServer.asStateFlow()

    private val _serverConnectionMessage = MutableStateFlow<String?>(null)
    val serverConnectionMessage: StateFlow<String?> = _serverConnectionMessage.asStateFlow()

    private val _displayThemeMode = MutableStateFlow(ThemeDisplayMode.ANIME_AVATAR)
    val displayThemeMode: StateFlow<ThemeDisplayMode> = _displayThemeMode.asStateFlow()

    var voicePitch: Float
        get() = audioEngine.voicePitch
        set(value) { audioEngine.voicePitch = value }

    var voiceRate: Float
        get() = audioEngine.voiceRate
        set(value) { audioEngine.voiceRate = value }

    init {
        audioEngine.listener = object : AudioEngine.Listener {
            override fun onListeningStarted() {
                _sessionState.value = FizaSessionState.LISTENING
                _liveTranscript.value = ""
            }

            override fun onListeningEnded() {
                if (_sessionState.value == FizaSessionState.LISTENING) {
                    _sessionState.value = FizaSessionState.THINKING
                }
            }

            override fun onSpeechRecognized(text: String) {
                _liveTranscript.value = text
                processUserMessage(text)
            }

            override fun onPartialSpeech(partialText: String) {
                _liveTranscript.value = partialText
            }

            override fun onSpeechError(errorMessage: String) {
                _sessionState.value = FizaSessionState.IDLE
                _activeToolNotification.value = errorMessage
            }

            override fun onAudioAmplitude(amplitude: Float) {
                _audioAmplitude.value = amplitude
            }

            override fun onSpeakingStarted(text: String) {
                _sessionState.value = FizaSessionState.SPEAKING
                _fizaSpokenText.value = text
            }

            override fun onSpeakingFinished() {
                _sessionState.value = FizaSessionState.IDLE
                _audioAmplitude.value = 0f
            }
        }
    }

    fun onMicClicked() {
        when (_sessionState.value) {
            FizaSessionState.SPEAKING -> {
                audioEngine.interrupt()
                _sessionState.value = FizaSessionState.IDLE
                startListening()
            }
            FizaSessionState.LISTENING -> {
                audioEngine.stopListening()
                _sessionState.value = FizaSessionState.IDLE
            }
            FizaSessionState.THINKING -> {
                audioEngine.interrupt()
                _sessionState.value = FizaSessionState.IDLE
            }
            FizaSessionState.IDLE, FizaSessionState.CONNECTING -> {
                startListening()
            }
        }
    }

    fun startListening() {
        audioEngine.interrupt()
        _sessionState.value = FizaSessionState.LISTENING
        audioEngine.startListening()
    }

    fun interruptSpeaking() {
        audioEngine.interrupt()
        _sessionState.value = FizaSessionState.IDLE
        _audioAmplitude.value = 0f
    }

    fun toggleThemeMode() {
        _displayThemeMode.value = if (_displayThemeMode.value == ThemeDisplayMode.ANIME_AVATAR) {
            ThemeDisplayMode.SCI_FI_ORB
        } else {
            ThemeDisplayMode.ANIME_AVATAR
        }
    }

    fun connectServer(ipOrKey: String) {
        val trimmed = ipOrKey.trim()
        if (trimmed.isBlank()) {
            _serverConnectionMessage.value = "Please enter an IP address or API key."
            return
        }

        _isConnectingServer.value = true
        _serverConnectionMessage.value = "Testing server connection..."

        viewModelScope.launch {
            val result = geminiService.testConnection(trimmed)
            _isConnectingServer.value = false
            _serverConnectionMessage.value = result.message

            if (result.isSuccess) {
                _isServerConnected.value = true
                _serverOrKey.value = trimmed
                prefs.edit()
                    .putString("saved_server_or_key", trimmed)
                    .putBoolean("is_server_connected", true)
                    .apply()

                val connectedWelcome = "Server connected! I am online and have full access to your phone 😏"
                _fizaSpokenText.value = connectedWelcome
                _conversationHistory.value = _conversationHistory.value + ChatMessage(
                    sender = MessageSender.FIZA,
                    text = connectedWelcome
                )
                audioEngine.speak(connectedWelcome)
            } else {
                _isServerConnected.value = false
                prefs.edit().putBoolean("is_server_connected", false).apply()
            }
        }
    }

    fun processUserMessage(text: String) {
        if (text.isBlank()) {
            _sessionState.value = FizaSessionState.IDLE
            return
        }

        val userMsg = ChatMessage(
            sender = MessageSender.USER,
            text = text.trim()
        )
        _conversationHistory.value = _conversationHistory.value + userMsg
        _sessionState.value = FizaSessionState.THINKING

        viewModelScope.launch {
            val response = geminiService.sendMessage(
                userMessage = text,
                conversationHistory = _conversationHistory.value,
                personalityMode = _personalityMode.value,
                customApiKey = _serverOrKey.value.takeIf { it.isNotBlank() }
            )

            // Execute tools if any
            var toolFeedback = ""
            for (tool in response.toolCalls) {
                val result = toolExecutor.execute(tool)
                if (result.userFriendlyMessage.isNotBlank()) {
                    toolFeedback = result.userFriendlyMessage
                    _activeToolNotification.value = result.userFriendlyMessage
                }
            }

            val finalReply = response.replyText
            val fizaMsg = ChatMessage(
                sender = MessageSender.FIZA,
                text = finalReply,
                toolCallInfo = toolFeedback.takeIf { it.isNotBlank() }
            )
            _conversationHistory.value = _conversationHistory.value + fizaMsg
            _fizaSpokenText.value = finalReply

            // Voice synthesis
            audioEngine.speak(finalReply)
        }
    }

    fun selectPersonality(mode: PersonalityMode) {
        _personalityMode.value = mode
        val greeting = when (mode) {
            PersonalityMode.SASSY_FLIRTY -> "Sassy mode unlocked! Let's see if you can keep up with me 😏"
            PersonalityMode.PLAYFUL_BESTIE -> "Bestie mode active! Grab your coffee, we have things to discuss 💅"
            PersonalityMode.QUEEN_ROAST -> "Roast mode enabled. Don't say I didn't warn you, sweetie 🔥"
            PersonalityMode.SWEET_TEASING -> "Sweet and teasing mode on! I'll be nice... mostly ✨"
        }
        _fizaSpokenText.value = greeting
        _conversationHistory.value = _conversationHistory.value + ChatMessage(
            sender = MessageSender.FIZA,
            text = greeting
        )
        audioEngine.speak(greeting)
    }

    fun clearNotification() {
        _activeToolNotification.value = null
    }

    fun clearHistory() {
        _conversationHistory.value = listOf(
            ChatMessage(
                sender = MessageSender.FIZA,
                text = "Clean slate! What are we talking about now? 😏"
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.destroy()
    }
}
