package com.universish.libre.keyboardtrigger

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import androidx.core.app.NotificationCompat

class MainActivity : AppCompatActivity() {

    private val CHANNEL_ID = "keyboard_trigger_setup"
    private val NOTIF_ID = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("MainActivity", "onCreate called")
        Toast.makeText(this, "App started", Toast.LENGTH_SHORT).show()
        createNotificationChannel()
        ensurePermissionsAndStartService()
    }

    override fun onResume() {
        super.onResume()
        Log.e("MainActivity", "onResume called")
        ensurePermissionsAndStartService()
    }

    private fun ensurePermissionsAndStartService() {
        val overlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true
        val accessibility = isAccessibilityServiceEnabled()
        Log.e("MainActivity", "hasPermission: overlay=$overlay, accessibility=$accessibility")

        if (overlay && accessibility) {
            // Clear any setup notification and start
            cancelSetupNotification()
            startFloatingService()
            return
        }

        // Show UI to help user grant missing permissions
        showPermissionUI(overlay, accessibility)

        // Show persistent notification with quick actions to settings
        showSetupNotification(overlay, accessibility)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabled = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (s in enabled) {
            if (s.resolveInfo.serviceInfo.packageName == packageName &&
                s.resolveInfo.serviceInfo.name == KeyboardTriggerAccessibilityService::class.java.name) {
                return true
            }
        }
        return false
    }

    private fun showPermissionUI(overlay: Boolean, accessibility: Boolean) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(50, 50, 50, 50)
        }

        val info = TextView(this).apply {
            text = buildString {
                append("Uygulama çalışması için şu izinler gereklidir:\n\n")
                append(if (!overlay) "• Üstte gösterme izni\n" else "")
                append(if (!accessibility) "• Erişilebilirlik servisi (Keyboard Trigger)\n" else "")
                append("\nLütfen izinleri verin, ardından geri dönün.")
            }
            textSize = 16f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }

        val btnOverlay = Button(this).apply {
            text = if (!overlay) "ÜSTTE GÖSTER İZNİ VER" else "ÜSTTE GÖSTER İZNİ VERİLDİ"
            isEnabled = !overlay
            setOnClickListener { requestOverlayPermission() }
        }
        val btnAccess = Button(this).apply {
            text = if (!accessibility) "ERİŞİLEBİLİRLİK İZNİ VER" else "ERİŞİLEBİLİRLİK AKTİF"
            isEnabled = !accessibility
            setOnClickListener { requestAccessibilityPermission() }
        }

        layout.removeAllViews()
        layout.addView(info)
        layout.addView(btnOverlay)
        layout.addView(btnAccess)
        setContentView(layout)
    }

    private fun requestOverlayPermission() {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivity(intent)
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
        finish()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(CHANNEL_ID, "Keyboard Trigger Setup", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(channel)
        }
    }

    private fun showSetupNotification(overlay: Boolean, accessibility: Boolean) {
        val overlayIntent = PendingIntent.getActivity(
            this, 101, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
            PendingIntent.FLAG_IMMUTABLE
        )
        val accessIntent = PendingIntent.getActivity(
            this, 102, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
            PendingIntent.FLAG_IMMUTABLE
        )

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Keyboard Trigger: Eksik izinler")
            .setContentText("Uygulamayı çalıştırmak için gerekli izinleri verin.")
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_manage, if (!overlay) "Üstte Göster" else "Üstte Göster ✓", overlayIntent)
            .addAction(android.R.drawable.ic_menu_info_details, if (!accessibility) "Erişilebilirlik" else "Erişilebilirlik ✓", accessIntent)
            .build()
        nm.notify(NOTIF_ID, notif)
    }

    private fun cancelSetupNotification() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIF_ID)
    }
}

