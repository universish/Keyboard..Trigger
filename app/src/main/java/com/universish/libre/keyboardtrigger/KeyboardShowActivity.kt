/*
    Program Name: Keyboard Trigger
    Program Package Name or Application ID: com.universish.libre.keyboardtrigger
    About and my Idea: The dynamic button that activates the keyboard is a button that can be placed on the edges of the screen. Pressing the button brings up the keyboard. The application is privacy-focused.  There is no tracking or advertising. It does not require internet access. It does not interfere with text editing and does not break focus. It is a FLOSS Android application.
    Copyright © 2026 Saffet Yavuz https://github.com/universish  
    (Developer's username: universish)

	This program comes with ABSOLUTELY NO WARRANTY; for details type
	`NO WARRANTY. BECAUSE THE PROGRAM IS LICENSED FREE OF CHARGE, THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.`

    This is free software, and you are welcome to redistribute it under certain conditions; type 
	`Keyboard..Trigger > Copyright (C) 2026 universish
	This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, and/or any later version.
	This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.' for details.
	You should have received a copy of the GNU General Public License along with this program. If not, see https://www.gnu.org/licenses/gpl-3.0.tr.html .`

*/

// Application creator and developer: universish (Saffet Yavuz)
// License: GPLv3 (see LICENSE in repo)
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

        // If AccessibilityService already handled this trigger very recently, don't steal IME
        try {
            val prefs = getSharedPreferences("keyboard_trigger_prefs", MODE_PRIVATE)
            val handledTs = prefs.getLong("last_handled_trigger_ts", 0L)
            if (handledTs != 0L && System.currentTimeMillis() - handledTs < 1500) {
                Log.e("KeyboardShowActivity", "Recent handledTs detected -> finishing without taking focus (handledTs=${'$'}handledTs)")
                finish(); overridePendingTransition(0,0)
                return
            }
        } catch (_: Exception) {}

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

        // prevent this invisible activity from appearing in screenshots/recents
        window.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE,
                        android.view.WindowManager.LayoutParams.FLAG_SECURE)
        // don't consume touches - let the user interact with whatever is underneath
        window.setFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

        // Ensure the window requests the IME
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        overridePendingTransition(0, 0)

        // Listen for an ACK from AccessibilityService — if received, relinquish IME immediately
        try {
            registerReceiver(object : android.content.BroadcastReceiver() {
                override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                    if (intent?.action == "com.universish.libre.keyboardtrigger.ACTION_TRIGGER_HANDLED") {
                        Log.e("KeyboardShowActivity", "Received ACTION_TRIGGER_HANDLED -> finishing to avoid IME steal")
                        try { finish(); overridePendingTransition(0,0) } catch (_: Exception) {}
                    }
                }
            }, android.content.IntentFilter("com.universish.libre.keyboardtrigger.ACTION_TRIGGER_HANDLED"))
        } catch (_: Exception) {}

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
