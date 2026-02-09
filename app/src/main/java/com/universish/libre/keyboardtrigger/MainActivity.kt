package com.universish.libre.keyboardtrigger

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.accessibility.AccessibilityManager
import android.accessibilityservice.AccessibilityServiceInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("MainActivity", "onCreate called")
        Toast.makeText(this, "App started", Toast.LENGTH_SHORT).show()

        // Sadece overlay izni kontrol et
        if (hasPermission()) {
            startFloatingService()
        } else {
            // İzin yoksa UI göster
            setupPermissionUI()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.e("MainActivity", "onResume called")
        Toast.makeText(this, "App resumed", Toast.LENGTH_SHORT).show()
        // Kullanıcı ayarlardan dönünce tekrar kontrol et
        if (hasPermission()) {
            startFloatingService()
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityManager = getSystemService(ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (service in enabledServices) {
            if (service.resolveInfo.serviceInfo.packageName == packageName &&
                service.resolveInfo.serviceInfo.name == KeyboardTriggerAccessibilityService::class.java.name) {
                return true
            }
        }
        return false
    }

    private fun hasPermission(): Boolean {
        val overlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true
        val accessibility = isAccessibilityServiceEnabled()
        Log.e("MainActivity", "hasPermission: overlay=$overlay, accessibility=$accessibility")
        return overlay && accessibility
    }

    private fun setupPermissionUI() {
        // XML kullanmadan kodla arayüz oluşturuyoruz (Hata riskini azaltır)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(50, 50, 50, 50)
        }

        val textInfo = TextView(this).apply {
            text = "Bu uygulamanın çalışması için\n'Diğer Uygulamaların Üzerinde Göster'\nve 'Erişilebilirlik' izinleri gereklidir."
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 50)
        }

        val btnGrantOverlay = Button(this).apply {
            text = "ÜSTTE GÖSTER İZNİ VER"
            textSize = 16f
            setBackgroundColor(Color.parseColor("#3DDC84"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                requestPermission()
            }
        }

        val btnGrantAccessibility = Button(this).apply {
            text = "ERİŞİLEBİLİRLİK İZNİ VER"
            textSize = 16f
            setBackgroundColor(Color.parseColor("#FF9800"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                requestAccessibilityPermission()
            }
        }

        layout.addView(textInfo)
        layout.addView(btnGrantOverlay)
        layout.addView(btnGrantAccessibility)
        setContentView(layout)
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            } catch (e: Exception) {
                // Bazı telefonlarda direkt paket sayfasına gidemezse
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                startActivity(intent)
            }
        }
    }

    private fun requestAccessibilityPermission() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Ayarlar açılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startFloatingService() {
        Log.e("MainActivity", "startFloatingService called")
        val intent = Intent().apply {
            setClassName(this@MainActivity, "com.universish.libre.keyboardtrigger.FloatingService")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        // Servis başladı, artık aktiviteyi kapatabiliriz.
        finish()
    }
}
