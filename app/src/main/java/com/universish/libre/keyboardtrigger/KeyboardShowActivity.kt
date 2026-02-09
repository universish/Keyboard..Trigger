package com.universish.libre.keyboardtrigger

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.util.Log

class KeyboardShowActivity : Activity() {
    private lateinit var edit: EditText

    private var imeWasActiveBefore = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("KeyboardShowActivity", "onCreate")

        imeWasActiveBefore = intent?.getBooleanExtra("ime_active", false) ?: false
        Log.e("KeyboardShowActivity", "imeWasActiveBefore=$imeWasActiveBefore")

        edit = EditText(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            // Make invisible to user and hide caret to avoid dual-caret artifacts
            alpha = 0f
            isFocusable = true
            isFocusableInTouchMode = true
            isCursorVisible = false
            setTextIsSelectable(false)
            setTextColor(android.graphics.Color.TRANSPARENT)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            id = android.R.id.edit
        }
        setContentView(edit)

        // Ensure the window requests the IME
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        overridePendingTransition(0, 0)

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            if (imeWasActiveBefore) {
                Log.e("KeyboardShowActivity", "IME already active before; not taking focus. Finishing.")
                try { finish(); overridePendingTransition(0,0) } catch (_: Exception) {}
                return
            }
            try {
                edit.requestFocus()
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT)
                Log.e("KeyboardShowActivity", "showSoftInput called on window focus")

                // Start polling IME state and keep activity alive while IME is shown.
                val handler = Handler(Looper.getMainLooper())
                var seenActive = false
                val check = object : Runnable {
                    override fun run() {
                        try {
                            val active = try { imm.isAcceptingText() } catch (_: Exception) { false }
                            if (active) {
                                seenActive = true
                                // keep checking while active
                                handler.postDelayed(this, 300)
                            } else {
                                if (seenActive) {
                                    // IME closed after being active -> finish
                                    Log.e("KeyboardShowActivity", "IME closed after being active; finishing")
                                    try { finish(); overridePendingTransition(0,0) } catch (_: Exception) {}
                                } else {
                                    // not active yet; keep checking for a short time
                                    handler.postDelayed(this, 300)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("KeyboardShowActivity", "IME poll error: ${e.message}")
                        }
                    }
                }
                handler.postDelayed(check, 250)

            } catch (e: Exception) {
                Log.e("KeyboardShowActivity", "showSoftInput error: ${e.message}")
                // As fallback, finish after a safe timeout to avoid leak
                Handler(Looper.getMainLooper()).postDelayed({ try { finish(); overridePendingTransition(0,0) } catch (_: Exception) {} }, 2000)
            }
        }
    }
}
