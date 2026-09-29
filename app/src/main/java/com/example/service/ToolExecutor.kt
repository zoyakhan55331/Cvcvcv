package com.example.service

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import com.example.model.ToolCall
import com.example.model.ToolExecutionResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ToolExecutor(private val context: Context) {

    private var isTorchOn = false

    fun execute(toolCall: ToolCall): ToolExecutionResult {
        return try {
            when (toolCall.name) {
                "openWebsite" -> executeOpenWebsite(toolCall)
                "toggleFlashlight" -> executeToggleFlashlight(toolCall)
                "makeCall" -> executeMakeCall(toolCall)
                "sendSms" -> executeSendSms(toolCall)
                "openWhatsApp" -> executeOpenWhatsApp(toolCall)
                "openCamera" -> executeOpenCamera()
                "openGallery" -> executeOpenGallery()
                "openSettings" -> executeOpenSettings(toolCall)
                "adjustVolume" -> executeAdjustVolume(toolCall)
                "vibratePhone" -> executeVibrate()
                "setTimer" -> executeSetTimer(toolCall)
                "getDeviceStatus" -> executeGetDeviceStatus()
                else -> ToolExecutionResult(
                    toolName = toolCall.name,
                    success = false,
                    userFriendlyMessage = "Unknown command: ${toolCall.name}"
                )
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                toolName = toolCall.name,
                success = false,
                userFriendlyMessage = "Action couldn't complete: ${e.localizedMessage}"
            )
        }
    }

    private fun executeOpenWebsite(toolCall: ToolCall): ToolExecutionResult {
        val rawUrl = toolCall.arguments["url"] ?: toolCall.arguments["siteName"] ?: "https://www.google.com"
        val finalUrl = when {
            rawUrl.startsWith("http://") || rawUrl.startsWith("https://") -> rawUrl
            rawUrl.contains("youtube", ignoreCase = true) -> "https://www.youtube.com"
            rawUrl.contains("instagram", ignoreCase = true) -> "https://www.instagram.com"
            rawUrl.contains("spotify", ignoreCase = true) -> "https://open.spotify.com"
            rawUrl.contains("twitter", ignoreCase = true) || rawUrl.contains("x.com", ignoreCase = true) -> "https://x.com"
            rawUrl.contains("github", ignoreCase = true) -> "https://github.com"
            rawUrl.contains("reddit", ignoreCase = true) -> "https://www.reddit.com"
            rawUrl.contains(".") -> "https://$rawUrl"
            else -> "https://www.google.com/search?q=${Uri.encode(rawUrl)}"
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return ToolExecutionResult(
            toolName = "openWebsite",
            success = true,
            userFriendlyMessage = "Opened $finalUrl"
        )
    }

    private fun executeToggleFlashlight(toolCall: ToolCall): ToolExecutionResult {
        val enableStr = toolCall.arguments["enable"] ?: toolCall.arguments["state"] ?: "toggle"
        val enable = when (enableStr.lowercase()) {
            "on", "true", "enable" -> true
            "off", "false", "disable" -> false
            else -> !isTorchOn
        }

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (cameraManager != null) {
            try {
                for (cameraId in cameraManager.cameraIdList) {
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                    if (hasFlash) {
                        cameraManager.setTorchMode(cameraId, enable)
                        isTorchOn = enable
                        return ToolExecutionResult(
                            toolName = "toggleFlashlight",
                            success = true,
                            userFriendlyMessage = if (enable) "Flashlight turned ON 🔦" else "Flashlight turned OFF 🔦"
                        )
                    }
                }
            } catch (e: Exception) {
                return ToolExecutionResult(
                    toolName = "toggleFlashlight",
                    success = false,
                    userFriendlyMessage = "Flashlight control error: ${e.message}"
                )
            }
        }
        return ToolExecutionResult(
            toolName = "toggleFlashlight",
            success = false,
            userFriendlyMessage = "Flashlight not available on this device"
        )
    }

    private fun executeMakeCall(toolCall: ToolCall): ToolExecutionResult {
        val number = toolCall.arguments["number"] ?: toolCall.arguments["phoneNumber"] ?: ""
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$number")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return ToolExecutionResult(
            toolName = "makeCall",
            success = true,
            userFriendlyMessage = if (number.isNotBlank()) "Opening dialer for $number 📞" else "Opening dialer 📞"
        )
    }

    private fun executeSendSms(toolCall: ToolCall): ToolExecutionResult {
        val number = toolCall.arguments["number"] ?: ""
        val message = toolCall.arguments["message"] ?: ""
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$number")
            if (message.isNotBlank()) {
                putExtra("sms_body", message)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return ToolExecutionResult(
            toolName = "sendSms",
            success = true,
            userFriendlyMessage = "Opened Messages app 💬"
        )
    }

    private fun executeOpenWhatsApp(toolCall: ToolCall): ToolExecutionResult {
        val phone = toolCall.arguments["number"] ?: toolCall.arguments["phone"] ?: ""
        val message = toolCall.arguments["message"] ?: ""
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        val url = if (cleanPhone.isNotBlank()) {
            "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}"
        } else {
            "https://api.whatsapp.com/send?text=${Uri.encode(message)}"
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return ToolExecutionResult(
            toolName = "openWhatsApp",
            success = true,
            userFriendlyMessage = "Opening WhatsApp 🟢"
        )
    }

    private fun executeOpenCamera(): ToolExecutionResult {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return ToolExecutionResult(
            toolName = "openCamera",
            success = true,
            userFriendlyMessage = "Camera launched 📸"
        )
    }

    private fun executeOpenGallery(): ToolExecutionResult {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            type = "image/*"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return ToolExecutionResult(
            toolName = "openGallery",
            success = true,
            userFriendlyMessage = "Opening Photos & Gallery 🖼️"
        )
    }

    private fun executeOpenSettings(toolCall: ToolCall): ToolExecutionResult {
        val target = toolCall.arguments["target"]?.lowercase() ?: "general"
        val intent = when (target) {
            "wifi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
            "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            "sound", "volume" -> Intent(Settings.ACTION_SOUND_SETTINGS)
            "display" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            "apps" -> Intent(Settings.ACTION_APPLICATION_SETTINGS)
            else -> Intent(Settings.ACTION_SETTINGS)
        }.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return ToolExecutionResult(
            toolName = "openSettings",
            success = true,
            userFriendlyMessage = "Opening $target settings ⚙️"
        )
    }

    private fun executeAdjustVolume(toolCall: ToolCall): ToolExecutionResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val action = toolCall.arguments["action"]?.lowercase() ?: "up"
        if (audioManager != null) {
            when (action) {
                "up", "increase" -> {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    return ToolExecutionResult(
                        toolName = "adjustVolume",
                        success = true,
                        userFriendlyMessage = "Volume turned up 🔊"
                    )
                }
                "down", "decrease" -> {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI
                    )
                    return ToolExecutionResult(
                        toolName = "adjustVolume",
                        success = true,
                        userFriendlyMessage = "Volume turned down 🔉"
                    )
                }
                "mute" -> {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_MUTE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    return ToolExecutionResult(
                        toolName = "adjustVolume",
                        success = true,
                        userFriendlyMessage = "Muted 🔇"
                    )
                }
                "unmute" -> {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_UNMUTE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    return ToolExecutionResult(
                        toolName = "adjustVolume",
                        success = true,
                        userFriendlyMessage = "Unmuted 🔊"
                    )
                }
            }
        }
        return ToolExecutionResult(
            toolName = "adjustVolume",
            success = false,
            userFriendlyMessage = "Could not adjust audio volume"
        )
    }

    private fun executeVibrate(): ToolExecutionResult {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(400)
            }
        }
        return ToolExecutionResult(
            toolName = "vibratePhone",
            success = true,
            userFriendlyMessage = "Buzzed your phone 📳"
        )
    }

    private fun executeSetTimer(toolCall: ToolCall): ToolExecutionResult {
        val seconds = toolCall.arguments["seconds"]?.toIntOrNull() ?: 60
        val label = toolCall.arguments["label"] ?: "Fiza Reminder"
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return ToolExecutionResult(
            toolName = "setTimer",
            success = true,
            userFriendlyMessage = "Setting $seconds-second timer for '$label' ⏳"
        )
    }

    private fun executeGetDeviceStatus(): ToolExecutionResult {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val isCharging = bm?.isCharging ?: false
        val timeStr = SimpleDateFormat("h:mm a, EEEE", Locale.getDefault()).format(Date())

        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
        val freeGb = bytesAvailable / (1024 * 1024 * 1024)

        val statusMessage = buildString {
            append("It's $timeStr. ")
            if (batteryLevel >= 0) {
                append("Battery is $batteryLevel%${if (isCharging) " (charging)" else ""}. ")
            }
            append("Storage has ~${freeGb} GB free.")
        }

        return ToolExecutionResult(
            toolName = "getDeviceStatus",
            success = true,
            userFriendlyMessage = statusMessage
        )
    }
}
