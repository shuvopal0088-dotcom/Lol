package com.pinboard.keyboard.data

/** A single pinned message. Stored locally as JSON in SharedPreferences. */
data class PinnedMessage(
    val id: String,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long
)