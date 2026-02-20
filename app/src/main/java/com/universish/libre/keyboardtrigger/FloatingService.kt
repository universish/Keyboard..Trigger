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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.net.Uri
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.math.abs

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingButton: TextView
    private lateinit var params: WindowManager.LayoutParams
    private var screenWidth = 0
    private var floatingButtonAdded = false

    private val handler = Handler(Looper.getMainLooper())
    private var overlayView: EditText? = null
    private var overlayPollingRunnable: Runnable? = null
    private var lastUserTriggerTs: Long = 0L

    private var selectionBubbleView: TextView? = null
    private var selectionBubbleParams: WindowManager.LayoutParams? = null
    private var permissionCheckRunnable: Runnable? = null

    // ACK state for the current trigger; persists across fallback attempts
    private val ackHandled = java.util.concurrent.atomic.AtomicBoolean(false)

    // persistent receiver listens for ACTION_TRIGGER_HANDLED broadcasts
    private val ackReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                ackHandled.set(true)
                android.util.Log.e("FloatingService", "Received ACTION_TRIGGER_HANDLED — will skip fallback")
            } catch (_: Exception) {}
        }
    }

    // Broadcast receiver to accept explicit overlay requests (from AccessibilityService fallback)
    private val overlayRequestReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.e("FloatingService", "Received overlay request from AccessibilityService")
            try {
                val now = System.currentTimeMillis()
                val allowedWindowMs = 3000L
                if (now - lastUserTriggerTs > allowedWindowMs) {
                    Log.e("FloatingService", "Ignoring overlay request: no recent user trigger (delta=${'$'}{now - lastUserTriggerTs}ms)")
                    return
                }

                // Overlay fallback removed for safety — start KeyboardShowActivity instead
                try {
                    val act = Intent(this@FloatingService, KeyboardShowActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    startActivity(act)
                    android.util.Log.e("FloatingService", "ACTION_REQUEST_OVERLAY received -> started KeyboardShowActivity (overlay removed)")
                } catch (e: Exception) {
                    hatayiDosyayaYaz(e)
                }
            } catch (e: Exception) {
                hatayiDosyayaYaz(e)
            }
        }
    }

    // Broadcast receiver to accept selection bubble requests
    private val selectionBubbleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                if (intent?.action == "com.universish.libre.keyboardtrigger.ACTION_SHOW_SELECTION_BUBBLE") {
                    val x = intent.getIntExtra("x", 0)
                    val y = intent.getIntExtra("y", 0)
                    val pkg = intent.getStringExtra("pkg") ?: ""
                    val launcherIgnore = setOf("de.mm20.launcher2.release", "com.android.launcher3", "com.google.android.apps.nexuslauncher", "com.oplus.launcher")
                    if (pkg.isNotEmpty() && launcherIgnore.contains(pkg)) {
                        Log.e("FloatingService", "Ignoring selection bubble request from launcher package: $pkg")
                        return
                    }
                    Log.e("FloatingService", "Show selection bubble at $x,$y (pkg=$pkg)")
                    showSelectionBubble(x, y)
                }
            } catch (e: Exception) {
                hatayiDosyayaYaz(e)
            }
        }
    }

    // Emergency broadcast receiver (panic) — removes overlays and stops service
    private val emergencyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.universish.libre.keyboardtrigger.ACTION_EMERGENCY_DISABLE") {
                try {
                    Log.e("FloatingService", "Emergency disable received — removing overlays and stopping service")
                    removeOverlayFallback()
                    removeSelectionBubble()
                    stopSelf()
                } catch (e: Exception) {
                    hatayiDosyayaYaz(e)
                }
            }
        }
    }

    // --- HATA RAPORLAMA FONKSİYONU ---
    private fun hatayiDosyayaYaz(e: Throwable) {
        try {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            val hataMesaji = sw.toString()

            // Dosya: Download/MyProjects/HATA_RAPORU.txt
            val dosyaYolu = File("/storage/emulated/0/Download/MyProjects/HATA_RAPORU.txt")
            val writer = FileWriter(dosyaYolu, true) // true = sona ekle
            writer.append("\n--- YENİ HATA ---\n")
            writer.append(hataMesaji)
            writer.append("\n------------------\n")
            writer.flush()
            writer.close()
        } catch (ex: Exception) {
            // Hata yazarken hata çıkarsa yapacak bir şey yok
        }
    }

    // Append lightweight telemetry (non-sensitive) to a file for post-mortem analysis
    private fun appendTelemetry(msg: String) {
        try {
            val ts = System.currentTimeMillis()
            val f = File("/storage/emulated/0/Download/MyProjects/TELEMETRY.txt")
            val w = FileWriter(f, true)
            w.append("$ts | $msg\n")
            w.flush()
            w.close()
        } catch (_: Exception) {
            // ignore telemetry write failures
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.e("FloatingService", "onCreate called")

        // Create notification channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "keyboard_trigger_channel",
                "Keyboard Trigger Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // Build a notification that can be used immediately so we call startForeground
        val notificationIntent = Intent().apply {
            setClassName(this@FloatingService, "com.universish.libre.keyboardtrigger.MainActivity")
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )
        val initialNotif = NotificationCompat.Builder(this, "keyboard_trigger_channel")
            .setContentTitle("Keyboard Trigger")
            .setContentText("Service starting...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .build()
        try {
            startForeground(1, initialNotif)
        } catch (e: Exception) {
            hatayiDosyayaYaz(e)
        }

        // GLOBAL HATA YAKALAYICI
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            hatayiDosyayaYaz(e)
            stopSelf() // Servisi öldür
        }

        try {
            // Register receiver for overlay request (AccessibilityService fallback)
            val filter = IntentFilter("com.universish.libre.keyboardtrigger.ACTION_REQUEST_OVERLAY")
            // Use two-arg overload for broad compatibility
            registerReceiver(overlayRequestReceiver, filter)

            // Register receiver for selection bubble requests
            val selFilter = IntentFilter("com.universish.libre.keyboardtrigger.ACTION_SHOW_SELECTION_BUBBLE")
            registerReceiver(selectionBubbleReceiver, selFilter)

            // Register emergency/panic receiver so we can remotely shut down overlays if needed
            val emFilter = IntentFilter("com.universish.libre.keyboardtrigger.ACTION_EMERGENCY_DISABLE")
            registerReceiver(emergencyReceiver, emFilter)

            // Register persistent ACK receiver
            try {
                registerReceiver(ackReceiver, IntentFilter("com.universish.libre.keyboardtrigger.ACTION_TRIGGER_HANDLED"))
            } catch (_: Exception) {
                // ignore
            }

            startPermissionMonitoring()
            // Defer heavy UI work to onStartCommand to ensure startForeground is called first
            // baslat() will be posted from onStartCommand
        } catch (e: Exception) {
            hatayiDosyayaYaz(e)
            Toast.makeText(this, "HATA OLUŞTU! Dosyaya yazıldı.", Toast.LENGTH_LONG).show()
        }
    }

    private fun baslat() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        screenWidth = Resources.getSystem().displayMetrics.widthPixels

        // Prevent duplicate floating buttons being added if baslat() is called multiple times
        try {
            if (::floatingButton.isInitialized) {
                try {
                    if (floatingButton.parent != null) {
                        Log.e("FloatingService", "Floating button already present; skipping baslat()")
                        return
                    } else {
                        // If the previous instance exists but is not attached, try to clean it up
                        try { windowManager.removeViewImmediate(floatingButton) } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        floatingButton = TextView(this).apply {
            text = "⬆"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            
            val shape = GradientDrawable()
            shape.shape = GradientDrawable.RECTANGLE
            shape.cornerRadius = 15f
            // choose a calm blue background
            shape.setColor(Color.parseColor("#FF2196F3"))
            shape.setStroke(2, Color.parseColor("#1976D2"))
            background = shape
            // arrow will be white
            setTextColor(Color.WHITE)
        }

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // slightly wider, taller button
        val btnWidth = 80
        val btnHeight = 210 // 1.5x original
        params = WindowManager.LayoutParams(
            btnWidth,
            btnHeight,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_SECURE, // don't include this overlay in screenshots/recents
            PixelFormat.TRANSLUCENT
        )

        // position at left-center of screen
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
        params.gravity = Gravity.START or Gravity.TOP
        params.x = 0
        params.y = screenHeight/2 - btnHeight/2
        var lastClick = 0L
        floatingButton.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastClick < 300) return@setOnClickListener // debounce
            lastClick = now
            Log.e("FloatingService", "Button clicked")

            // QUICK WIN: if IME is already shown ignore the trigger entirely to avoid
            // launching fallback/activity or stealing focus from a working session.
            try {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                if (try { imm.isAcceptingText() } catch (_: Exception) { false }) {
                    Log.e("FloatingService", "Trigger ignored: IME already active")
                    return@setOnClickListener
                }
            } catch (_: Exception) {
                // ignore failure and continue
            }

            // Respect persisted pref: if user disabled service, ignore
            try {
                val prefs = getSharedPreferences("keyboard_trigger_prefs", MODE_PRIVATE)
                if (!prefs.getBoolean("service_enabled", true)) {
                    Log.e("FloatingService", "Trigger ignored: service_enabled=false")
                    return@setOnClickListener
                }
            } catch (_: Exception) {}

            try {
                // remember user trigger timestamp (used to authorize overlay fallback requests)
                lastUserTriggerTs = System.currentTimeMillis()

                // 1) Ask AccessibilityService to focus any editable node
                val intent = Intent("com.universish.libre.keyboardtrigger.ACTION_TRIGGER_KEYBOARD").apply { setPackage(packageName) }
                sendBroadcast(intent)

                // 2) Prepare an ACK flag; the receiver itself is registered globally so it survives fallbacks
                val ackHandled = java.util.concurrent.atomic.AtomicBoolean(false)
                val triggerInProgress = java.util.concurrent.atomic.AtomicBoolean(false)
                val fallbackAttemptCount = java.util.concurrent.atomic.AtomicInteger(0)

                // ackReceiver registration moved to onCreate

                // observe AccessibilityService STARTED/FINISHED handshake and retry-aware fallback
                val startedReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        try { triggerInProgress.set(true); appendTelemetry("trigger_started") } catch (_: Exception) {}
                    }
                }
                val finishedReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        try { triggerInProgress.set(false); appendTelemetry("trigger_finished") } catch (_: Exception) {}
                    }
                }
                try {
                    registerReceiver(startedReceiver, IntentFilter("com.universish.libre.keyboardtrigger.ACTION_TRIGGER_STARTED"))
                    registerReceiver(finishedReceiver, IntentFilter("com.universish.libre.keyboardtrigger.ACTION_TRIGGER_FINISHED"))
                } catch (_: Exception) {}

                // wait with retries for the ACK / IME to become active; do several checks before starting Activity fallback
                val attemptIntervals = longArrayOf(800L, 300L, 300L) // ms
                var attemptIndex = 0
                val fallbackChecker = object : Runnable {
                    override fun run() {
                        try {
                            val prefs = getSharedPreferences("keyboard_trigger_prefs", MODE_PRIVATE)

                            // If ACK already received, skip
                            if (ackHandled.get()) {
                                android.util.Log.e("FloatingService", "ACK received — skipping fallback (attempt=${'$'}attemptIndex)")
                                appendTelemetry("fallback: ack received, skip (attempt=$attemptIndex)")
                                return
                            }

                            // If AccessibilityService is still working on the same trigger, postpone fallback a few times
                            if (triggerInProgress.get() && fallbackAttemptCount.get() < 3) {
                                fallbackAttemptCount.incrementAndGet()
                                android.util.Log.e("FloatingService", "Postponing fallback because triggerInProgress (attempt=${'$'}{fallbackAttemptCount.get()})")
                                appendTelemetry("fallback: postponing because triggerInProgress (attempt=${fallbackAttemptCount.get()})")
                                handler.postDelayed(this, 300)
                                return
                            }

                            val handledTs = prefs.getLong("last_handled_trigger_ts", 0L)
                            val now = System.currentTimeMillis()
                            if (handledTs != 0L && handledTs >= lastUserTriggerTs && now - handledTs < 2000) {
                                android.util.Log.e("FloatingService", "Skipping fallback because AccessibilityService already handled trigger (handledTs=${'$'}handledTs) (attempt=${'$'}attemptIndex)")

                            }

                            // If IME already active, skip fallback
                            try {
                                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                if (try { imm.isAcceptingText() } catch (_: Exception) { false }) {
                                    android.util.Log.e("FloatingService", "Skipping fallback because IME reports active (attempt=${'$'}attemptIndex)")
                                    appendTelemetry("fallback: imeActive true (attempt=$attemptIndex)")
                                    return
                                }
                            } catch (_: Exception) {}

                            // Not handled yet — either retry or start safe Activity fallback
                            if (attemptIndex < attemptIntervals.size - 1) {
                                appendTelemetry("fallback: retrying (attempt=$attemptIndex)")
                                attemptIndex++
                                handler.postDelayed(this, attemptIntervals[attemptIndex])
                            } else {
                                appendTelemetry("fallback: final attempt -> starting KeyboardShowActivity (attempt=$attemptIndex)")
                                try {
                                    val act = Intent(this@FloatingService, KeyboardShowActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                    startActivity(act)
                                    android.util.Log.e("FloatingService", "No ACK — started KeyboardShowActivity as safe fallback (overlay removed)")
                                } catch (e: Exception) {
                                    hatayiDosyayaYaz(e)
                                }
                            }
                        } catch (e: Exception) {
                            hatayiDosyayaYaz(e)
                            try { unregisterReceiver(ackReceiver) } catch (_: Exception) {}
                        }
                    }
                }
                handler.postDelayed(fallbackChecker, attemptIntervals[0])

            } catch (e: Exception) {
                hatayiDosyayaYaz(e)
                Toast.makeText(this, "Klavye hatası!", Toast.LENGTH_SHORT).show()
            }
        }

        // long-press = immediate user kill-switch for the floating service
        floatingButton.setOnLongClickListener {
            try {
                val prefs = getSharedPreferences("keyboard_trigger_prefs", MODE_PRIVATE)
                prefs.edit().putBoolean("service_enabled", false).apply()
                android.util.Log.e("FloatingService", "Floating button long-press -> stopping service (user kill-switch)")
                try { if (floatingButton.parent != null) windowManager.removeView(floatingButton) } catch (_: Exception) {}
                stopSelf()
            } catch (_: Exception) {}
            true
        }

        floatingButton.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (abs(dx) > 10 || abs(dy) > 10) {
                            isDragging = true
                            params.x = initialX + dx
                            params.y = initialY + dy
                            windowManager.updateViewLayout(floatingButton, params)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            v.performClick()
                        } else {
                            val middle = screenWidth / 2
                            if (params.x >= middle) {
                                params.x = screenWidth - params.width
                            } else {
                                params.x = 0
                            }
                            windowManager.updateViewLayout(floatingButton, params)
                        }
                        return true
                    }
                }
                return false
            }
        })

        Log.e("FloatingService", "Adding floating button to window (type=${'$'}{params.type}, flags=${'$'}{params.flags})")
        try {
            windowManager.addView(floatingButton, params)
            floatingButtonAdded = true
            Log.e("FloatingService", "Floating button added successfully (type=${'$'}{params.type}, flags=${'$'}{params.flags})")
        } catch (e: Exception) {
            Log.e("FloatingService", "Failed to add floating button: ${e.message}")
            hatayiDosyayaYaz(e)
            // Show notification guiding user to enable overlay permission
            try {
                val nm = getSystemService(NotificationManager::class.java)
                val channelId = "keyboard_trigger_alerts"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val ch = NotificationChannel(channelId, "Keyboard Trigger Alerts", NotificationManager.IMPORTANCE_HIGH)
                    nm.createNotificationChannel(ch)
                }
                val overlayIntent = PendingIntent.getActivity(this, 301, Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:$packageName")), PendingIntent.FLAG_IMMUTABLE)
                val notif = NotificationCompat.Builder(this, channelId)
                    .setContentTitle("Keyboard Trigger: Overlay izni gerekli")
                    .setContentText("Floating buton görüntülenemedi. Lütfen Üstte Gösterme iznini verin.")
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setOngoing(false)
                    .addAction(android.R.drawable.ic_menu_manage, "İzni Ver", overlayIntent)
                    .build()
                nm.notify(4, notif)
            } catch (_: Exception) {}
        }
    
    }

    private fun showSelectionBubble(cx: Int, cy: Int) {
        try {
            // Check preference
            val prefs = getSharedPreferences("keyboard_trigger_prefs", MODE_PRIVATE)
            if (!prefs.getBoolean("selection_bubble_enabled", false)) return

            // If already showing, reset timer
            if (selectionBubbleView != null) {
                handler.removeCallbacks(selectionBubbleHideRunnable)
                handler.postDelayed(selectionBubbleHideRunnable, 3000)
                return
            }

            val bubble = TextView(this).apply {
                text = "⌨"
                textSize = 16f
                setTextColor(android.graphics.Color.WHITE)
                val shape = android.graphics.drawable.GradientDrawable()
                shape.shape = android.graphics.drawable.GradientDrawable.OVAL
                shape.setColor(android.graphics.Color.parseColor("#FF6200EE"))
                background = shape
                setPadding(20, 10, 20, 10)
                setOnClickListener {
                    // Trigger keyboard via broadcast
                    try {
                        val intent = Intent("com.universish.libre.keyboardtrigger.ACTION_TRIGGER_KEYBOARD").apply { setPackage(packageName) }
                        sendBroadcast(intent)
                    } catch (e: Exception) {
                        hatayiDosyayaYaz(e)
                    }
                    // remove bubble after action
                    removeSelectionBubble()
                }
            }

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_SECURE,
                PixelFormat.TRANSLUCENT
            )
            lp.gravity = Gravity.TOP or Gravity.START
            // place centered at provided coordinates
            lp.x = cx - 40
            lp.y = cy - 40

            windowManager.addView(bubble, lp)
            selectionBubbleView = bubble
            selectionBubbleParams = lp

            handler.postDelayed(selectionBubbleHideRunnable, 3000)
        } catch (e: Exception) {
            hatayiDosyayaYaz(e)
            removeSelectionBubble()
        }
    }

    private val selectionBubbleHideRunnable = Runnable {
        removeSelectionBubble()
    }

    private fun removeSelectionBubble() {
        try {
            selectionBubbleView?.let { windowManager.removeView(it) }
        } catch (_: Exception) {}
        selectionBubbleView = null
        selectionBubbleParams = null
    }

    // Overlay fallback removed for safety. This method is now a no-op and will not add any window overlays.
    private fun showKeyboardOverlayFallback() {
        android.util.Log.e("FloatingService", "showKeyboardOverlayFallback called but overlay strategy has been REMOVED — no action taken")
        // Ensure any previously added overlay is removed (defensive)
        try { removeOverlayFallback() } catch (_: Exception) {}
    }

    private fun removeOverlayFallback() {
        try {
            overlayPollingRunnable?.let { handler.removeCallbacks(it) }
            overlayPollingRunnable = null
            overlayView?.let { windowManager.removeView(it) }
            overlayView = null
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun triggerKeyboard() {
        Log.e("FloatingService", "Triggering keyboard (legacy)")
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    private fun startPermissionMonitoring() {
        // Periodically check overlay and accessibility permissions and show persistent notification if missing
        permissionCheckRunnable = object : Runnable {
            override fun run() {
                try {
                    val overlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.provider.Settings.canDrawOverlays(this@FloatingService) else true
                    val am = getSystemService(ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
                    val accessibility = run {
                        val enabled = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                        var found = false
                        for (s in enabled) {
                            if (s.resolveInfo.serviceInfo.packageName == packageName && s.resolveInfo.serviceInfo.name == KeyboardTriggerAccessibilityService::class.java.name) { found = true; break }
                        }
                        found
                    }

                    if (!overlay || !accessibility) {
                        showPermissionNotification(overlay, accessibility)
                    } else {
                        cancelPermissionNotification()
                    }
                } catch (e: Exception) {
                    hatayiDosyayaYaz(e)
                }
                handler.postDelayed(this, 5000)
            }
        }
        handler.post(permissionCheckRunnable!!)
    }

    private fun showPermissionNotification(overlay: Boolean, accessibility: Boolean) {
        try {
            val nm = getSystemService(NotificationManager::class.java)
            val channelId = "keyboard_trigger_alerts"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val ch = NotificationChannel(channelId, "Keyboard Trigger Alerts", NotificationManager.IMPORTANCE_HIGH)
                nm.createNotificationChannel(ch)
            }
            val overlayIntent = PendingIntent.getActivity(this, 201, Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")), PendingIntent.FLAG_IMMUTABLE)
            val accessIntent = PendingIntent.getActivity(this, 202, Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS), PendingIntent.FLAG_IMMUTABLE)

            val notif = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Keyboard Trigger: İzin gerekli")
                .setContentText(buildString { if (!overlay) append("Üstte gösterme izni eksik. ") ; if (!accessibility) append("Erişilebilirlik devre dışı. ") })
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_manage, if (!overlay) "Üstte Göster" else "Üstte Göster ✓", overlayIntent)
                .addAction(android.R.drawable.ic_menu_info_details, if (!accessibility) "Erişilebilirlik" else "Erişilebilirlik ✓", accessIntent)
                .build()
            nm.notify(3, notif)
        } catch (e: Exception) {
            hatayiDosyayaYaz(e)
        }
    }

    private fun cancelPermissionNotification() {
        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm.cancel(3)
        } catch (_: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val notificationManager = getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "keyboard_trigger_channel",
                    "Keyboard Trigger Service",
                    NotificationManager.IMPORTANCE_LOW
                )
                notificationManager.createNotificationChannel(channel)
            }

            val notificationIntent = Intent().apply {
                setClassName(this@FloatingService, "com.universish.libre.keyboardtrigger.MainActivity")
            }
            val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

            val notification = NotificationCompat.Builder(this, "keyboard_trigger_channel")
                .setContentTitle("Keyboard Trigger")
                .setContentText("Floating button is active")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .build()

            // Ensure we call startForeground promptly as required by platform
            try {
                startForeground(1, notification)
            } catch (e: Exception) {
                hatayiDosyayaYaz(e)
            }

            // SAFETY: overlay fallback has been removed from the codebase — honor persisted service_enabled pref
            android.util.Log.e("FloatingService", "SAFETY: overlay fallback removed from runtime; Activity-based fallback will be used")

            // Respect persisted user preference: if user disabled the service, stop immediately
            try {
                val prefs = getSharedPreferences("keyboard_trigger_prefs", MODE_PRIVATE)
                if (!prefs.getBoolean("service_enabled", true)) {
                    android.util.Log.e("FloatingService", "Service disabled by preference -> stopping")
                    stopSelf()
                    return START_NOT_STICKY
                }
            } catch (_: Exception) {}

            // Post the heavy UI add operation to the main thread event queue to avoid blocking
            handler.post { try { baslat() } catch (e: Exception) { hatayiDosyayaYaz(e) } }
        } catch (e: Exception) {
            hatayiDosyayaYaz(e)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(overlayRequestReceiver) } catch (_: Exception) {}
        try { unregisterReceiver(selectionBubbleReceiver) } catch (_: Exception) {}
        try { unregisterReceiver(emergencyReceiver) } catch (_: Exception) {}
        removeOverlayFallback()
        removeSelectionBubble()
        permissionCheckRunnable?.let { handler.removeCallbacks(it) }
        if (::floatingButton.isInitialized) {
            try {
                if (floatingButton.parent != null) windowManager.removeView(floatingButton)
            } catch (e: Exception) {
            }
            floatingButtonAdded = false
        }
    }
}

