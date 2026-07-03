package com.pinboard.keyboard.ui

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.widget.Button
import android.widget.LinearLayout
import com.pinboard.keyboard.InputProxy
import com.pinboard.keyboard.data.KeyboardTheme

/**
 * The letter / number / symbol key grid. Built from Buttons so it works on
 * every Android version with no deprecated KeyboardView APIs.
 */
class KeysView(
    context: Context,
    private val proxy: InputProxy,
    private val theme: KeyboardTheme
) : LinearLayout(context) {

    private enum class Page { LETTERS, NUMBERS, SYMBOLS }
    private enum class Kind { CHAR, FN, ACCENT }

    private data class K(
        val label: () -> String,
        val weight: Float,
        val kind: Kind,
        val action: () -> Unit
    )

    private var page = Page.LETTERS
    private var shift = false

    init {
        orientation = VERTICAL
        render()
    }

    private fun render() {
        removeAllViews()
        val rows = when (page) {
            Page.LETTERS -> lettersRows()
            Page.NUMBERS -> numbersRows()
            Page.SYMBOLS -> symbolsRows()
        }
        for (row in rows) addView(buildRow(row))
    }

    private fun typeChar(c: String) {
        val out = if (shift && page == Page.LETTERS) c.uppercase() else c
        proxy.commit(out)
        if (shift && page == Page.LETTERS) {
            shift = false
            render()
        }
    }

    /** A single letter key. `c` must already be a 1-char String (not a Char). */
    private fun letterKey(c: String): K =
        K({ if (shift && page == Page.LETTERS) c.uppercase() else c }, 1f, Kind.CHAR) { typeChar(c) }

    private fun lettersRows(): List<List<K>> {
        val r1 = "qwertyuiop".map { letterKey(it.toString()) }
        val r2 = "asdfghjkl".map { letterKey(it.toString()) }
        val r3 = mutableListOf<K>()
        r3 += K({ "⇧" }, 1.5f, Kind.FN) { shift = !shift; render() }
        "zxcvbnm".forEach { r3 += letterKey(it.toString()) }
        r3 += K({ "⌫" }, 1.5f, Kind.FN) { proxy.deleteChar() }
        val r4 = listOf(
            K({ "123" }, 1.5f, Kind.FN) { page = Page.NUMBERS; shift = false; render() },
            K({ "," }, 1f, Kind.CHAR) { proxy.commit(",") },
            K({ "🌐" }, 5f, Kind.CHAR) { proxy.commit(" ") },
            K({ "." }, 1f, Kind.CHAR) { proxy.commit(".") },
            K({ "⏎" }, 2f, Kind.ACCENT) { proxy.enter() }
        )
        return listOf(r1, r2, r3, r4)
    }

    private fun numbersRows(): List<List<K>> {
        val r1 = "1234567890".map { c -> K({ c.toString() }, 1f, Kind.CHAR) { proxy.commit(c.toString()) } }
        val r2 = listOf("-", "/", ":", ";", "(", ")", "$", "&", "@", "\"")
            .map { c -> K({ c }, 1f, Kind.CHAR) { proxy.commit(c) } }
        val r3 = mutableListOf<K>()
        r3 += K({ "#+=" }, 1.5f, Kind.FN) { page = Page.SYMBOLS; render() }
        listOf(".", ",", "?", "!", "'").forEach { c -> r3 += K({ c }, 1f, Kind.CHAR) { proxy.commit(c) } }
        r3 += K({ "⌫" }, 1.5f, Kind.FN) { proxy.deleteChar() }
        val r4 = listOf(
            K({ "ABC" }, 1.5f, Kind.FN) { page = Page.LETTERS; render() },
            K({ "," }, 1f, Kind.CHAR) { proxy.commit(",") },
            K({ "🌐" }, 5f, Kind.CHAR) { proxy.commit(" ") },
            K({ "." }, 1f, Kind.CHAR) { proxy.commit(".") },
            K({ "⏎" }, 2f, Kind.ACCENT) { proxy.enter() }
        )
        return listOf(r1, r2, r3, r4)
    }

    private fun symbolsRows(): List<List<K>> {
        val r1 = listOf("[", "]", "{", "}", "#", "%", "^", "*", "+", "=")
            .map { c -> K({ c }, 1f, Kind.CHAR) { proxy.commit(c) } }
        val r2 = listOf("_", "\\", "|", "~", "<", ">", "€", "£", "¥", "•")
            .map { c -> K({ c }, 1f, Kind.CHAR) { proxy.commit(c) } }
        val r3 = mutableListOf<K>()
        r3 += K({ "123" }, 1.5f, Kind.FN) { page = Page.NUMBERS; render() }
        listOf(".", ",", "\"", "'", ";").forEach { c -> r3 += K({ c }, 1f, Kind.CHAR) { proxy.commit(c) } }
        r3 += K({ "⌫" }, 1.5f, Kind.FN) { proxy.deleteChar() }
        val r4 = listOf(
            K({ "ABC" }, 1.5f, Kind.FN) { page = Page.LETTERS; render() },
            K({ "," }, 1f, Kind.CHAR) { proxy.commit(",") },
            K({ "🌐" }, 5f, Kind.CHAR) { proxy.commit(" ") },
            K({ "." }, 1f, Kind.CHAR) { proxy.commit(".") },
            K({ "⏎" }, 2f, Kind.ACCENT) { proxy.enter() }
        )
        return listOf(r1, r2, r3, r4)
    }

    private fun buildRow(keys: List<K>): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val m = context.dp(2)
            setPadding(m, m, m, m)
        }
        for (k in keys) {
            val h = context.dp(44)
            val b = Button(context).apply {
                this.text = k.label()
                isAllCaps = false
                transformationMethod = null
                typeface = Typeface.DEFAULT
                gravity = Gravity.CENTER
                minHeight = 0
                minimumHeight = h
                setPadding(0, 0, 0, 0)
                when (k.kind) {
                    Kind.CHAR -> {
                        background = roundedDrawable(context, theme.keyBg, 9f)
                        setTextColor(theme.keyText)
                        textSize = 16f
                    }
                    Kind.FN -> {
                        background = roundedDrawable(context, theme.keyFnBg, 9f)
                        setTextColor(theme.fnText)
                        textSize = 13f
                    }
                    Kind.ACCENT -> {
                        background = roundedDrawable(context, theme.accentBg, 9f)
                        setTextColor(theme.accentText)
                        textSize = 16f
                    }
                }
                setOnClickListener {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    k.action()
                }
            }
            val lp = LinearLayout.LayoutParams(0, h, k.weight).apply {
                marginEnd = context.dp(3)
            }
            row.addView(b, lp)
        }
        return row
    }
}
