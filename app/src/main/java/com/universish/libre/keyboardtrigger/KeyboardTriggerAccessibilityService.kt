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

class KeyboardTriggerAccessibilityService : AccessibilityService() {

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
                            android.util.Log.e("AccessibilityService", "Found editable node; attempting focus and click")
                            node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_FOCUS)
                            node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
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
                    // No editable node found; ask FloatingService to show an overlay EditText instead of starting an Activity.
                    android.util.Log.e("AccessibilityService", "No editable node; requesting overlay fallback from FloatingService")
                    try {
                        val b = android.content.Intent("com.universish.libre.keyboardtrigger.ACTION_REQUEST_OVERLAY").apply { setPackage(packageName) }
                        sendBroadcast(b)
                    } catch (e: Exception) {
                        android.util.Log.e("AccessibilityService", "Failed to send overlay request: ${'$'}{e.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AccessibilityService", "triggerReceiver error: ${'$'}{e.message}")
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
                // Check preference: only act if selection bubble enabled
                val prefs = getSharedPreferences("keyboard_trigger_prefs", MODE_PRIVATE)
                val selectionEnabled = prefs.getBoolean("selection_bubble_enabled", false)
                if (!selectionEnabled) return

                val source = event.source
                if (source != null) {
                    val r = Rect()
                    source.getBoundsInScreen(r)
                    val cx = (r.left + r.right) / 2
                    val cy = (r.top + r.bottom) / 2
                    android.util.Log.e("AccessibilityService", "Selection changed at $cx,$cy - requesting bubble")
                    val b = Intent("com.universish.libre.keyboardtrigger.ACTION_SHOW_SELECTION_BUBBLE").apply {
                        putExtra("x", cx)
                        putExtra("y", cy)
                        setPackage(packageName)
                    }
                    sendBroadcast(b)
                }
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
                // Request FloatingService overlay as fallback rather than launching an Activity
                android.util.Log.e("AccessibilityService", "triggerKeyboard: sending overlay request to FloatingService")
                val intent = android.content.Intent("com.universish.libre.keyboardtrigger.ACTION_REQUEST_OVERLAY").apply { setPackage(context.packageName) }
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                android.util.Log.e("AccessibilityService", "triggerKeyboard error: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
