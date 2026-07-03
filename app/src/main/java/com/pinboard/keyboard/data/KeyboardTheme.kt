package com.pinboard.keyboard.data

import android.graphics.Color

/** Colors used to paint the keyboard. Applied directly (independent of the
 *  app day/night theme) so the in-app toggle always controls the look. */
data class KeyboardTheme(
    val background: Int,
    val panelBg: Int,
    val keyBg: Int,
    val keyFnBg: Int,
    val accentBg: Int,
    val keyText: Int,
    val fnText: Int,
    val accentText: Int,
    val mutedText: Int,
    val primaryContainer: Int,
    val onPrimaryContainer: Int
) {
    companion object {
        val LIGHT = KeyboardTheme(
            background = Color.parseColor("#ECE9F5"),
            panelBg = Color.parseColor("#FFFFFF"),
            keyBg = Color.parseColor("#E7E0F0"),
            keyFnBg = Color.parseColor("#D6CBE6"),
            accentBg = Color.parseColor("#6750A4"),
            keyText = Color.parseColor("#1B1A22"),
            fnText = Color.parseColor("#3A3543"),
            accentText = Color.parseColor("#FFFFFF"),
            mutedText = Color.parseColor("#6B6677"),
            primaryContainer = Color.parseColor("#EADDFF"),
            onPrimaryContainer = Color.parseColor("#21005D")
        )

        val DARK = KeyboardTheme(
            background = Color.parseColor("#141217"),
            panelBg = Color.parseColor("#1B1822"),
            keyBg = Color.parseColor("#2C2934"),
            keyFnBg = Color.parseColor("#3A3644"),
            accentBg = Color.parseColor("#D0BCFF"),
            keyText = Color.parseColor("#E8E2F1"),
            fnText = Color.parseColor("#C9C3D6"),
            accentText = Color.parseColor("#381E72"),
            mutedText = Color.parseColor("#9A93AB"),
            primaryContainer = Color.parseColor("#4F378B"),
            onPrimaryContainer = Color.parseColor("#EADDFF")
        )

        fun load(prefs: PrefsManager): KeyboardTheme =
            if (prefs.darkMode) DARK else LIGHT
    }
}