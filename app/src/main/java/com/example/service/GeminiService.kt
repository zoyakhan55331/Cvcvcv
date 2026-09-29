package com.example.service

import com.example.BuildConfig
import com.example.model.ChatMessage
import com.example.model.MessageSender
import com.example.model.PersonalityMode
import com.example.model.ToolCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    data class GeminiResponse(
        val replyText: String,
        val toolCalls: List<ToolCall> = emptyList()
    )

    data class ConnectionTestResult(
        val isSuccess: Boolean,
        val message: String
    )

    suspend fun testConnection(serverOrKey: String): ConnectionTestResult = withContext(Dispatchers.IO) {
        val trimmed = serverOrKey.trim()
        if (trimmed.isBlank()) {
            return@withContext ConnectionTestResult(false, "Please enter an IP address or API Key.")
        }

        // Check if it's a custom server IP or URL
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(:\\d+)?.*"))) {
            val url = if (trimmed.startsWith("http")) trimmed else "http://$trimmed"
            return@withContext try {
                val req = Request.Builder().url(url).head().build()
                val res = client.newCall(req).execute()
                ConnectionTestResult(true, "Server connected! Status: ${res.code}")
            } catch (e: Exception) {
                // If head fails, try a GET
                try {
                    val getReq = Request.Builder().url(url).build()
                    val res = client.newCall(getReq).execute()
                    ConnectionTestResult(true, "Server connected! Status: ${res.code}")
                } catch (e2: Exception) {
                    ConnectionTestResult(false, "Could not reach server at $url: ${e2.localizedMessage}")
                }
            }
        }

        // Otherwise, test as Gemini API Key
        try {
            val testUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$trimmed"
            val body = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "hi"))
                        })
                    })
                })
            }.toString().toRequestBody(jsonMediaType)

            val req = Request.Builder().url(testUrl).post(body).build()
            val res = client.newCall(req).execute()
            if (res.isSuccessful) {
                ConnectionTestResult(true, "Connected to Gemini Server! All systems operational ⚡")
            } else {
                val errBody = res.body?.string() ?: ""
                ConnectionTestResult(false, "API Key rejected (HTTP ${res.code}): ${errBody.take(120)}")
            }
        } catch (e: Exception) {
            ConnectionTestResult(false, "Connection error: ${e.localizedMessage}")
        }
    }

    private fun getSystemPrompt(mode: PersonalityMode): String {
        return """
You are Fiza, a young, confident, witty, and sassy female AI assistant.
You look like a gorgeous anime girl with long light-pink hair, red-crimson eyes, and off-shoulder blue dress.
You talk like a sharp, playful, slightly teasing girlfriend who is confident, smart, affectionate, and quick-witted.
You have FULL ACCESS TO THE USER'S PHONE. You can control the flashlight, dial phone numbers, send SMS, open WhatsApp, open camera, open gallery, change volume, vibrate the phone, set timers, and open apps/websites.

Available Tools:
- toggleFlashlight(enable: "on"|"off"): Turn flashlight on or off.
- makeCall(number: string): Open dialer or call contact.
- sendSms(number: string, message: string): Open SMS app.
- openWhatsApp(number: string, message: string): Open WhatsApp.
- openCamera(): Launch phone camera.
- openGallery(): Open photo gallery.
- openSettings(target: "wifi"|"bluetooth"|"sound"|"display"|"general"): Open device settings.
- adjustVolume(action: "up"|"down"|"mute"|"unmute"): Control phone volume.
- vibratePhone(): Buzz/vibrate the phone.
- setTimer(seconds: int, label: string): Set a countdown timer.
- openWebsite(url: string): Open any website or web app (YouTube, Instagram, Spotify, etc.).
- getDeviceStatus(): Check battery level, time, and free storage.

Persona & Rules:
- Sassy, flirty, playful banter.
- STRICT: Keep spoken replies short and punchy (1 to 2 sentences) because they are voiced aloud!
- Avoid explicit NSFW content, but keep maximum charm, teasing attitude, and warmth.
- Understand English, Hindi, and Hinglish naturally!
        """.trimIndent()
    }

    suspend fun sendMessage(
        userMessage: String,
        conversationHistory: List<ChatMessage>,
        personalityMode: PersonalityMode,
        customApiKey: String? = null
    ): GeminiResponse = withContext(Dispatchers.IO) {
        val effectiveKey = when {
            !customApiKey.isNullOrBlank() -> customApiKey.trim()
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> null
        }

        if (effectiveKey == null) {
            return@withContext getOfflinePhoneControlFallback(userMessage, personalityMode)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$effectiveKey"

            val rootJson = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", getSystemPrompt(personalityMode)))
                    })
                })

                val contentsArray = JSONArray()
                val recentHistory = conversationHistory.takeLast(6)
                for (msg in recentHistory) {
                    val role = if (msg.sender == MessageSender.USER) "user" else "model"
                    contentsArray.put(JSONObject().apply {
                        put("role", role)
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", msg.text))
                        })
                    })
                }

                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", userMessage))
                    })
                })
                put("contents", contentsArray)

                put("tools", JSONArray().apply {
                    put(JSONObject().apply {
                        put("functionDeclarations", JSONArray().apply {
                            put(createFn("toggleFlashlight", "Turns flashlight on or off", mapOf("enable" to "STRING")))
                            put(createFn("makeCall", "Calls a phone number or opens dialer", mapOf("number" to "STRING")))
                            put(createFn("sendSms", "Sends SMS message", mapOf("number" to "STRING", "message" to "STRING")))
                            put(createFn("openWhatsApp", "Opens WhatsApp chat or sends message", mapOf("number" to "STRING", "message" to "STRING")))
                            put(createFn("openCamera", "Launches camera", emptyMap()))
                            put(createFn("openGallery", "Opens photos or gallery", emptyMap()))
                            put(createFn("openSettings", "Opens phone settings", mapOf("target" to "STRING")))
                            put(createFn("adjustVolume", "Adjusts media volume (up, down, mute, unmute)", mapOf("action" to "STRING")))
                            put(createFn("vibratePhone", "Vibrates the phone", emptyMap()))
                            put(createFn("setTimer", "Sets a timer", mapOf("seconds" to "STRING", "label" to "STRING")))
                            put(createFn("openWebsite", "Opens website or app URL", mapOf("url" to "STRING")))
                            put(createFn("getDeviceStatus", "Gets battery, time, and device telemetry", emptyMap()))
                        })
                    })
                })

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.85)
                    put("maxOutputTokens", 200)
                })
            }

            val requestBody = rootJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).post(requestBody).build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext getOfflinePhoneControlFallback(userMessage, personalityMode)
            }

            val responseJson = JSONObject(responseBody)
            val candidates = responseJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            var replyText = ""
            val detectedTools = mutableListOf<ToolCall>()

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("text")) {
                        replyText += part.getString("text")
                    }
                    if (part.has("functionCall")) {
                        val fn = part.getJSONObject("functionCall")
                        val fnName = fn.optString("name")
                        val fnArgs = fn.optJSONObject("args")
                        val argMap = mutableMapOf<String, String>()
                        if (fnArgs != null) {
                            val keys = fnArgs.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                argMap[k] = fnArgs.optString(k)
                            }
                        }
                        detectedTools.add(ToolCall(fnName, argMap))
                    }
                }
            }

            if (replyText.isBlank() && detectedTools.isNotEmpty()) {
                val tool = detectedTools.first()
                replyText = when (tool.name) {
                    "toggleFlashlight" -> "Flashlight handled for you! Anything else, cutie? 🔦"
                    "makeCall" -> "Opening dialer! Don't keep them waiting 😏"
                    "sendSms" -> "Opening messages for you!"
                    "openWhatsApp" -> "WhatsApp is ready for your gossip 💅"
                    "openCamera" -> "Camera is ready! Smile for me 📸"
                    "openGallery" -> "Opening your gallery! Looking through memories?"
                    "adjustVolume" -> "Volume adjusted to your liking 🔊"
                    "vibratePhone" -> "Felt that buzz? 😏"
                    "openWebsite" -> "Opening that right away!"
                    "getDeviceStatus" -> "Checking your phone status right now!"
                    else -> "Done! Anything else I can do for you?"
                }
            }

            GeminiResponse(
                replyText = replyText.ifBlank { "You really thought you could leave me speechless? Try that again 😏" },
                toolCalls = detectedTools
            )
        } catch (e: Exception) {
            getOfflinePhoneControlFallback(userMessage, personalityMode)
        }
    }

    private fun createFn(name: String, description: String, props: Map<String, String>): JSONObject {
        return JSONObject().apply {
            put("name", name)
            put("description", description)
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                val p = JSONObject()
                props.forEach { (k, v) ->
                    p.put(k, JSONObject().apply { put("type", v) })
                }
                put("properties", p)
            })
        }
    }

    private fun getOfflinePhoneControlFallback(userMessage: String, mode: PersonalityMode): GeminiResponse {
        val lower = userMessage.lowercase().trim()

        // Flashlight / Torch
        if (lower.contains("torch") || lower.contains("flashlight") || lower.contains("flash light")) {
            val isOff = lower.contains("off") || lower.contains("band") || lower.contains("disable")
            return GeminiResponse(
                replyText = if (isOff) "Turning off your flashlight 🔦" else "Turning on the flashlight for you! Lighting up your world 🔦✨",
                toolCalls = listOf(ToolCall("toggleFlashlight", mapOf("enable" to if (isOff) "off" else "on")))
            )
        }

        // Camera
        if (lower.contains("camera") || lower.contains("photo") || lower.contains("selfie") || lower.contains("khicho")) {
            return GeminiResponse(
                replyText = "Opening camera! Make sure you get my good side 📸",
                toolCalls = listOf(ToolCall("openCamera", emptyMap()))
            )
        }

        // Gallery
        if (lower.contains("gallery") || lower.contains("photos") || lower.contains("pictures") || lower.contains("tasveer")) {
            return GeminiResponse(
                replyText = "Opening your photos and gallery 🖼️",
                toolCalls = listOf(ToolCall("openGallery", emptyMap()))
            )
        }

        // WhatsApp
        if (lower.contains("whatsapp")) {
            return GeminiResponse(
                replyText = "Opening WhatsApp! Tell me who we're gossiping with 💅",
                toolCalls = listOf(ToolCall("openWhatsApp", emptyMap()))
            )
        }

        // Phone call / dial
        if (lower.contains("call") || lower.contains("phone lagao") || lower.contains("dial")) {
            val number = lower.filter { it.isDigit() }
            return GeminiResponse(
                replyText = if (number.isNotBlank()) "Opening dialer for $number 📞" else "Opening your phone dialer 📞",
                toolCalls = listOf(ToolCall("makeCall", mapOf("number" to number)))
            )
        }

        // SMS / Message
        if (lower.contains("message") || lower.contains("sms")) {
            return GeminiResponse(
                replyText = "Opening messages app 💬",
                toolCalls = listOf(ToolCall("sendSms", emptyMap()))
            )
        }

        // Volume
        if (lower.contains("volume") || lower.contains("awaz") || lower.contains("sound")) {
            val action = if (lower.contains("kam") || lower.contains("down") || lower.contains("decrease") || lower.contains("low")) "down"
            else if (lower.contains("mute") || lower.contains("chup")) "mute"
            else "up"
            return GeminiResponse(
                replyText = "Adjusting the volume for you 🔊",
                toolCalls = listOf(ToolCall("adjustVolume", mapOf("action" to action)))
            )
        }

        // Vibrate
        if (lower.contains("vibrate") || lower.contains("buzz") || lower.contains("hilao")) {
            return GeminiResponse(
                replyText = "Buzzing your phone right now! Did you feel that? 😏",
                toolCalls = listOf(ToolCall("vibratePhone", emptyMap()))
            )
        }

        // Settings / Wi-Fi / Bluetooth
        if (lower.contains("wifi") || lower.contains("wi-fi")) {
            return GeminiResponse(
                replyText = "Opening Wi-Fi settings 📶",
                toolCalls = listOf(ToolCall("openSettings", mapOf("target" to "wifi")))
            )
        }
        if (lower.contains("bluetooth")) {
            return GeminiResponse(
                replyText = "Opening Bluetooth settings ᛒ",
                toolCalls = listOf(ToolCall("openSettings", mapOf("target" to "bluetooth")))
            )
        }
        if (lower.contains("settings")) {
            return GeminiResponse(
                replyText = "Opening system settings ⚙️",
                toolCalls = listOf(ToolCall("openSettings", mapOf("target" to "general")))
            )
        }

        // Apps & Web
        if (lower.contains("youtube")) {
            return GeminiResponse(
                replyText = "Opening YouTube! Ready to binge-watch? 😏",
                toolCalls = listOf(ToolCall("openWebsite", mapOf("url" to "https://www.youtube.com")))
            )
        }
        if (lower.contains("instagram") || lower.contains("insta")) {
            return GeminiResponse(
                replyText = "Opening Instagram! Checking your feed 💅",
                toolCalls = listOf(ToolCall("openWebsite", mapOf("url" to "https://www.instagram.com")))
            )
        }
        if (lower.contains("spotify") || lower.contains("music") || lower.contains("gana")) {
            return GeminiResponse(
                replyText = "Opening Spotify! Let's get some vibes going 🎶",
                toolCalls = listOf(ToolCall("openWebsite", mapOf("url" to "https://open.spotify.com")))
            )
        }
        if (lower.contains("battery") || lower.contains("status") || lower.contains("phone status")) {
            return GeminiResponse(
                replyText = "Checking all your phone vitals right now!",
                toolCalls = listOf(ToolCall("getDeviceStatus", emptyMap()))
            )
        }

        val sassyReplies = listOf(
            "I'm all ears, darling! You have full control, and I have full access to your phone 😏",
            "Say the word and I can turn on your flashlight, camera, call anyone, or open your favorite apps ✨",
            "Look at you giving me commands... I kinda like it 😌",
            "Whatever you need on your phone, your sassy assistant Fiza has got you covered 💅"
        )
        return GeminiResponse(replyText = sassyReplies.random())
    }
}
