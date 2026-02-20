// Application creator and developer: universish (Saffet Yavuz)
// License: GPLv3 (see LICENSE in repo)
/*
    Program Name: Keyboard Trigger
    Program Package Name or Application ID: com.universish.libre.keyboardtrigger
    About and my Idea: The dynamic button that activates the keyboard is a button that can be placed on the edges of the screen. Pressing the button brings up the keyboard. The application is privacy-focused.  There is no tracking or advertising. It does not require internet access. It does not interfere with text editing and does not break focus. It is a FLOSS Android application.
    Copyright © 2026 Saffet Yavuz https://github.com/universish  
    (Developer's username: universish)

	This program comes with ABSOLUTELY NO WARRANTY; for details type
	`NO WARRANTY. BECAUSE THE PROGRAM IS LICENSED FREE OF CHARGE, THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.`

    This is free software, and you are welcome to redistribute it under certain conditions; type 
	`Keyboard..Trigger > Copyright (C) 2026 universish
	This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, and/or any later version.
	This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.' for details.
	You should have received a copy of the GNU General Public License along with this program. If not, see https://www.gnu.org/licenses/gpl-3.0.tr.html .`

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
import android.os.Handler
import android.os.Looper
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
        fun lang(en: String, tr: String): String {
            val l = prefs.getString("language", "en")
            return if (l == "tr") tr else en
        }
        // theme preference may affect colors
        val themePref = prefs.getString("theme","light") ?: "light"
        val isDark = themePref == "dark"
        val bgColor = if (isDark) Color.parseColor("#222222") else Color.parseColor("#F0F4F8")
        val buttonBaseColor = if (isDark) Color.parseColor("#444444") else Color.parseColor("#FF2196F3")
        val checkMarkColor = if (isDark) Color.parseColor("#bbbbff") else Color.parseColor("#a2bfff")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(bgColor)
            setPadding(30, 30, 30, 30)
        }

        // Add About button
        val btnAbout = Button(this).apply {
            text = lang("About","Hakkında")
            setOnClickListener { startActivity(Intent(this@MainActivity, AboutActivity::class.java)) }
            setBackgroundColor(buttonBaseColor)
            setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        }
        // License button (links to repo LICENSE URL)
        val btnLicenseMain = Button(this).apply {
            text = "LICENSE"
            setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/universish/Keyboard..Trigger/blob/main/LICENSE"))) }
            setBackgroundColor(buttonBaseColor)
            setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        }
        // Other licenses in-app
        val btnOtherLicensesMain = Button(this).apply {
            text = lang("Other licenses","Diğer lisanslar")
            setOnClickListener { startActivity(Intent(this@MainActivity, LicensesActivity::class.java)) }
            setBackgroundColor(buttonBaseColor)
            setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        }
        layout.addView(btnAbout)
        layout.addView(btnLicenseMain)
        layout.addView(btnOtherLicensesMain)

        val info = TextView(this).apply {
            text = buildString {
                append(lang("The following permissions are required for operation:\n\n","Uygulama çalışması için şu izinler gereklidir:\n\n"))
                append(if (!overlay) lang("• Display over other apps\n","• Üstte gösterme izni\n") else "")
                append(if (!accessibility) lang("• Accessibility service (Keyboard Trigger)\n","• Erişilebilirlik servisi (Keyboard Trigger)\n") else "")
                append(lang("\nPlease grant permissions and return.","\nLütfen izinleri verin, ardından geri dönün."))
            }
            textSize = 16f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }

        // Overlay fallback removed label is kept in code but hidden for now
        val overlayRemovedLabel = TextView(this).apply {
            text = "Overlay fallback is disabled for security" // english default, not shown
            visibility = TextView.GONE
        }

        // Selection bubble toggle
        val selectionSwitch = android.widget.Switch(this).apply {
            text = lang("Selection bubble (optional)","Seçim balonu (opsiyonel)")
            isChecked = prefs.getBoolean("selection_bubble_enabled", false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("selection_bubble_enabled", checked).apply()
            }
            setPadding(0, 10, 0, 10)
            setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        }

        val btnOverlay = Button(this).apply {
            isEnabled = !overlay
            if (!overlay) {
                text = lang("GRANT OVERLAY","ÜSTTE GÖSTER İZNİ VER")
                // default background uses theme
                setBackgroundColor(buttonBaseColor)
            } else {
                // show checkmark in custom color and bold, and set button background to accent
                val sb = android.text.SpannableStringBuilder(lang("OVERLAY GRANTED ","ÜSTTE GÖSTER İZNİ VERİLDİ "))
                val check = "✓"
                val start = sb.length
                sb.append(check)
                sb.setSpan(android.text.style.ForegroundColorSpan(checkMarkColor), start, start+1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, start+1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                text = sb
                setBackgroundColor(android.graphics.Color.parseColor("#bbf3ae"))
            }
            setOnClickListener { requestOverlayPermission() }
        }
        val btnAccess = Button(this).apply {
            isEnabled = !accessibility
            if (!accessibility) {
                text = lang("ENABLE ACCESSIBILITY","ERİŞİLEBİLİRLİK İZNİ VER")
                setBackgroundColor(buttonBaseColor)
            } else {
                val sb = android.text.SpannableStringBuilder(lang("ACCESSIBILITY ENABLED ","ERİŞİLEBİLİRLİK AKTİF "))
                val check = "✓"
                val start = sb.length
                sb.append(check)
                sb.setSpan(android.text.style.ForegroundColorSpan(checkMarkColor), start, start+1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, start+1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                text = sb
                setBackgroundColor(android.graphics.Color.parseColor("#bbf3ae"))
            }
            setOnClickListener { requestAccessibilityPermission() }
        }

        val btnDebug = Button(this).apply {
            text = lang("Debug issue","Hata ayıkla")
            setOnClickListener { startActivity(Intent(this@MainActivity, DebugActivity::class.java)) }
            setBackgroundColor(buttonBaseColor)
            setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        }

        // Service control button shown in UI so user can start/stop manually
        val btnService = Button(this).apply {
            text = lang("Start / Restart service","Servisi Başlat / Yeniden Başlat")
            setOnClickListener {
                val prefs = getSharedPreferences("keyboard_trigger_prefs", MODE_PRIVATE)
                if (!overlay) Toast.makeText(this@MainActivity, "Lütfen önce Üstte Göster iznini verin", Toast.LENGTH_SHORT).show()
                else if (!accessibility) Toast.makeText(this@MainActivity, "Lütfen önce Erişilebilirlik iznini verin", Toast.LENGTH_SHORT).show()
                else {
                    // persist that user explicitly enabled the service
                    prefs.edit().putBoolean("service_enabled", true).apply()
                    startFloatingService()
                    Toast.makeText(this@MainActivity, "Servis başlatıldı", Toast.LENGTH_SHORT).show()
                }
            }
            setBackgroundColor(buttonBaseColor)
            setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        }

        // Stop service button — allows immediate shutdown of floating service
        val btnStopService = Button(this).apply {
            text = lang("Stop service","Servisi Durdur")
            setOnClickListener {
                try {
                    val prefs = getSharedPreferences("keyboard_trigger_prefs", MODE_PRIVATE)
                    prefs.edit().putBoolean("service_enabled", false).apply()

                    val intent = Intent().apply { setClassName(this@MainActivity, "com.universish.libre.keyboardtrigger.FloatingService") }
                    stopService(intent)
                    Toast.makeText(this@MainActivity, "Servis durduruldu", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Servis durdurulamadı: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            setBackgroundColor(buttonBaseColor)
            setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        }

        // Status indicators
        val status = TextView(this).apply {
            text = buildString {
                append(if (overlay) lang("Overlay: ✅\n","Üstte gösterme: ✅\n") else lang("Overlay: ❌\n","Üstte gösterme: ❌\n"))
                append(if (accessibility) lang("Accessibility: ✅\n","Erişilebilirlik: ✅\n") else lang("Accessibility: ❌\n","Erişilebilirlik: ❌\n"))
                // removed fallback message as it's always hidden
            }
            textSize = 14f
            setPadding(0, 10, 0, 10)
        }

        layout.removeAllViews()
        layout.addView(info)
        layout.addView(status)
        layout.addView(overlayRemovedLabel)
        layout.addView(selectionSwitch)
        layout.addView(btnOverlay)
        layout.addView(btnAccess)
        layout.addView(btnService)
        layout.addView(btnStopService)
        layout.addView(btnDebug)
        // language switch
        // language selection: first show toggle button, then two choices
        val btnLang = Button(this).apply {
            text = lang("Language","Dil")
            setBackgroundColor(buttonBaseColor)
            setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        }
        val btnLangTr = Button(this).apply {
            text = "Türkçe"
            visibility = TextView.GONE
            setOnClickListener {
                prefs.edit().putString("language","tr").apply()
                recreate()
            }
            setBackgroundColor(buttonBaseColor)
            setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        }
        val btnLangEn = Button(this).apply {
            text = "English"
            visibility = TextView.GONE
            setOnClickListener {
                prefs.edit().putString("language","en").apply()
                recreate()
            }
            setBackgroundColor(buttonBaseColor)
            setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        }
        btnLang.setOnClickListener {
            btnLangTr.visibility = TextView.VISIBLE
            btnLangEn.visibility = TextView.VISIBLE
        }
        layout.addView(btnLang)
        layout.addView(btnLangTr)
        layout.addView(btnLangEn)
        // theme selector button
        val btnTheme = Button(this).apply {
            text = lang("Theme","Tema")
            setBackgroundColor(buttonBaseColor)
            setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        }
        val btnThemeLight = Button(this).apply {
            text = lang("Light","Açık")
            visibility = TextView.GONE
            setOnClickListener {
                prefs.edit().putString("theme","light").apply()
                recreate()
            }
            setBackgroundColor(buttonBaseColor)
            setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        }
        val btnThemeDark = Button(this).apply {
            text = lang("Dark","Koyu")
            visibility = TextView.GONE
            setOnClickListener {
                prefs.edit().putString("theme","dark").apply()
                recreate()
            }
            setBackgroundColor(buttonBaseColor)
            setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        }
        btnTheme.setOnClickListener {
            btnThemeLight.visibility = TextView.VISIBLE
            btnThemeDark.visibility = TextView.VISIBLE
        }
        layout.addView(btnTheme)
        layout.addView(btnThemeLight)
        layout.addView(btnThemeDark)
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
        // Generate a sanitized log report, include a GitHub-style issue template and prompt user to share via email
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

                // Prepend an issue-report template so the attached file and the email body guide the user
                val issueTemplate = buildString {
                    append("Kısa başlık (örn. 'Butona basınca klavye açılıyor, metin yazılamıyor'):\n")
                    append("\n**Açıklama**\n(Ne oluyor? Kısa ve öz yazın)\n\n")
                    append("**Tekrar üretme adımları**\n1. \n2. \n3. \n\n")
                    append("**Beklenen davranış**\n- \n\n")
                    append("**Gerçekleşen davranış**\n- \n\n")
                    append("**Ekran görüntüleri / ekran kaydı**\n(Lütfen gerekli yerleri ekleyin)\n\n")
                    append("**Cihaz bilgileri**\n- Model: \n- Android sürümü: \n- Uygulama sürümü: \n\n")
                    append("**Ek bilgiler / Loglar**\n(Lütfen varsa ek notlar)\n\n")
                    append("--- OTOMATİK EKLENEN ÖZET (lütfen düzenleyin) ---\n")
                    append("• Butona basınca klavye geliyor; metin editörüne yazı yazılamıyor. Android'de bozulma oluşuyor; normal klavye açılımıyla da yazılamıyor.\n")
                    append("• Açık uygulamalar butonuna basınca uygulamalar listeleniyor; herhangi bir uygulamaya dokununca uygulamanın GUI'si tam ekran açılmalı ama klavye açılıyor. Uygulama ekranı tam açılmıyor.\n")
                    append("• Klavye, klavye açılmaması gereken yerlerde açılıyor; bazen butona basmasam da açılıyor. Bir yere dokununca metin kutusu olmasa da klavye açılıyor.\n\n")
                    append("(Lütfen yukarıdaki alanları doldurun; ekran görüntülerini ekleyin. Oluşturulan log dosyası ektedir.)\n")
                }

                f.writeText("Keyboard Trigger Debug Report\nTime: $ts\nPackage: $pkg\n\n--- ISSUE TEMPLATE ---\n\n" + issueTemplate + "\n--- LOGS ---\n\n" + sanitized)

                // Ask user to share via email
                runOnUiThread {
                    val builder = androidx.appcompat.app.AlertDialog.Builder(this)
                    builder.setTitle("Hata raporu hazır")
                    builder.setMessage("Hata raporu oluşturuldu. Kişisel bilgiler kaldırıldı. E-posta ile paylaşmak ister misiniz?")
                    builder.setPositiveButton("Evet") { _, _ ->
                        try {
                            // use FileProvider
                            val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.fileprovider", f)

                            // prefill email: recipient, subject and body (issue template)
                            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "message/rfc822"
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("universish@tutamail.com"))
                                putExtra(Intent.EXTRA_SUBJECT, "[Hata Raporu] Keyboard Trigger — kısa başlık ekleyin")
                                putExtra(Intent.EXTRA_TEXT, issueTemplate)
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }

                            startActivity(Intent.createChooser(emailIntent, "Hata raporunu paylaş"))
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
        val prefs = getSharedPreferences("keyboard_trigger_prefs", MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val last = prefs.getLong("last_service_start_ts", 0L)
        // debounce rapid repeated start requests (prevents OS timeout race)
        if (now - last < 2000) {
            Log.e("MainActivity", "startFloatingService suppressed: debounced (delta=${'$'}{now-last}ms)")
            return
        }
        prefs.edit().putLong("last_service_start_ts", now).apply()

        val intent = Intent().apply {
            setClassName(this@MainActivity, "com.universish.libre.keyboardtrigger.FloatingService")
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            // defensive: schedule a short retry instead of crashing the Activity
            Log.e("MainActivity", "startForegroundService failed, scheduling retry: ${'$'}{e.message}")
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
                } catch (_: Exception) {}
            }, 500)
        }

        // instead of calling finish(), just move the task to the background so
        // an activity instance lives on. this keeps a proper entry and screenshot
        // in Recents, and allows tapping the tile to reopen the UI later.
        moveTaskToBack(true)
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

