package com.universish.libre.keyboardtrigger

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Önce izni kontrol et
        if (hasPermission()) {
            startFloatingService()
        } else {
            // İzin yoksa UI göster (Çökme yerine bu ekran açılır)
            setupPermissionUI()
        }
    }

    override fun onResume() {
        super.onResume()
        // Kullanıcı ayarlardan dönünce tekrar kontrol et
        if (hasPermission()) {
            startFloatingService()
        }
    }

    private fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
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
            text = "Bu uygulamanın çalışması için\n'Diğer Uygulamaların Üzerinde Göster'\nizni gereklidir."
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 50)
        }

        val btnGrant = Button(this).apply {
            text = "İZİN VER"
            textSize = 16f
            setBackgroundColor(Color.parseColor("#3DDC84"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                requestPermission()
            }
        }

        layout.addView(textInfo)
        layout.addView(btnGrant)
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

    private fun startFloatingService() {
        val intent = Intent(this, FloatingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        // Servis başladı, artık aktiviteyi kapatabiliriz.
        finish()
    }
}
