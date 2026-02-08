package com.universish.libre.keyboardtrigger

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.inputmethod.InputMethodManager
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout

class TriggerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Şeffaf bir EditText oluştur
        val editText = EditText(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            isFocusable = true
            isFocusableInTouchMode = true
            visibility = View.INVISIBLE // Görünmez yap
        }
        
        val layout = LinearLayout(this).apply {
            addView(editText)
            setBackgroundColor(Color.TRANSPARENT)
        }
        
        setContentView(layout)
        
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                editText.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
            } catch(e: Exception) {
                e.printStackTrace()
            }
            finish()
            overridePendingTransition(0, 0)
        }, 100)
    }
}
