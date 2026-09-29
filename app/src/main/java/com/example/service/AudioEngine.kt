package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

class AudioEngine(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) : TextToSpeech.OnInitListener {

    interface Listener {
        fun onListeningStarted()
        fun onListeningEnded()
        fun onSpeechRecognized(text: String)
        fun onPartialSpeech(partialText: String)
        fun onSpeechError(errorMessage: String)
        fun onAudioAmplitude(amplitude: Float) // 0.0f to 1.0f
        fun onSpeakingStarted(text: String)
        fun onSpeakingFinished()
    }

    var listener: Listener? = null

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var speechWaveJob: Job? = null

    var voicePitch: Float = 1.18f
        set(value) {
            field = value
            textToSpeech?.setPitch(value)
        }

    var voiceRate: Float = 1.05f
        set(value) {
            field = value
            textToSpeech?.setSpeechRate(value)
        }

    init {
        textToSpeech = TextToSpeech(context.applicationContext, this)
        initSpeechRecognizer()
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        listener?.onListeningStarted()
                    }

                    override fun onBeginningOfSpeech() {
                        // User started talking
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // rmsdB typically ranges from -2 to 10
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
                        listener?.onAudioAmplitude(normalized)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        listener?.onListeningEnded()
                    }

                    override fun onError(error: Int) {
                        listener?.onListeningEnded()
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "I didn't catch that, darling. Try again?"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "A bit quiet over there... Tap the mic when you're ready!"
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording hiccup!"
                            SpeechRecognizer.ERROR_NETWORK -> "Network glitch while listening."
                            else -> "Mic issue ($error)"
                        }
                        listener?.onSpeechError(msg)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spoken = matches?.firstOrNull() ?: ""
                        if (spoken.isNotBlank()) {
                            listener?.onSpeechRecognized(spoken)
                        } else {
                            listener?.onListeningEnded()
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull() ?: ""
                        if (partial.isNotBlank()) {
                            listener?.onPartialSpeech(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.let { tts ->
                val result = tts.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.getDefault())
                }

                // Pick female voice if available
                try {
                    val voices = tts.voices
                    if (voices != null) {
                        val femaleVoice = voices.firstOrNull { voice ->
                            val name = voice.name.lowercase()
                            (name.contains("female") || name.contains("en-us-x-sfg") || name.contains("en-us-x-iol")) &&
                                    !voice.isNetworkConnectionRequired
                        } ?: voices.firstOrNull { it.name.lowercase().contains("female") }

                        if (femaleVoice != null) {
                            tts.voice = femaleVoice
                        }
                    }
                } catch (ignored: Exception) {}

                tts.setPitch(voicePitch)
                tts.setSpeechRate(voiceRate)

                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        startSyntheticWaveform()
                    }

                    override fun onDone(utteranceId: String?) {
                        stopSyntheticWaveform()
                        coroutineScope.launch(Dispatchers.Main) {
                            listener?.onSpeakingFinished()
                            listener?.onAudioAmplitude(0f)
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        stopSyntheticWaveform()
                        coroutineScope.launch(Dispatchers.Main) {
                            listener?.onSpeakingFinished()
                            listener?.onAudioAmplitude(0f)
                        }
                    }
                })

                isTtsInitialized = true
            }
        }
    }

    fun startListening() {
        stopSpeaking()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            listener?.onSpeechError("Could not start microphone: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (ignored: Exception) {}
        listener?.onListeningEnded()
    }

    fun speak(text: String) {
        if (!isTtsInitialized) {
            listener?.onSpeakingFinished()
            return
        }

        // Clean any emojis that TTS engines might read awkwardly
        val cleanedText = text.replace(Regex("[\\p{So}\\p{Cn}]"), "").trim()
        val utteranceId = "fiza_utterance_${System.currentTimeMillis()}"

        coroutineScope.launch(Dispatchers.Main) {
            listener?.onSpeakingStarted(text)
        }

        textToSpeech?.speak(
            if (cleanedText.isNotBlank()) cleanedText else text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            utteranceId
        )
    }

    fun stopSpeaking() {
        stopSyntheticWaveform()
        try {
            textToSpeech?.stop()
        } catch (ignored: Exception) {}
        listener?.onSpeakingFinished()
        listener?.onAudioAmplitude(0f)
    }

    fun interrupt() {
        stopSpeaking()
        stopListening()
    }

    private fun startSyntheticWaveform() {
        stopSyntheticWaveform()
        speechWaveJob = coroutineScope.launch(Dispatchers.Default) {
            var phase = 0.0
            while (isActive) {
                // Natural speech amplitude modulation
                val base = 0.45f + 0.35f * kotlin.math.sin(phase).toFloat()
                val jitter = Random.nextFloat() * 0.2f
                val amp = (base + jitter).coerceIn(0.15f, 0.98f)
                phase += 0.3
                coroutineScope.launch(Dispatchers.Main) {
                    listener?.onAudioAmplitude(amp)
                }
                delay(60)
            }
        }
    }

    private fun stopSyntheticWaveform() {
        speechWaveJob?.cancel()
        speechWaveJob = null
    }

    fun destroy() {
        interrupt()
        try {
            speechRecognizer?.destroy()
        } catch (ignored: Exception) {}
        try {
            textToSpeech?.shutdown()
        } catch (ignored: Exception) {}
        speechRecognizer = null
        textToSpeech = null
    }
}
