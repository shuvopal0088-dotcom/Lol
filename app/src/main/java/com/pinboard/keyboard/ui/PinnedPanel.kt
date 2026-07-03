package com.pinboard.keyboard.ui

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.pinboard.keyboard.InputProxy
import com.pinboard.keyboard.data.KeyboardTheme
import com.pinboard.keyboard.data.PinnedMessage

/**
 * Pinned messages: search, add, edit, delete, one-tap insert.
 * Editing is done with an INLINE editor (not a dialog) because an IME's
 * service window cannot reliably host an AlertDialog without crashing.
 *
 * Note: the view properties are `lateinit var` (not uninitialized `val`)
 * because they are assigned inside helper functions, and Kotlin only allows
 * deferred `val` initialization directly inside an `init {}` block.
 */
class PinnedPanel(
    context: Context,
    private val proxy: InputProxy,
    private val theme: KeyboardTheme
) : LinearLayout(context) {

    private lateinit var searchBox: EditText
    private lateinit var listContainer: LinearLayout
    private lateinit var editorHost: LinearLayout
    private var all: List<PinnedMessage> = emptyList()

    init {
        orientation = VERTICAL

        editorHost = LinearLayout(context).apply {
            orientation = VERTICAL
            visibility = View.GONE
        }
        addView(editorHost)

        addView(buildHeader())

        val scroll = ScrollView(context)
        listContainer = LinearLayout(context).apply { orientation = VERTICAL }
        scroll.addView(listContainer)
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    /** Called when this panel becomes visible. */
    fun reload() {
        proxy.runAsync {
            val data = proxy.store.getAll()
            proxy.runOnUi {
                all = data
                applyFilter()
            }
        }
    }

    /** Long-pressing the 📌 toolbar button triggers this. */
    fun requestNew() = showEditor(null)

    private fun buildHeader(): View {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        searchBox = EditText(context).apply {
            hint = "Search pinned"
            setSingleLine(true)
            textSize = 14f
            background = roundedDrawable(context, theme.keyBg, 12f)
            val p = context.dp(10)
            setPadding(p, p / 2, p, p / 2)
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) = applyFilter()
                override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            })
        }
        row.addView(searchBox, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = context.dp(8)
        })
        val newBtn = TextView(context).apply {
            this.text = "+ New"
            setTextColor(theme.accentText)
            typeface = Typeface.DEFAULT_BOLD
            textSize = 13f
            gravity = Gravity.CENTER
            background = roundedDrawable(context, theme.accentBg, 12f)
            val p = context.dp(12)
            setPadding(p, context.dp(10), p, context.dp(10))
            setOnClickListener { showEditor(null) }
        }
        row.addView(newBtn, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        val wrap = LinearLayout(context).apply {
            orientation = VERTICAL
            val p = context.dp(4)
            setPadding(p, p, p, p)
            addView(row)
        }
        return wrap
    }

    private fun applyFilter() {
        val q = searchBox.text.toString().trim().lowercase()
        val filtered = if (q.isEmpty()) all else all.filter { it.text.lowercase().contains(q) }
        listContainer.removeAllViews()
        if (filtered.isEmpty()) {
            listContainer.addView(emptyState())
        } else {
            for (m in filtered) listContainer.addView(card(m))
        }
    }

    private fun emptyState(): View = TextView(context).apply {
        this.text = if (all.isEmpty()) "No pinned messages yet.\nTap “+ New” to add one."
        else "No matches found."
        gravity = Gravity.CENTER
        setTextColor(theme.mutedText)
        textSize = 13f
        val p = context.dp(24)
        setPadding(p, p, p, p)
    }

    private fun card(m: PinnedMessage): View {
        val card = LinearLayout(context).apply {
            orientation = VERTICAL
            background = roundedDrawable(context, theme.panelBg, 14f)
            val p = context.dp(12)
            setPadding(p, p, p, p)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = context.dp(8)
            }
        }
        val body = TextView(context).apply {
            this.text = m.text
            setTextColor(theme.keyText)
            textSize = 14f
            maxLines = 6
            setOnClickListener { proxy.commit(m.text) }
        }
        card.addView(body)

        val actions = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.END
        }
        actions.addView(smallTextButton("Edit") { showEditor(m) })
        actions.addView(smallTextButton("Delete") {
            proxy.runAsync {
                proxy.store.delete(m.id)
                reload()
            }
        })
        card.addView(actions)
        return card
    }

    private fun showEditor(existing: PinnedMessage?) {
        editorHost.removeAllViews()
        val padding = context.dp(10)

        val et = EditText(context).apply {
            hint = if (existing == null) "Type a message to pin…" else "Edit message"
            setText(existing?.text ?: "")
            setSingleLine(false)
            minLines = 3
            maxLines = 6
            textSize = 14f
            setTextColor(theme.keyText)
            setHintTextColor(theme.mutedText)
            background = roundedDrawable(context, theme.panelBg, 12f)
            setPadding(padding, padding, padding, padding)
        }

        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.END
            val m = context.dp(4)
            setPadding(m, m, m, m)
        }
        val cancel = smallTextButton("Cancel") { closeEditor() }
        val save = smallTextButton("Save") {
            val text = et.text.toString().trim()
            if (text.isNotEmpty()) {
                proxy.runAsync {
                    if (existing == null) proxy.store.add(text)
                    else proxy.store.update(existing.id, text)
                    reload()
                }
            }
            closeEditor()
        }
        row.addView(cancel)
        row.addView(save)

        editorHost.addView(et, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = context.dp(4)
        })
        editorHost.addView(row)
        editorHost.visibility = View.VISIBLE
        et.requestFocus()
    }

    private fun closeEditor() {
        editorHost.removeAllViews()
        editorHost.visibility = View.GONE
    }

    private fun smallTextButton(label: String, onClick: () -> Unit): View =
        TextView(context).apply {
            this.text = label
            setTextColor(theme.accentBg)
            typeface = Typeface.DEFAULT_BOLD
            textSize = 13f
            val p = context.dp(12)
            setPadding(p, context.dp(6), p, context.dp(6))
            setOnClickListener { onClick() }
        }
}
