package com.pinboard.keyboard

import com.pinboard.keyboard.data.PrefsManager
import com.pinboard.keyboard.data.PinStore

/**
 * Bridge between the keyboard UI and the InputMethodService.
 * Every input action is safe to call even when no InputConnection exists
 * (the IME null-checks internally), which prevents the "app stops" crash.
 */
interface InputProxy {
    /** Insert plain text at the cursor. */
    fun commit(text: String)

    /** Insert text AND press Enter (send) in one action. */
    fun commitAndEnter(text: String)

    /** Backspace one character. */
    fun deleteChar()

    /** Press Enter / perform the field's action (send, go, done…). */
    fun enter()

    /** Clear all text in the field. */
    fun clear()

    /** Hide the keyboard. */
    fun closeKeyboard()

    val store: PinStore
    val prefs: PrefsManager

    /** Run a block on a background thread (DB / heavy reads). */
    fun runAsync(action: () -> Unit)

    /** Run a block back on the UI thread. */
    fun runOnUi(action: () -> Unit)
}