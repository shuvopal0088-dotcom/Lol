-package com.pinboard.keyboard.data

import android.content.Context

/**
 * Lightweight settings store. Holds theme, generator length, and the three
 * quick-paste slots (1, 2, 3). Uses SharedPreferences only.
 */
class PrefsManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("pinboard_prefs", Context.MODE_PRIVATE)

    var darkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK, false) // light by default
        set(value) = prefs.edit().putBoolean(KEY_DARK, value).apply()

    var genLength: Int
        get() = prefs.getInt(KEY_LEN, 7).coerceIn(6, 20)
        set(value) = prefs.edit().putInt(KEY_LEN, value.coerceIn(6, 20)).apply()

    fun slotText(id: Int): String =
        prefs.getString("slot_${id}_text", defaultText(id)) ?: defaultText(id)

    fun setSlotText(id: Int, text: String) =
        prefs.edit().putString("slot_${id}_text", text).apply()

    /** Slots 1 & 2 default to paste+enter; slot 3 defaults to paste-only. */
    fun slotEnter(id: Int): Boolean = prefs.getBoolean("slot_${id}_enter", id != 3)

    fun setSlotEnter(id: Int, enter: Boolean) =
        prefs.edit().putBoolean("slot_${id}_enter", enter).apply()

    private fun defaultText(id: Int): String = when (id) {
        1 -> "Thank you so much! 🙏"
        2 -> "Sure, that works for me 👍"
        3 -> "Let me check and confirm."
        else -> ""
    }

    companion object {
        private const val KEY_DARK = "dark_mode"
        private const val KEY_LEN = "gen_length"
    }
}