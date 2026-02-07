package com.universish.libre.keyboardtrigger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
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
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var iconView: ImageView

    override fun onBind(intent: Intent?): IBinder? { return null }

    override fun onCreate() {
        super.onCreate()
        startForegroundSafely()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null)
        iconView = floatingView.findViewById(R.id.floating_icon)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        // EKRAANIN TAM ORTASINA KOYUYORUZ (Görünmeme şansı yok)
        params.gravity = Gravity.CENTER
        params.x = 0
        params.y = 0

        try {
            windowManager.addView(floatingView, params)
            setupTouchListener()
            // Kullanıcıya bilgi ver
            Toast.makeText(this, "Buton oluşturuldu!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun startForegroundSafely() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "floating_service_channel"
            val channelName = "Keyboard Trigger Service"
            val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)

            val notification: Notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Keyboard Trigger")
                .setContentText("Klavye butonu aktif.")
                .setSmallIcon(R.drawable.ic_app_icon)
                .build()

            startForeground(1, notification)
        }
    }

    private fun setupTouchListener() {
        iconView.setOnTouchListener(object : View.OnTouchListener {
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
                        // Gravity CENTER iken x,y davranışı farklı olabilir,
                        // bu yüzden sürükleme sonrası Gravity'i TOP|LEFT'e çekiyoruz
                        params.gravity = Gravity.TOP or Gravity.START
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val xDiff = abs(event.rawX - initialTouchX)
                        val yDiff = abs(event.rawY - initialTouchY)
                        if (xDiff < clickThreshold && yDiff < clickThreshold) {
                            forceToggleKeyboard()
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun forceToggleKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        iconView.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction {
            iconView.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) windowManager.removeView(floatingView)
    }
}
