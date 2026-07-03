package com.pinboard.keyboard.ui

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.TextView
import com.pinboard.keyboard.InputProxy
import com.pinboard.keyboard.data.KeyboardTheme

/** Grid of ~55 popular emojis. One tap inserts. Offline only. */
class EmojiPanel(
    context: Context,
    private val proxy: InputProxy,
    private val theme: KeyboardTheme
) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        val grid = GridView(context).apply {
            numColumns = 8
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            verticalSpacing = context.dp(2)
            horizontalSpacing = context.dp(2)
            adapter = EmojiAdapter(context, EMOJIS) { emoji -> proxy.commit(emoji) }
        }
        addView(grid, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    private class EmojiAdapter(
        private val ctx: Context,
        private val items: List<String>,
        private val onClick: (String) -> Unit
    ) : BaseAdapter() {
        override fun getCount(): Int = items.size
        override fun getItem(position: Int): String = items[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val tv = (convertView as? TextView) ?: TextView(ctx).apply {
                gravity = Gravity.CENTER
                textSize = 22f
            }
            tv.text = items[position]
            tv.setOnClickListener { onClick(items[position]) }
            return tv
        }
    }

    companion object {
        val EMOJIS = listOf(
            "😀", "😁", "😂", "🤣", "😊", "😍", "😘", "😎", "🤩", "🙂",
            "😉", "😋", "😴", "🤔", "🤗", "🤭", "😅", "😭", "😢", "😡",
            "👍", "👎", "👏", "🙏", "💪", "🙌", "👋", "✌️", "🤞", "👀",
            "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "💔", "💯", "🔥",
            "✨", "⭐", "🎉", "🎊", "🎁", "✅", "❌", "⚠️", "❓", "❗",
            "📍", "📅", "⏰", "💰", "🚀"
        )
    }
}