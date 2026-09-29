package com.example.model

enum class FizaSessionState {
    IDLE,
    CONNECTING,
    LISTENING,
    THINKING,
    SPEAKING
}

enum class PersonalityMode(val displayName: String, val emoji: String, val tagline: String) {
    SASSY_FLIRTY(
        displayName = "Sassy & Flirty",
        emoji = "😏",
        tagline = "Playful banter, witty comebacks, and charming attitude"
    ),
    PLAYFUL_BESTIE(
        displayName = "Playful Bestie",
        emoji = "💅",
        tagline = "Your confident girlfriend who tells it like it is"
    ),
    QUEEN_ROAST(
        displayName = "Queen Roast",
        emoji = "🔥",
        tagline = "Sharp, sarcastic burns and zero-filter witty commentary"
    ),
    SWEET_TEASING(
        displayName = "Sweet & Teasing",
        emoji = "✨",
        tagline = "Gentle teasing, warm affection, and playful spark"
    )
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val toolCallInfo: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER,
    FIZA,
    SYSTEM
}

data class ToolCall(
    val name: String,
    val arguments: Map<String, String>
)

data class ToolExecutionResult(
    val toolName: String,
    val success: Boolean,
    val userFriendlyMessage: String
)
