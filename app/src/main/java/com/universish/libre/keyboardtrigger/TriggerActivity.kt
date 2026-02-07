package com.universish.libre.keyboardtrigger

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.inputmethod.InputMethodManager

class TriggerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Aktivite açılır açılmaz klavyeyi çağır
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        
        // İşimiz bitti, kullanıcı fark etmeden hemen kapan
        finish()
        
        // Animasyonu iptal et (Görünmez olması için)
        overridePendingTransition(0, 0)
    }
}
