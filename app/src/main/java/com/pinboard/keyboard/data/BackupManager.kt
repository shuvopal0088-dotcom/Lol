package com.pinboard.keyboard.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Exports/imports all app data (pinned + settings + slots) as a single JSON
 * blob, used by Backup / Restore in Settings.
 */
class BackupManager(context: Context) {

    private val app = context.applicationContext

    fun export(): String {
        val store = PinStore(app)
        val prefs = PrefsManager(app)

        val pinnedArr = JSONArray()
        for (m in store.getAll()) {
            val o = JSONObject()
            o.put("id", m.id)
            o.put("text", m.text)
            o.put("createdAt", m.createdAt)
            o.put("updatedAt", m.updatedAt)
            pinnedArr.put(o)
        }

        val root = JSONObject()
        root.put("app", "PinBoardKeyboard")
        root.put("version", 1)
        root.put("pinned", pinnedArr)
        root.put("genLength", prefs.genLength)
        root.put("darkMode", prefs.darkMode)
        for (id in 1..3) {
            val s = JSONObject()
            s.put("text", prefs.slotText(id))
            s.put("enter", prefs.slotEnter(id))
            root.put("slot$id", s)
        }
        return root.toString(2)
    }

    /** Returns true on success, false if the JSON is invalid. */
    fun import(json: String): Boolean {
        return try {
            val root = JSONObject(json)
            val prefs = PrefsManager(app)
            val store = PinStore(app)

            val arr = root.optJSONArray("pinned")
            if (arr != null) {
                val list = ArrayList<PinnedMessage>(arr.length())
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val now = System.currentTimeMillis()
                    list += PinnedMessage(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        text = o.optString("text", ""),
                        createdAt = o.optLong("createdAt", now),
                        updatedAt = o.optLong("updatedAt", now)
                    )
                }
                store.replaceAll(list)
            }

            if (root.has("genLength")) prefs.genLength = root.getInt("genLength")
            if (root.has("darkMode")) prefs.darkMode = root.getBoolean("darkMode")
            for (id in 1..3) {
                val s = root.optJSONObject("slot$id") ?: continue
                if (s.has("text")) prefs.setSlotText(id, s.getString("text"))
                if (s.has("enter")) prefs.setSlotEnter(id, s.getBoolean("enter"))
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}