package com.universish.libre.keyboardtrigger

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast

class TriggerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Kullanıcıya tepki ver
        // Toast.makeText(this, "Klavye...", Toast.LENGTH_SHORT).show()

        // Biraz bekleyip klavyeyi vur (Focus otursun diye)
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
            } catch(e: Exception) {
                e.printStackTrace()
            }
            // İşi bitir ve iz bırakmadan kaybol
            finish()
            overridePendingTransition(0, 0)
        }, 50) 
    }
}
