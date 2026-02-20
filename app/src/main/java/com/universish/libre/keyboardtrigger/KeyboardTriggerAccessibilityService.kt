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

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import java.io.File
import java.io.FileWriter

class KeyboardTriggerAccessibilityService : AccessibilityService() {

    // Timestamp to debounce repeated selection-change events (ms)
    private var lastSelectionBubbleTs: Long = 0L

    // Append lightweight telemetry like FloatingService
    private fun appendTelemetry(msg: String) {
        try {
            val ts = System.currentTimeMillis()
            val f = File("/storage/emulated/0/Download/MyProjects/TELEMETRY.txt")
            val w = FileWriter(f, true)
            w.append("$ts | $msg\n")
            w.flush()
            w.close()
        } catch (_: Exception) {
            // ignore
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        android.util.Log.e("AccessibilityService", "onServiceConnected")
        val info = AccessibilityServiceInfo()
        // Include selection changed so we can optionally show selection bubble
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_FOCUSED or AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        // Allow retrieving windows so we can find editable nodes and restore focus
        info.flags = AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        serviceInfo = info

        // Register a receiver so other components (FloatingService) can ask us to trigger keyboard
        val filter = android.content.IntentFilter("com.universish.libre.keyboardtrigger.ACTION_TRIGGER_KEYBOARD")
        try {
            // Use not-exported receiver to satisfy newer platform security checks
            registerReceiver(triggerReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } catch (e: Exception) {
            android.util.Log.e("AccessibilityService", "registerReceiver failed: ${'$'}{e.message}")
        }
    }

    private val triggerReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            android.util.Log.e("AccessibilityService", "Received ACTION_TRIGGER_KEYBOARD broadcast")

            // Notify FloatingService that we're starting to handle the trigger (handshake)
            try {
                val started = Intent("com.universish.libre.keyboardtrigger.ACTION_TRIGGER_STARTED").apply { setPackage(packageName) }
                sendBroadcast(started)
            } catch (_: Exception) {}

            // Schedule a 'finished' notifier after the verification window so FloatingService waits safely
            try {
                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                handler.postDelayed({
                    try {
                        val fin = Intent("com.universish.libre.keyboardtrigger.ACTION_TRIGGER_FINISHED").apply { setPackage(packageName) }
                        sendBroadcast(fin)
                    } catch (_: Exception) {}
                }, 1400)
            } catch (_: Exception) {}

            try {
                // Try to find a focused editable node and give it focus/click so IME will attach
                val root = rootInActiveWindow
                var handled = false
                if (root != null) {
                    val queue = java.util.ArrayDeque<android.view.accessibility.AccessibilityNodeInfo>()
                    queue.add(root)
                    while (queue.isNotEmpty()) {
                        val node = queue.removeFirst()
                        if (node.isEditable) {
                            android.util.Log.e("AccessibilityService", "Found editable node; attempting focus/click")
                            // only disturb the view if it isn't already focused – clicks can collapse
                            // selection or move the cursor, so prefer ACTION_FOCUS alone when possible.
                            try {
                                if (!node.isFocused) {
                                    node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_FOCUS)
                                }
                            } catch (_: Exception) {}
                            // always attempt a click; some fields need an explicit click even when already focused
                            try {
                                node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
                            } catch (_: Exception) {}
                            // After focusing/clicking, verify IME actually attached before marking handled.
                            try {
                                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

                                // If IME already accepting text *and* the node is focused, mark handled immediately
                                try {
                                    node.refresh()
                                } catch (_: Exception) {}

                                // regardless of IME state, treat focus/click attempt as handled and inform FloatingService
                                val prefs = getSharedPreferences("keyboard_trigger_prefs", MODE_PRIVATE)
                                val nowTs = System.currentTimeMillis()
                                prefs.edit().putLong("last_handled_trigger_ts", nowTs).apply()
                                try {
                                    val ack = Intent("com.universish.libre.keyboardtrigger.ACTION_TRIGGER_HANDLED")
                                    sendBroadcast(ack)
                                    android.util.Log.e("AccessibilityService", "ACK sent after focusing/clicking node")
                                } catch (_: Exception) {}
                                // still enter verification block to gather telemetry and for future robustness
                                if (try { imm.isAcceptingText() } catch (_: Exception) { false } && node.isFocused) {
                                    // already handled
                                } else {
                                    // Retry verification window: check (node.focused && IME active) several times before giving up.
                                    val handler = android.os.Handler(android.os.Looper.getMainLooper())
                                    val attempts = intArrayOf(100, 300, 600) // ms offsets
                                    var attemptIndex = 0
                                    val verifyRunnable = object : Runnable {
                                        override fun run() {
                                            try {
                                                var focused = false
                                                try { node.refresh(); focused = node.isFocused } catch (_: Exception) {}
                                                val imeActive = try { imm.isAcceptingText() } catch (_: Exception) { false }
                                                // record attempt to telemetry as well as logcat
                                                appendTelemetry("verifyAttempt idx=$attemptIndex focused=$focused imeActive=$imeActive")
                                                android.util.Log.e("AccessibilityService", "verifyAttempt idx=$attemptIndex focused=$focused imeActive=$imeActive")
                                                if (focused && imeActive) {
                                                    val prefs = getSharedPreferences("keyboard_trigger_prefs", MODE_PRIVATE)
                                                    val nowTs = System.currentTimeMillis()
                                                    prefs.edit().putLong("last_handled_trigger_ts", nowTs).apply()
                                                    try {
                                                        val ack = Intent("com.universish.libre.keyboardtrigger.ACTION_TRIGGER_HANDLED")
                                                        sendBroadcast(ack)
                                                        android.util.Log.e("AccessibilityService", "ACK sent after verifyAttempt idx=$attemptIndex")
                                                    } catch (_: Exception) {}
                                                    return
                                                }
                                                attemptIndex++
                                                if (attemptIndex < attempts.size) {
                                                    handler.postDelayed(this, (attempts[attemptIndex] - attempts[attemptIndex - 1]).toLong())
                                                }
                                            } catch (_: Exception) {}
                                        }
                                    }
                                    handler.postDelayed(verifyRunnable, attempts[0].toLong())
                                }
                            } catch (_: Exception) {
                                // If we cannot query IME, fall back to the old behavior (mark handled) to preserve UX
                                try {
                                    val prefs = getSharedPreferences("keyboard_trigger_prefs", MODE_PRIVATE)
                                    val nowTs = System.currentTimeMillis()
                                    prefs.edit().putLong("last_handled_trigger_ts", nowTs).apply()
                                    try {
                                        val ack = Intent("com.universish.libre.keyboardtrigger.ACTION_TRIGGER_HANDLED").apply { setPackage(packageName) }
                                        sendBroadcast(ack)
                                    } catch (_: Exception) {}
                                } catch (_: Exception) {}
                            }

                            handled = true
                            break
                        }
                        for (i in 0 until node.childCount) {
                            val c = node.getChild(i)
                            if (c != null) queue.add(c)
                        }
                    }
                }
                if (!handled) {
                    // No editable node found — use the safe Activity fallback (KeyboardShowActivity) instead of overlay
                    android.util.Log.e("AccessibilityService", "No editable node; starting KeyboardShowActivity as safe fallback")
                    try {
                        val act = Intent(this@KeyboardTriggerAccessibilityService, KeyboardShowActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        startActivity(act)
                    } catch (e: Exception) {
                        android.util.Log.e("AccessibilityService", "Failed to start KeyboardShowActivity: ${'$'}{e.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AccessibilityService", "triggerReceiver error: ${'$'}{e.message}", e)
            }
        }
    }

    override fun onDestroy() {
        try { unregisterReceiver(triggerReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event == null) return
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
                // Only proceed when selection-bubble preference is enabled
                val prefs = getSharedPreferences("keyboard_trigger_prefs", MODE_PRIVATE)
                val selectionEnabled = prefs.getBoolean("selection_bubble_enabled", false)
                if (!selectionEnabled) return

                val source = event.source ?: return

                // Ignore system UI and other suspicious packages to avoid false positives
                val pkg = event.packageName?.toString() ?: ""
                val launcherIgnore = setOf("de.mm20.launcher2.release", "com.android.launcher3", "com.google.android.apps.nexuslauncher", "com.oplus.launcher")
                if (pkg.startsWith("com.android.systemui") || pkg == "android" || launcherIgnore.contains(pkg)) {
                    android.util.Log.e("AccessibilityService", "Selection changed ignored: package=$pkg")
                    return
                }

                // Ignore non-editable sources to avoid false positives from system UI
                try {
                    if (!source.isEditable) {
                        android.util.Log.e("AccessibilityService", "Selection changed ignored: source not editable")
                        return
                    }
                } catch (_: Exception) {
                    // fallthrough if isEditable check fails for some node
                }

                // Require class name to indicate a real text editor (avoid false positives)
                val className = source.className?.toString() ?: ""
                if (!className.contains("EditText", ignoreCase = true) && !className.contains("TextView", ignoreCase = true) && !className.contains("Editor", ignoreCase = true)) {
                    android.util.Log.e("AccessibilityService", "Selection changed ignored: className=$className")
                    return
                }

                // Ignore very small/empty bounds (likely not a real text field)
                val r = Rect()
                source.getBoundsInScreen(r)
                val width = r.right - r.left
                val height = r.bottom - r.top
                if (width < 16 || height < 8) {
                    android.util.Log.e("AccessibilityService", "Selection changed ignored: small bounds ${'$'}width x ${'$'}height")
                    return
                }

                // Debounce repeated selection events to avoid spamming
                val now = System.currentTimeMillis()
                if (now - lastSelectionBubbleTs < 500) {
                    android.util.Log.e("AccessibilityService", "Selection changed ignored: debounce")
                    return
                }
                lastSelectionBubbleTs = now

                val cx = (r.left + r.right) / 2
                val cy = (r.top + r.bottom) / 2
                android.util.Log.e("AccessibilityService", "Selection changed at $cx,$cy - requesting bubble (pkg=$pkg, class=$className)")
                val b = Intent("com.universish.libre.keyboardtrigger.ACTION_SHOW_SELECTION_BUBBLE").apply {
                    putExtra("x", cx)
                    putExtra("y", cy)
                    putExtra("pkg", pkg)
                    putExtra("class", className)
                    setPackage(packageName)
                }
                sendBroadcast(b)
            }
        } catch (e: Exception) {
            android.util.Log.e("AccessibilityService", "onAccessibilityEvent error: ${'$'}{e.message}")
        }
    }

    override fun onInterrupt() {
        // Handle interruption
    }

    companion object {
        fun triggerKeyboard(context: android.content.Context) {
            try {
                // Safe fallback: launch KeyboardShowActivity which requests IME in an Activity context
                android.util.Log.e("AccessibilityService", "triggerKeyboard: starting KeyboardShowActivity as safe fallback")
                val act = Intent(context, KeyboardShowActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(act)
            } catch (e: Exception) {
                android.util.Log.e("AccessibilityService", "triggerKeyboard error: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
