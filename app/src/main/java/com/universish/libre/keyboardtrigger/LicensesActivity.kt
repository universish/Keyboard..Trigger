package com.universish.libre.keyboardtrigger

import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LicensesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = TextView(this).apply {
            text = """
Kütüphane lisansları

Bu projede kullanılan kütüphanelerin lisansları:
- AndroidX (Android Open Source Project): Apache 2.0
- (Diğer kütüphaneler varsa buraya eklenecektir)

GPLv3: Projenin ana lisansı GPLv3'tür. Lütfen LICENSE dosyasını inceleyin.
""".trimIndent()
            setPadding(24,24,24,24)
        }

        val sv = ScrollView(this).apply { addView(text) }
        setContentView(sv)
    }
}
