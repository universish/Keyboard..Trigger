// Application creator and developer: universish (Saffet Yavuz)
// License: GPLv3 (see LICENSE in repo)
/*
    Program Name: Keyboard Trigger
    Program Package Name or Application ID: com.universish.libre.keyboardtrigger

    About and Idea: The dynamic button that activates the keyboard is a button that can be placed on the edges of the screen. Pressing the button brings up the keyboard. The application is privacy-focused.  There is no tracking or advertising. It does not require internet access. It does not interfere with text editing and does not break focus. It is a FLOSS Android application.

    Copyright © 2026 Saffet Yavuz https://github.com/universish  (Developer's username: universish)

    This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

    To obtain information about the WARRANTY, you must read the LICENSE paper.

    You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

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

        // If activity launched with auto_start=true, try to auto start the service and finish
        val autoStart = intent?.getBooleanExtra("auto_start", false) ?: false
        if (autoStart) {
            ensurePermissionsAndStartService(autoStart = true)
        } else {
            // show status UI so user can see permission checks and controls
            ensurePermissionsAndStartService(autoStart = false)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.e("MainActivity", "onResume called")
        // Re-evaluate when user returns from settings
        ensurePermissionsAndStartService(autoStart = false)
    }

    private fun ensurePermissionsAndStartService(autoStart: Boolean) {
        val overlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true
        val accessibility = isAccessibilityServiceEnabled()
        Log.e("MainActivity", "hasPermission: overlay=$overlay, accessibility=$accessibility")

        if (overlay && accessibility && autoStart) {
            // Clear any setup notification and start (auto mode)
            cancelSetupNotification()
            startFloatingService()
            return
        }

        // Always show UI when user opened the app manually so they can see statuses
        showPermissionUI(overlay, accessibility)

        // Show persistent notification with quick actions to settings if missing
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
        val prefs = getSharedPreferences("keyboard_trigger_prefs", MODE_PRIVATE)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(30, 30, 30, 30)
        }

        // Add About button
        val btnAbout = Button(this).apply {
            text = "Hakkında"
            setOnClickListener { startActivity(Intent(this@MainActivity, AboutActivity::class.java)) }
        }
        // License button (links to repo LICENSE URL)
        val btnLicenseMain = Button(this).apply {
            text = "LICENSE"
            setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/universish/Keyboard..Trigger/blob/main/LICENSE"))) }
        }
        // Other licenses in-app
        val btnOtherLicensesMain = Button(this).apply {
            text = "OTHER LICENSES FOR THE APP*"
            setOnClickListener { startActivity(Intent(this@MainActivity, LicensesActivity::class.java)) }
        }
        layout.addView(btnAbout)
        layout.addView(btnLicenseMain)
        layout.addView(btnOtherLicensesMain)

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
            setPadding(0, 0, 0, 20)
        }

        // Overlay toggle
        val overlaySwitch = android.widget.Switch(this).apply {
            text = "Overlay fallback (opsiyonel)"
            isChecked = prefs.getBoolean("overlay_fallback_enabled", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("overlay_fallback_enabled", checked).apply()
            }
            setPadding(0, 10, 0, 10)
        }

        // Selection bubble toggle
        val selectionSwitch = android.widget.Switch(this).apply {
            text = "Seçim balonu (opsiyonel)"
            isChecked = prefs.getBoolean("selection_bubble_enabled", false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("selection_bubble_enabled", checked).apply()
            }
            setPadding(0, 10, 0, 10)
        }

        val btnOverlay = Button(this).apply {
            isEnabled = !overlay
            if (!overlay) {
                text = "ÜSTTE GÖSTER İZNİ VER"
                // default background
            } else {
                // show checkmark in custom color and bold, and set button background to #bbf3ae
                val sb = android.text.SpannableStringBuilder("ÜSTTE GÖSTER İZNİ VERİLDİ ")
                val check = "✓"
                val start = sb.length
                sb.append(check)
                sb.setSpan(android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#a2bfff")), start, start+1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, start+1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                text = sb
                setBackgroundColor(android.graphics.Color.parseColor("#bbf3ae"))
            }
            setOnClickListener { requestOverlayPermission() }
        }
        val btnAccess = Button(this).apply {
            isEnabled = !accessibility
            if (!accessibility) {
                text = "ERİŞİLEBİLİRLİK İZNİ VER"
            } else {
                val sb = android.text.SpannableStringBuilder("ERİŞİLEBİLİRLİK AKTİF ")
                val check = "✓"
                val start = sb.length
                sb.append(check)
                sb.setSpan(android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#a2bfff")), start, start+1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, start+1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                text = sb
                setBackgroundColor(android.graphics.Color.parseColor("#bbf3ae"))
            }
            setOnClickListener { requestAccessibilityPermission() }
        }

        val btnDebug = Button(this).apply {
            text = "Hata raporu oluştur & paylaş"
            setOnClickListener { createAndShareDebugReport() }
        }

        // Service control button shown in UI so user can start/stop manually
        val btnService = Button(this).apply {
            text = "Servisi Başlat / Yeniden Başlat"
            setOnClickListener {
                if (!overlay) Toast.makeText(this@MainActivity, "Lütfen önce Üstte Göster iznini verin", Toast.LENGTH_SHORT).show()
                else if (!accessibility) Toast.makeText(this@MainActivity, "Lütfen önce Erişilebilirlik iznini verin", Toast.LENGTH_SHORT).show()
                else {
                    startFloatingService()
                    Toast.makeText(this@MainActivity, "Servis başlatıldı", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Status indicators
        val status = TextView(this).apply {
            text = buildString {
                append(if (overlay) "Üstte gösterme: ✅\n" else "Üstte gösterme: ❌\n")
                append(if (accessibility) "Erişilebilirlik: ✅\n" else "Erişilebilirlik: ❌\n")
                append("Overlay fallback: " + if (overlaySwitch.isChecked) "Açık" else "Kapalı")
            }
            textSize = 14f
            setPadding(0, 10, 0, 10)
        }

        layout.removeAllViews()
        layout.addView(info)
        layout.addView(status)
        layout.addView(overlaySwitch)
        layout.addView(selectionSwitch)
        layout.addView(btnOverlay)
        layout.addView(btnAccess)
        layout.addView(btnService)
        layout.addView(btnDebug)
        // Add main UI quick links
        layout.addView(btnAbout)
        layout.addView(btnLicenseMain)
        layout.addView(btnOtherLicensesMain)
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

    private fun createAndShareDebugReport() {
        // Generate a sanitized log report and prompt user to share via email
        Thread {
            try {
                val pkg = packageName
                val proc = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "time", "FloatingService:*", "AccessibilityService:*", "KeyboardShowActivity:*", "*:S"))
                val reader = proc.inputStream.bufferedReader()
                val raw = reader.readText()
                reader.close()

                // Sanitize: remove likely PII like emails, phone numbers, long hex tokens
                var sanitized = raw
                // emails
                sanitized = sanitized.replace(Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"), "[REDACTED_EMAIL]")
                // phones (simple)
                sanitized = sanitized.replace(Regex("\\+?\\d[\\d ()-]{6,}"), "[REDACTED_PHONE]")
                // long hex/base64 tokens
                sanitized = sanitized.replace(Regex("(?i)[A-F0-9]{16,}"), "[REDACTED_HEX]")
                sanitized = sanitized.replace(Regex("[A-Za-z0-9-_]{32,}"), "[REDACTED_TOKEN]")

                val ts = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(java.time.LocalDateTime.now())
                val fname = "keyboard_trigger_report_$ts.txt"
                val f = java.io.File(cacheDir, fname)
                f.writeText("Keyboard Trigger Debug Report\nTime: $ts\nPackage: $pkg\n\n--- LOGS ---\n\n" + sanitized)

                // Ask user to share via email
                runOnUiThread {
                    val builder = androidx.appcompat.app.AlertDialog.Builder(this)
                    builder.setTitle("Hata raporu hazır")
                    builder.setMessage("Hata raporu oluşturuldu. Kişisel bilgiler kaldırıldı. E-posta ile paylaşmak ister misiniz?")
                    builder.setPositiveButton("Evet") { _, _ ->
                        try {
                            // use FileProvider
                            val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.fileprovider", f)
                            val i = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Keyboard Trigger Debug Report")
                                putExtra(Intent.EXTRA_TEXT, "Lütfen hata raporunu inceleyin.")
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            startActivity(Intent.createChooser(i, "Hata raporunu paylaş"))
                        } catch (e: Exception) {
                            Toast.makeText(this, "E-posta açılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    builder.setNegativeButton("Hayır", null)
                    builder.show()
                }

            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Hata raporu oluşturulamadı: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }.start()
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

