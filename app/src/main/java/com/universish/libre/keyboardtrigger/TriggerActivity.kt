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
        
        // 1. Aktivite açıldı (Kullanıcı görmüyor çünkü Translucent tema)
        
        // 2. 100ms gecikme ile klavyeyi çağır (Sistemin pencereyi algılaması için)
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
            } catch(e: Exception) {
                e.printStackTrace()
            }
            
            // 3. Görev tamamlandı, kendini yok et.
            finish()
            
            // 4. Kapanış animasyonunu iptal et (Titreme olmasın)
            overridePendingTransition(0, 0)
        }, 100)
    }
}
