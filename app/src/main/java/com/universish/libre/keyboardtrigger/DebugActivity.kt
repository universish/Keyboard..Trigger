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
        text.text = "Lütfen aşağıdaki talimatları okuyun:\n" +
                "1. Aşağıdaki 'İleri' düğmesine basın.\n" +
                "2. Uygulamayı normal şekilde kullanarak testi yapın.\n" +
                "3. Hatanın tekrar oluşmasını sağlayın.\n" +
                "4. Hata meydana geldiğinde 'Yaptığım test bitti' düğmesine basın." 
        btnNext.text = "İleri"
        btnFinish.visibility = Button.GONE
    }

    private fun showStepTest() {
        step = 1
        text.text = "Testi gerçekleştirin:\n" +
                "• Uygulamayı kullanın ve hatayı tetikleyin.\n" +
                "• Hata tekrar oluştuğunda 'Yaptığım test bitti' düğmesine basın."
        btnNext.visibility = Button.GONE
        btnFinish.visibility = Button.VISIBLE
        btnFinish.text = "Yaptığım test bitti"
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
        text.text = "İşlem tamamlandı.\n" +
                "Kayıtlar Key-Trigger klasörüne kaydedildi.\n" +
                "Lütfen dosyaları açıp kişisel bilgilerinizin (e-posta, telefon, gizli veriler) silindiğinden emin olun.\n" +
                "Aşağıdaki düğmeye basmak, loglarınızı incelemem ve sorunu araştırmam için uygulama geliştiricisine göndermeyi kabul ettiğiniz anlamına gelir.\n" +
                "Bu işlem sırasında kişisel verileriniz silinmiş ve gizliliğiniz korunmuştur.\n\n" +
                "Onaylayıp e-posta penceresini açmak için 'Onay veriyorum' butonuna dokunun."
        btnNext.visibility = Button.VISIBLE
        btnNext.text = "Onay veriyorum"
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
                    append("=== HATA RAPORU FORMATI ===\n")
                    append("Kısa başlık (örneğin: 'Klavye tetiklenmiyor'):\n")
                    append("Açıklama:\nLütfen meydana gelen sorunu detaylı şekilde anlatın.\n")
                    append("Tekrar üretme adımları:\n1. \n2. \n3. \n")
                    append("Beklenen davranış:\n(Ne olmalıydı?)\n")
                    append("Gerçekleşen davranış:\n(Ne oldu?)\n")
                    append("Cihaz bilgileri:\n- Model:\n- Android sürümü:\n- Uygulama sürümü:\n")
                    append("Ek bilgiler, loglar ekte. Kişisel verileriniz raporlardan silinmiştir.\n")
                    append("Bu raporu göndermeden önce kişisel bilgilerinizin çıkarıldığını ve paylaşıma izin verdiğinizi onaylıyorsunuz.\n")
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
