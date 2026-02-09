package com.universish.libre.keyboardtrigger

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager

class KeyboardTriggerAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        android.util.Log.e("AccessibilityService", "onServiceConnected")
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_FOCUSED or AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
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
        // We don't need to react to events, just be active to trigger keyboard
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
