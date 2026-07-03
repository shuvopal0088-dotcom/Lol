package com.pinboard.keyboard.settings

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.pinboard.keyboard.R
import com.pinboard.keyboard.data.BackupManager
import com.pinboard.keyboard.data.PinStore
import com.pinboard.keyboard.data.PrefsManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: PrefsManager

    private val rcCreateDoc = 1001
    private val rcOpenDoc = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = PrefsManager(this)
        // Apply the in-app theme before creating the view.
        AppCompatDelegate.setDefaultNightMode(
            if (prefs.darkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)
        // Opt out of edge-to-edge so content never hides under system bars (API 36).
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_settings)

        findViewById<MaterialToolbar>(R.id.toolbar).apply {
            setNavigationIcon(android.R.drawable.ic_menu_close_clear_cancel)
            setNavigationOnClickListener { finish() }
        }

        setupAppearance()
        setupGenerator()
        setupSlots()
        setupData()
        setupKeyboard()
    }

    private fun setupAppearance() {
        val sw = findViewById<MaterialSwitch>(R.id.switch_dark)
        sw.isChecked = prefs.darkMode
        sw.setOnCheckedChangeListener { _, checked ->
            prefs.darkMode = checked
            AppCompatDelegate.setDefaultNightMode(
                if (checked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    private fun setupGenerator() {
        val seek = findViewById<SeekBar>(R.id.seek_length)
        val text = findViewById<android.widget.TextView>(R.id.text_length)
        seek.max = 14 // 0..14 -> 6..20
        seek.progress = prefs.genLength - 6
        text.text = prefs.genLength.toString()
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val len = progress + 6
                text.text = len.toString()
                prefs.genLength = len
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    private fun setupSlots() {
        for (id in 1..3) {
            val textId = resources.getIdentifier("slot${id}_text", "id", packageName)
            val enterId = resources.getIdentifier("slot${id}_enter", "id", packageName)
            val et = findViewById<TextInputEditText>(textId)
            val sw = findViewById<MaterialSwitch>(enterId)
            et.setText(prefs.slotText(id))
            sw.isChecked = prefs.slotEnter(id)
            sw.setOnCheckedChangeListener { _, checked -> prefs.setSlotEnter(id, checked) }
        }
    }

    private fun setupData() {
        findViewById<android.view.View>(R.id.btn_backup).setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "pinboard-backup.json")
            }
            startActivityForResult(intent, rcCreateDoc)
        }

        findViewById<android.view.View>(R.id.btn_restore).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
            }
            startActivityForResult(intent, rcOpenDoc)
        }

        findViewById<android.view.View>(R.id.btn_clear).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear all pinned")
                .setMessage("Delete ALL pinned messages? This cannot be undone.")
                .setPositiveButton("Clear") { _, _ ->
                    PinStore(this).clearAll()
                    toast("All pinned messages cleared")
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun setupKeyboard() {
        findViewById<android.view.View>(R.id.btn_enable).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        findViewById<android.view.View>(R.id.btn_select).setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }
    }

    override fun onPause() {
        super.onPause()
        // Persist any typed quick-paste text (covers the night-mode recreate case).
        for (id in 1..3) {
            val textId = resources.getIdentifier("slot${id}_text", "id", packageName)
            findViewById<TextInputEditText?>(textId)?.text?.toString()
                ?.let { prefs.setSlotText(id, it) }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        when (requestCode) {
            rcCreateDoc -> {
                val json = BackupManager(this).export()
                contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                toast("Backup saved")
            }
            rcOpenDoc -> {
                val json = contentResolver.openInputStream(uri)?.use {
                    it.readBytes().toString(Charsets.UTF_8)
                }
                if (json != null && BackupManager(this).import(json)) toast("Backup restored")
                else toast("Invalid backup file")
            }
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}