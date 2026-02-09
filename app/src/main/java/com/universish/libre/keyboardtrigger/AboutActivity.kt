package com.universish.libre.keyboardtrigger

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        val btnReadme = Button(this).apply {
            text = "Readme (Repository)"
            setOnClickListener {
                val url = "https://github.com/universish/Keyboard..Trigger"
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }

        // Show default IME info
        val defaultImeInfo = TextView(this).apply {
            try {
                val defaultImeId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
                val pkg = if (defaultImeId.contains('/')) defaultImeId.substringBefore('/') else defaultImeId
                val label = try {
                    packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
                } catch (e: Exception) { pkg }
                text = "Varsayılan Klavye: $label ($pkg)"
            } catch (e: Exception) {
                text = "Varsayılan Klavye: bilinmiyor"
            }
            setPadding(0,16,0,16)
        }

        val btnLicense = Button(this).apply {
            text = "License (GPLv3)"
            setOnClickListener {
                val url = "https://github.com/universish/Keyboard..Trigger/blob/main/LICENSE"
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }

        val btnLicensesInApp = Button(this).apply {
            text = "Kütüphane Lisansları"
            setOnClickListener {
                startActivity(Intent(this@AboutActivity, LicensesActivity::class.java))
            }
        }

        val btnPrivacy = Button(this).apply {
            text = "Privacy"
            setOnClickListener {
                val url = "https://github.com/universish/Keyboard..Trigger/blob/main/PRIVACY.md"
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }

        val btnDonate = Button(this).apply {
            text = "Donate (universish)"
            setOnClickListener {
                val url = "https://github.com/universish"
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }

        layout.addView(btnReadme)
        layout.addView(defaultImeInfo)
        layout.addView(btnLicense)
        layout.addView(btnLicensesInApp)
        layout.addView(btnPrivacy)
        layout.addView(btnDonate)

        setContentView(layout)
    }
}
