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

    private val handler = Handler(Looper.getMainLooper())
    private var overlayView: EditText? = null
    private var overlayPollingRunnable: Runnable? = null

    // Broadcast receiver to accept explicit overlay requests (from AccessibilityService fallback)
    private val overlayRequestReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.e("FloatingService", "Received overlay request from AccessibilityService")
            try {
                showKeyboardOverlayFallback()
            } catch (e: Exception) {
                hatayiDosyayaYaz(e)
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

        // Create notification
        val notificationIntent = Intent().apply {
            setClassName(this@FloatingService, "com.universish.libre.keyboardtrigger.MainActivity")
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "keyboard_trigger_channel")
            .setContentTitle("Keyboard Trigger")
            .setContentText("Floating button is active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .build()

        // Start foreground service FIRST
        startForeground(1, notification)

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

            baslat()
        } catch (e: Exception) {
            hatayiDosyayaYaz(e)
            Toast.makeText(this, "HATA OLUŞTU! Dosyaya yazıldı.", Toast.LENGTH_LONG).show()
        }
    }

    private fun baslat() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        screenWidth = Resources.getSystem().displayMetrics.widthPixels

        floatingButton = TextView(this).apply {
            text = "⬆"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER

            val shape = GradientDrawable()
            shape.shape = GradientDrawable.RECTANGLE
            shape.cornerRadius = 15f
            shape.setColor(Color.parseColor("#99FFEB3B"))
            shape.setStroke(2, Color.parseColor("#FFC107"))
            background = shape
        }

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            100,
            140,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = screenWidth - 100
        params.y = 300

        var lastClick = 0L
        floatingButton.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastClick < 300) return@setOnClickListener // debounce
            lastClick = now
            Log.e("FloatingService", "Button clicked")
            try {
                // 1) Ask AccessibilityService to focus any editable node
                val intent = Intent("com.universish.libre.keyboardtrigger.ACTION_TRIGGER_KEYBOARD").apply { setPackage(packageName) }
                sendBroadcast(intent)

                // 2) Schedule a fallback overlay after a short delay; AccessibilityService may have handled it already
                handler.postDelayed({
                    try {
                        showKeyboardOverlayFallback()
                    } catch (e: Exception) {
                        hatayiDosyayaYaz(e)
                    }
                }, 300)

            } catch (e: Exception) {
                hatayiDosyayaYaz(e)
                Toast.makeText(this, "Klavye hatası!", Toast.LENGTH_SHORT).show()
            }
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
                                params.x = screenWidth - 100
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

        Log.e("FloatingService", "Adding floating button to window")
        try {
            windowManager.addView(floatingButton, params)
            Log.e("FloatingService", "Floating button added successfully")
        } catch (e: Exception) {
            Log.e("FloatingService", "Failed to add floating button: ${e.message}")
        }
    }

    private fun showKeyboardOverlayFallback() {
        // If already showing or added, skip
        if (overlayView != null) return
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            // Create a tiny invisible EditText that can receive focus
            val edit = EditText(this).apply {
                alpha = 0f
                isFocusable = true
                isFocusableInTouchMode = true
                isCursorVisible = false
                setTextIsSelectable(false)
                setTextColor(Color.TRANSPARENT)
                setBackgroundColor(Color.TRANSPARENT)
            }

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
            val lp = WindowManager.LayoutParams(
                1,
                1,
                layoutFlag,
                // important: do NOT set FLAG_NOT_FOCUSABLE so IME can attach
                0,
                PixelFormat.TRANSLUCENT
            )
            lp.gravity = Gravity.TOP or Gravity.START
            lp.x = 0
            lp.y = 0
            lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING

            windowManager.addView(edit, lp)
            overlayView = edit

            // Request focus and show IME
            edit.requestFocus()
            imm.showSoftInput(edit, InputMethodManager.SHOW_FORCED)

            // Poll IME state and remove overlay when keyboard is visible or after timeout
            var elapsed = 0
            overlayPollingRunnable = object : Runnable {
                override fun run() {
                    val active = try { imm.isAcceptingText() } catch (_: Exception) { false }
                    if (active) {
                        // Keep the overlay briefly so keyboard stays attached, then remove
                        handler.postDelayed({ removeOverlayFallback() }, 700)
                    } else {
                        elapsed += 200
                        if (elapsed > 3000) {
                            removeOverlayFallback()
                        } else {
                            handler.postDelayed(this, 200)
                        }
                    }
                }
            }
            handler.postDelayed(overlayPollingRunnable!!, 250)

        } catch (e: Exception) {
            hatayiDosyayaYaz(e)
            removeOverlayFallback()
        }
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

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(overlayRequestReceiver) } catch (_: Exception) {}
        removeOverlayFallback()
        if (::floatingButton.isInitialized) {
            try {
                windowManager.removeView(floatingButton)
            } catch (e: Exception) {
            }
        }
    }
}

