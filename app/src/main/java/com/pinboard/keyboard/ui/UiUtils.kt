package com.pinboard.keyboard.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable

/** dp -> px. */
fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

/** A rounded-rectangle drawable of the given color (used for keys/cards). */
fun roundedDrawable(context: Context, color: Int, radiusDp: Float = 10f): Drawable {
    return GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = radiusDp * context.resources.displayMetrics.density
        setColor(color)
    }
}