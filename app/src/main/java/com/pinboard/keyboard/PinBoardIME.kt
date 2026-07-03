package com.pinboard.keyboard

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.pinboard.keyboard.data.PrefsManager
import com.pinboard.keyboard.data.PinStore
import com.pinboard.keyboard.ui.KeyboardView
import java.util.concurrent.Executors

/**
 * The actual Input Method Service. Android launches this whenever the user
 * focuses a text field while PinBoard is the active keyboard.
 *
 * Crash-safety notes (your point #5):
 *  - onCreateInputView() is tiny and never returns null.
 *  - Every InputConnection call is null-checked.
 *  - No DB/heavy work happens on the main thread (uses a single-thread Executor).
 */
class PinBoardIME : InputMethodService(), InputProxy {

    private lateinit var store: PinStore
    private lateinit var prefs: PrefsManager
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        prefs = PrefsManager(applicationContext)
        store = PinStore(applicationContext)
    }

    override fun onCreateInputView(): View {
        // Lightweight: just inflate the keyboard view. Reads run lazily/async.
        return KeyboardView(this, this)
    }

    override val store: PinStore get() = this.store
    override val prefs: PrefsManager get() = this.prefs

    override fun commit(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    override fun commitAndEnter(text: String) {
        val ic: InputConnection = currentInputConnection ?: return
        ic.commitText(text, 1)
        ic.finishComposingText()
        performEnter(ic)
    }

    override fun deleteChar() {
        val ic = currentInputConnection ?: return
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
    }

    override fun enter() {
        val ic = currentInputConnection ?: return
        performEnter(ic)
    }

    override fun clear() {
        val ic = currentInputConnection ?: return
        ic.performContextMenuAction(android.R.id.selectAll)
        ic.commitText("", 1)
    }

    override fun closeKeyboard() {
        requestHideSelf(0)
    }

    override fun runAsync(action: () -> Unit) {
        io.execute { action() }
    }

    override fun runOnUi(action: () -> Unit) {
        main.post { action() }
    }

    /** Performs the field's IME action (send/go/done/next) when present,
     *  otherwise inserts a newline. This is what makes "paste + Enter" send
     *  a message in chat apps. */
    private fun performEnter(ic: InputConnection) {
        val info = currentInputEditorInfo
        val action = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
            ?: EditorInfo.IME_ACTION_UNSPECIFIED
        when (action) {
            EditorInfo.IME_ACTION_DONE,
            EditorInfo.IME_ACTION_GO,
            EditorInfo.IME_ACTION_NEXT,
            EditorInfo.IME_ACTION_PREVIOUS,
            EditorInfo.IME_ACTION_SEND,
            EditorInfo.IME_ACTION_SEARCH -> ic.performEditorAction(action)
            else -> ic.commitText("\n", 1)
        }
    }
}