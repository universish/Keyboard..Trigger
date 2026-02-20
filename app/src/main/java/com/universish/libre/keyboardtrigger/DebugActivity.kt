// Application creator and developer: universish (Saffet Yavuz)
// License: GPLv3 (see LICENSE in repo)
package com.universish.libre.keyboardtrigger

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime

class DebugActivity : Activity() {
    private lateinit var text: TextView
    private lateinit var btnNext: Button
    private lateinit var btnFinish: Button
    private var step = 0
    private var sanitizedReport: File? = null

    private val prefs by lazy { getSharedPreferences("keyboard_trigger_prefs", MODE_PRIVATE) }
    private fun lang(en: String, tr: String): String {
        val l = prefs.getString("language", "en")
        return if (l == "tr") tr else en
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        showStepInstructions()
    }

    private fun buildUi() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
        }
        text = TextView(this).apply {
            textSize = 16f
        }
        val scroll = ScrollView(this)
        scroll.addView(text)
        btnNext = Button(this).apply {
            setOnClickListener { onNext() }
        }
        btnFinish = Button(this).apply {
            setOnClickListener { onFinish() }
        }
        container.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        container.addView(btnNext)
        container.addView(btnFinish)
        setContentView(container)
    }

    private fun onNext() {
        when (step) {
            0 -> showStepTest()
            2 -> sendEmailWithReport()
        }
    }

    private fun onFinish() {
        when (step) {
            1 -> beginLogCollection()
        }
    }

    private fun showStepInstructions() {
        step = 0
        text.text = lang(
            "Please read the following instructions:\n1. Press the 'Next' button below.\n2. Use the app and reproduce the issue.\n3. Once the problem occurs again, press 'My test is done'.",
            "Lütfen aşağıdaki talimatları okuyun:\n1. Aşağıdaki 'İleri' düğmesine basın.\n2. Uygulamayı normal şekilde kullanarak testi yapın.\n3. Hatanın tekrar oluşmasını sağlayın.\n4. Hata meydana geldiğinde 'Yaptığım test bitti' düğmesine basın."
        )
        btnNext.text = lang("Next","İleri")
        btnFinish.visibility = Button.GONE
    }

    private fun showStepTest() {
        step = 1
        text.text = lang(
            "Perform the test:\n• Use the app and trigger the issue.\n• When the problem recurs, press 'My test is done'.",
            "Testi gerçekleştirin:\n• Uygulamayı kullanın ve hatayı tetikleyin.\n• Hata tekrar oluştuğunda 'Yaptığım test bitti' düğmesine basın."
        )
        btnNext.visibility = Button.GONE
        btnFinish.visibility = Button.VISIBLE
        btnFinish.text = lang("My test is done","Yaptığım test bitti")
    }

    private fun beginLogCollection() {
        step = 2
        btnFinish.visibility = Button.GONE
        text.text = "Lütfen bekleyin; hata kayıtları oluşturuluyor ve Key-Trigger klasörüne kaydediliyor.\n" +
                "Kişisel verileriniz silinecek." 
        Thread {
            collectAndSanitizeLogs()
        }.start()
    }

    private fun collectAndSanitizeLogs() {
        try {
            // pull logs via logcat
            val proc = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "time", "FloatingService:*", "AccessibilityService:*", "KeyboardShowActivity:*", "*:S"))
            val raw = proc.inputStream.bufferedReader().readText()
            proc.inputStream.close()

            val sanitized = sanitizeLog(raw)
            val ts = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
            val folder = File("/storage/emulated/0/Download/MyProjects")
            folder.mkdirs()
            val report = File(folder, "debug_report_$ts.txt")
            report.writeText(sanitized)
            sanitizedReport = report

            // sanitize telemetry and hata reports if exist
            sanitizeFile(File(folder, "TELEMETRY.txt"), File(folder, "TELEMETRY_sanitized_$ts.txt"))
            sanitizeFile(File(folder, "HATA_RAPORU.txt"), File(folder, "HATA_sanitized_$ts.txt"))

            runOnUiThread { showFinalScreen() }
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, "Kayıt oluşturulamadı: ${e.message}", Toast.LENGTH_LONG).show() }
        }
    }

    private fun sanitizeLog(raw: String): String {
        var out = raw
        out = out.replace(Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"), "[REDACTED_EMAIL]")
        out = out.replace(Regex("\\+?\\d[\\d ()-]{6,}"), "[REDACTED_PHONE]")
        out = out.replace(Regex("(?i)[A-F0-9]{16,}"), "[REDACTED_HEX]")
        out = out.replace(Regex("[A-Za-z0-9-_]{32,}"), "[REDACTED_TOKEN]")
        return out
    }

    private fun sanitizeFile(src: File, dst: File) {
        try {
            if (!src.exists()) return
            val raw = src.readText()
            dst.writeText(sanitizeLog(raw))
        } catch (_: Exception) {}
    }

    private fun showFinalScreen() {
        step = 2
        text.text = lang(
            "Process complete.\nLogs have been saved to the Key-Trigger folder.\nPlease review the files and ensure your personal data (email, phone, etc.) has been removed.\nPress the button below to send the sanitized logs to the developer. By doing so you consent to sharing non-personal information and agree to the terms of this procedure.",
            "İşlem tamamlandı.\nKayıtlar Key-Trigger klasörüne kaydedildi.\nLütfen dosyaları açıp kişisel bilgilerinizin (e-posta, telefon, gizli veriler) silindiğinden emin olun.\nAşağıdaki düğmeye basmak, loglarınızı incelemem ve sorunu araştırmam için uygulama geliştiricisine göndermeyi kabul ettiğiniz anlamına gelir.\nBu işlem sırasında kişisel verileriniz silinmiş ve gizliliğiniz korunmuştur.\n\nOnaylayıp e-posta penceresini açmak için 'Onay veriyorum' butonuna dokunun."
        )
        btnNext.visibility = Button.VISIBLE
        btnNext.text = lang("I consent","Onay veriyorum")
    }

    private fun sendEmailWithReport() {
        if (sanitizedReport == null) return
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", sanitizedReport!!)
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("universish@tutamail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "[Hata Raporu] Keyboard Trigger – lütfen kısa başlık ekleyin")
                putExtra(Intent.EXTRA_TEXT, buildString {
                    append(lang("=== BUG REPORT FORMAT ===\n","=== HATA RAPORU FORMATI ===\n"))
                    append(lang("Short title (e.g. 'Keyboard not appearing'):\n","Kısa başlık (örneğin: 'Klavye tetiklenmiyor'):\n"))
                    append(lang("Description:\nPlease detail what happened.\n","Açıklama:\nLütfen meydana gelen sorunu detaylı şekilde anlatın.\n"))
                    append(lang("Steps to reproduce:\n1. \n2. \n3. \n","Tekrar üretme adımları:\n1. \n2. \n3. \n"))
                    append(lang("Expected behavior:\n","Beklenen davranış:\n"))
                    append(lang("Actual behavior:\n","Gerçekleşen davranış:\n"))
                    append(lang("Device info:\n- Model:\n- Android version:\n- App version:\n","Cihaz bilgileri:\n- Model:\n- Android sürümü:\n- Uygulama sürümü:\n"))
                    append(lang("Additional info, logs attached. Personal data has been removed.\n","Ek bilgiler, loglar ekte. Kişisel verileriniz raporlardan silinmiştir.\n"))
                    append(lang("By sending this report you confirm the removal of personal data and consent to share non-personal information.\n","Bu raporu göndermeden önce kişisel bilgilerinizin çıkarıldığını ve paylaşıma izin verdiğinizi onaylıyorsunuz.\n"))
                })
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(emailIntent, "Hata raporunu paylaş"))
        } catch (e: Exception) {
            Toast.makeText(this, "E-posta açılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
