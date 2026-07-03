package com.pinboard.keyboard.ui

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.pinboard.keyboard.InputProxy
import com.pinboard.keyboard.data.KeyboardTheme
import com.pinboard.keyboard.settings.SettingsActivity
import com.pinboard.keyboard.util.Generator
import kotlin.random.Random

/**
 * Root view of the keyboard: quick-paste bar (1·2·3) + tool bar (📌😀🎲) +
 * a content area that swaps between Keys / Pinned / Emoji.
 */
class KeyboardView(context: Context, private val proxy: InputProxy) :
    LinearLayout(context) {

    private val theme = KeyboardTheme.load(proxy.prefs)
    private val content = FrameLayout(context)
    private val keysView = KeysView(context, proxy, theme)
    private val pinnedPanel = PinnedPanel(context, proxy, theme)
    private val emojiPanel = EmojiPanel(context, proxy, theme)

    private enum class Mode { KEYS, PINNED, EMOJI }
    private var mode = Mode.KEYS

    init {
        orientation = VERTICAL
        setBackgroundColor(theme.background)
        val pad = context.dp(6)
        setPadding(pad, pad, pad, context.dp(8))

        addView(buildQuickPasteBar())
        addView(buildToolbar())
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, context.dp(196)).apply {
            topMargin = context.dp(4)
        })
        showMode(Mode.KEYS)
    }

    // ---------- Quick paste bar (1 · 2 · 3) ----------
    private fun buildQuickPasteBar(): View {
        val row = LinearLayout(context).apply { orientation = HORIZONTAL }
        for (id in 1..3) {
            row.addView(quickPasteButton(id), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = context.dp(6)
            })
        }
        return row
    }

    private fun quickPasteButton(id: Int): View {
        // NOTE: locals are named slotText/slotEnter (not text/enter) so they
        // don't shadow the TextView members inside the `apply {}` blocks.
        val slotText = proxy.prefs.slotText(id)
        val slotEnter = proxy.prefs.slotEnter(id)

        val container = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedDrawable(context, theme.primaryContainer, 12f)
            val p = context.dp(8)
            setPadding(p, context.dp(9), p, context.dp(9))
        }
        val badge = TextView(context).apply {
            this.text = id.toString()
            setTextColor(theme.onPrimaryContainer)
            typeface = Typeface.DEFAULT_BOLD
            textSize = 13f
        }
        val label = TextView(context).apply {
            this.text = if (slotText.isBlank()) "Hold to set" else slotText
            setTextColor(theme.onPrimaryContainer)
            textSize = 12f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        container.addView(badge)
        container.addView(label, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = context.dp(6)
        })
        if (slotEnter && slotText.isNotBlank()) {
            container.addView(TextView(context).apply {
                this.text = "↵"
                setTextColor(theme.onPrimaryContainer)
                textSize = 13f
            })
        }

        // Tap = paste (+Enter if enabled). Long-press = open Settings to edit.
        container.setOnClickListener {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            val t = proxy.prefs.slotText(id)
            if (t.isBlank()) {
                openSettings()
                return@setOnClickListener
            }
            if (proxy.prefs.slotEnter(id)) proxy.commitAndEnter(t) else proxy.commit(t)
        }
        container.setOnLongClickListener {
            openSettings()
            true
        }
        return container
    }

    // ---------- Tool bar (📌 😀 🎲) ----------
    private fun buildToolbar(): View {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val p = context.dp(2)
            setPadding(p, context.dp(6), p, context.dp(2))
        }

        // 📌 Pinned: tap opens panel, long-press = quick new message.
        row.addView(tool("📌 Pinned",
            onClick = { toggle(Mode.PINNED) },
            onLong = { toggle(Mode.PINNED); pinnedPanel.requestNew() }
        ), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = context.dp(6) })

        // 😀 Emoji: tap = random 2-3 emojis + Enter; long-press = open panel.
        row.addView(tool("😀 Emoji",
            onClick = {
                val count = Random.nextInt(2, 4) // 2 or 3
                val pool = EmojiPanel.EMOJIS
                val sb = StringBuilder()
                repeat(count) { sb.append(pool[Random.nextInt(pool.size)]) }
                proxy.commitAndEnter(sb.toString())
            },
            onLong = { toggle(Mode.EMOJI) }
        ), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = context.dp(6) })

        // 🎲 Generator: tap = generate + paste + Enter; long-press = settings.
        row.addView(tool("🎲 Generate",
            onClick = { proxy.commitAndEnter(Generator.generate(proxy.prefs.genLength)) },
            onLong = { openSettings() }
        ), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

        return row
    }

    private fun tool(label: String, onClick: () -> Unit, onLong: (() -> Unit)?): View {
        val b = Button(context).apply {
            this.text = label
            isAllCaps = false
            transformationMethod = null
            textSize = 12f
            minimumHeight = 0
            minHeight = 0
            val p = context.dp(10)
            setPadding(p, context.dp(8), p, context.dp(8))
            background = roundedDrawable(context, theme.keyBg, 999f) // pill
            setTextColor(theme.keyText)
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
            if (onLong != null) {
                setOnLongClickListener {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onLong()
                    true
                }
            }
        }
        return b
    }

    // ---------- Content switching ----------
    private fun toggle(target: Mode) {
        mode = if (mode == target) Mode.KEYS else target
        showMode(mode)
    }

    private fun showMode(m: Mode) {
        content.removeAllViews()
        when (m) {
            Mode.KEYS -> content.addView(keysView)
            Mode.PINNED -> {
                pinnedPanel.reload()
                content.addView(pinnedPanel)
            }
            Mode.EMOJI -> content.addView(emojiPanel)
        }
    }

    private fun openSettings() {
        val intent = Intent(context, SettingsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
