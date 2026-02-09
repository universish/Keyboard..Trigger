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
- androidx.core:core-ktx:1.12.0 — Apache 2.0
- androidx.appcompat:appcompat:1.6.1 — Apache 2.0
- com.google.android.material:material:1.10.0 — Apache 2.0

GPLv3: Projenin ana lisansı GPLv3'tür. Lütfen LICENSE dosyasını inceleyin.
""".trimIndent()
            setPadding(24,24,24,24)
        }

        val sv = ScrollView(this).apply { addView(text) }
        setContentView(sv)
    }
}
