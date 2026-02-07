package com.universish.libre.keyboardtrigger

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkPermissionAndStart()
    }

    private fun checkPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            // İzin yoksa iste
            Toast.makeText(this, "Lütfen 'Diğer uygulamaların üzerinde göster' iznini verin.", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, 123)
        } else {
            // İzin varsa servisi başlat ve çık
            startFloatingService()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startFloatingService()
            } else {
                Toast.makeText(this, "İzin verilmedi, uygulama çalışamaz.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startFloatingService() {
        val intent = Intent(this, FloatingService::class.java)
        // Android 8.0+ için startForegroundService gerekebilir ama Overlay servisi için startService yeterlidir.
        // Çökme ihtimaline karşı try-catch
        try {
            startService(intent)
            finish() // Aktiviteyi kapat
        } catch (e: Exception) {
            Toast.makeText(this, "Servis Başlatılamadı: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
