package com.rk.terminal.model

data class Media(
    val id: String,
    val title: String,
    val description: String,
    val type: MediaType,
    val telegramFileId: Long, // Referring to the message/video in Telegram
    val chatId: Long
)

enum class MediaType {
    MOVIE, SERIES
}

data class AppDatabase(
    val medias: List<Media> = emptyList()
)
