package com.pinboard.keyboard.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Local store for pinned messages using SharedPreferences + built-in org.json.
 * No Room / no SQL / no third-party libs => no "module not available" risk.
 * All read/write of the list is cheap; callers run it off the main thread.
 */
class PinStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("pinboard_pinned", Context.MODE_PRIVATE)

    fun getAll(): List<PinnedMessage> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = ArrayList<PinnedMessage>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list += PinnedMessage(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    text = o.optString("text", ""),
                    createdAt = o.optLong("createdAt", 0L),
                    updatedAt = o.optLong("updatedAt", 0L)
                )
            }
            // newest first
            list.sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(text: String) {
        val now = System.currentTimeMillis()
        val list = getAll().toMutableList()
        list.add(0, PinnedMessage(UUID.randomUUID().toString(), text, now, now))
        save(list)
    }

    fun update(id: String, text: String) {
        val now = System.currentTimeMillis()
        val updated = getAll().map {
            if (it.id == id) it.copy(text = text, updatedAt = now) else it
        }
        save(updated)
    }

    fun delete(id: String) {
        save(getAll().filterNot { it.id == id })
    }

    fun clearAll() {
        prefs.edit().remove(KEY).apply()
    }

    fun replaceAll(list: List<PinnedMessage>) {
        save(list)
    }

    private fun save(list: List<PinnedMessage>) {
        val arr = JSONArray()
        for (m in list) {
            val o = JSONObject()
            o.put("id", m.id)
            o.put("text", m.text)
            o.put("createdAt", m.createdAt)
            o.put("updatedAt", m.updatedAt)
            arr.put(o)
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object {
        private const val KEY = "pinned_json"
    }
}