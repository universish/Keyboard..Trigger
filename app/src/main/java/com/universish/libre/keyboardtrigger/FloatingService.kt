package com.universish.libre.keyboardtrigger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: ImageView // Direkt ImageView kullanıyoruz
    private lateinit var params: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? { return null }

    override fun onCreate() {
        super.onCreate()
        
        // 1. Bildirim (Kapanmaması için şart)
        startForegroundSafely()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // 2. Butonu Kodla Oluştur (XML Yok, Hata Yok)
        floatingView = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_input_add) // Sistem ikonu (Garanti)
            setColorFilter(Color.WHITE) // İkon rengi
            
            // Arkaplan (Yeşil Yuvarlak)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#3DDC84")) // Android Yeşili
                setStroke(2, Color.WHITE)
            }
            setPadding(20, 20, 20, 20)
            elevation = 10f
        }

        // 3. Pencere Ayarları
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            150, // Genişlik (px)
            150, // Yükseklik (px)
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, // Ekran dışına taşabilme
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.CENTER // Ekranın ortasında başla

        try {
            windowManager.addView(floatingView, params)
            setupTouchListener()
            Toast.makeText(this, "Klavye Tetikleyici Aktif", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startForegroundSafely() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "floating_service_channel"
            val channelName = "Keyboard Trigger"
            val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)

            val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Keyboard Trigger")
                .setContentText("Dokunarak klavyeyi aç")
                .setSmallIcon(android.R.drawable.ic_input_add)
                .build()

            startForeground(1, notification)
        }
    }

    private fun setupTouchListener() {
        floatingView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private val clickThreshold = 10

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true 
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val xDiff = abs(event.rawX - initialTouchX)
                        val yDiff = abs(event.rawY - initialTouchY)
                        if (xDiff < clickThreshold && yDiff < clickThreshold) {
                            toggleKeyboard()
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun toggleKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        
        // Tıklama animasyonu
        floatingView.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).withEndAction {
            floatingView.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) windowManager.removeView(floatingView)
    }
}
