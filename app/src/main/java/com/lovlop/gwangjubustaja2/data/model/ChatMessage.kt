package com.lovlop.gwangjubustaja2.data.model

sealed class ChatMessage {

    // 1. 사용자(User) 메시지 말풍선
    data class User(
        val text: String,
        val timestamp: String = getCurrentTime()
    ) : ChatMessage()

    // 2. 챗봇(Bot) 메시지 말풍선
    data class Bot(
        val text: String,
        val timestamp: String = getCurrentTime()
    ) : ChatMessage()

    object Loading : ChatMessage()
}

private fun getCurrentTime(): String {
    val sdf = java.text.SimpleDateFormat("a h:mm", java.util.Locale.KOREA)
    return sdf.format(java.util.Date())
}