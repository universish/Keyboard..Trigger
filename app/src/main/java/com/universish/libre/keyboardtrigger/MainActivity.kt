package com.universish.libre.keyboardtrigger

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Arayüz yüklemeye gerek yok, şeffaf kalabilir veya boş.
        checkPermissionAndStart()
    }

    private fun checkPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Lütfen İzin Verin", Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, 123)
        } else {
            startServiceWithDelay()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startServiceWithDelay()
            } else {
                Toast.makeText(this, "İzin verilmedi", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startServiceWithDelay() {
        // Android'in "Activity Manager"ı bazen hemen izin verdiğini algılayamaz.
        // 500ms gecikme ile servisi başlatıp Activity'i öldürüyoruz.
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, FloatingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            // Uygulamayı tamamen kapat, arkada Activity kalmasın (Beyaz ekran çözümü)
            finishAndRemoveTask() 
        }, 500)
    }
}
