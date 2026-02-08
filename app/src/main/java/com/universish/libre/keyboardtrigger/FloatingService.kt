package com.universish.libre.keyboardtrigger

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import kotlin.math.abs

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingButton: TextView
    private lateinit var params: WindowManager.LayoutParams
    private var screenWidth = 0

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // Ekran genişliğini al (Mıknatıs hesabı için lazım)
        screenWidth = Resources.getSystem().displayMetrics.widthPixels

        // --- TASARIM GÜNCELLEMESİ ---
        floatingButton = TextView(this).apply {
            text = "^" // İnce ok işareti (Şapka)
            textSize = 24f 
            setTypeface(null, Typeface.BOLD) // Kalın yazı
            setTextColor(Color.BLACK) // Siyah Yazı
            gravity = Gravity.CENTER
            
            // Arka Plan: Yarı Saydam SARI ve Köşeleri Yuvarlak
            val shape = GradientDrawable()
            shape.shape = GradientDrawable.RECTANGLE
            shape.cornerRadius = 15f // Hafif yumuşak köşeler
            shape.setColor(Color.parseColor("#99FFEB3B")) // Şeffaf Sarı
            shape.setStroke(2, Color.parseColor("#FFC107")) // İnce turuncu kenarlık
            background = shape
        }

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            100, // Genişlik biraz arttı (rahat basılsın diye)
            120, // Yükseklik
            layoutFlag,
            // FLAG_NOT_FOCUSABLE: Odaklanma sorunu ve dalgalanma için şart
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        // Başlangıç konumu (Sağ kenar)
        params.gravity = Gravity.TOP or Gravity.START
        params.x = screenWidth - 100
        params.y = 300

        floatingButton.setOnClickListener {
            triggerKeyboard()
        }

        // --- MIKNATIS VE SÜRÜKLEME MANTIĞI ---
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
                        
                        // Ufak titremeleri sürükleme sayma
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
                            // MIKNATIS: Bırakınca en yakın kenara yapış
                            val middle = screenWidth / 2
                            if (params.x >= middle) {
                                params.x = screenWidth - 100 // Sağa yapış
                            } else {
                                params.x = 0 // Sola yapış
                            }
                            windowManager.updateViewLayout(floatingButton, params)
                        }
                        return true
                    }
                }
                return false
            }
        })

        try {
            windowManager.addView(floatingButton, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun triggerKeyboard() {
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            // 1. Yöntem: Zorla Aç
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        } catch (e: Exception) {
            // Hata olursa sessizce yut, uygulama çökmesin
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingButton.isInitialized) {
            try {
                windowManager.removeView(floatingButton)
            } catch (e: Exception) {
                // Zaten silindiyse sorun yok
            }
        }
    }
}
