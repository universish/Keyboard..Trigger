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
        
        // 1. Aktivite açıldı (Görünmez)
        // 2. 50ms sonra klavyeyi zorla aç
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
            } catch(e: Exception) {
                e.printStackTrace()
            }
            // 3. Kendini yok et
            finish()
            // 4. Kapanış animasyonunu sil (titreme olmasın)
            overridePendingTransition(0, 0)
        }, 50)
    }
}
