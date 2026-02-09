/*
    Program Name: Keyboard Trigger
    Program Package Name or Application ID: com.universish.libre.keyboardtrigger

    About and Idea: The dynamic button that activates the keyboard is a button that can be placed on the edges of the screen. Pressing the button brings up the keyboard. The application is privacy-focused.  There is no tracking or advertising. It does not require internet access. It does not interfere with text editing and does not break focus. It is a FLOSS Android application.

    Copyright © 2026 Saffet Yavuz https://github.com/universish  (Developer's username: universish)

    This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

    To obtain information about the WARRANTY, you must read the LICENSE paper.

    You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package com.universish.libre.keyboardtrigger

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
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

        // WARRANTY block (exact text as requested)
        val warrantyTextView = TextView(this).apply {
            text = """
Keyboard..Trigger > Copyright (C) 2026 universish

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

NO WARRANTY.
BECAUSE THE PROGRAM IS LICENSED FREE OF CHARGE, THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.
"""
            setPadding(0,8,0,12)
        }

        // About paragraph (exact text)
        val aboutTextView = TextView(this).apply {
            text = "The dynamic button that activates the keyboard is a button that can be placed on the edges of the screen. Pressing the button brings up the keyboard. The application is privacy-focused.  There is no tracking or advertising. It does not require internet access. It does not interfere with text editing and does not break focus. It is a FLOSS Android application."
            setPadding(0,8,0,12)
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

        layout.addView(warrantyTextView)
        layout.addView(aboutTextView)
        layout.addView(btnReadme)
        layout.addView(defaultImeInfo)
        layout.addView(btnLicense)
        layout.addView(btnLicensesInApp)
        layout.addView(btnPrivacy)
        layout.addView(btnDonate)

        setContentView(layout)
    }
}
