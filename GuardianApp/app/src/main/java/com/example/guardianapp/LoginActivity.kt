package com.example.guardianapp

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity   // ← FIXED: was ComponentActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("GuardianPrefs", MODE_PRIVATE)

        // ── AUTO LOGIN ──
        if (prefs.contains("worker")) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val scrollView = ScrollView(this)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(40, 80, 40, 40)

        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.parseColor("#020617"),
                Color.parseColor("#0F172A"),
                Color.parseColor("#1E293B")
            )
        )
        layout.background = gradient

        // ── TITLE ──
        val title = TextView(this)
        title.text = "🛡 Guardian Login"
        title.textSize = 34f
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER
        title.setPadding(0, 0, 0, 60)

        // ── WORKER LABEL ──
        val workerLabel = TextView(this)
        workerLabel.text = "Select Worker"
        workerLabel.textSize = 18f
        workerLabel.setTextColor(Color.parseColor("#CBD5E1"))
        workerLabel.setPadding(0, 0, 0, 16)

        // ── WORKER SPINNER ──
        // Names MUST match Flask worker database exactly
        val workerSpinner = Spinner(this)
        val workers = arrayOf("KIMIYETO", "HEONG", "KOMAL", "SCHWARZENEGGER")
        workerSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            workers
        )

        // ── TUNNEL LABEL ──
        val tunnelLabel = TextView(this)
        tunnelLabel.text = "Select Tunnel"
        tunnelLabel.textSize = 18f
        tunnelLabel.setTextColor(Color.parseColor("#CBD5E1"))
        tunnelLabel.setPadding(0, 40, 0, 16)

        // ── TUNNEL SPINNER ──
        val tunnelSpinner = Spinner(this)
        val tunnels = arrayOf("ENTRY", "TUNNEL 1", "TUNNEL 2", "TUNNEL 3")
        tunnelSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            tunnels
        )

        // ── LOGIN BUTTON ──
        val loginButton = Button(this)
        loginButton.text = "ENTER GUARDIAN"
        loginButton.setBackgroundColor(Color.parseColor("#2563EB"))
        loginButton.setTextColor(Color.WHITE)
        loginButton.textSize = 20f

        val btnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        btnParams.setMargins(0, 60, 0, 0)
        loginButton.layoutParams = btnParams

        loginButton.setOnClickListener {
            val worker = workerSpinner.selectedItem.toString()
            val tunnel = tunnelSpinner.selectedItem.toString()

            prefs.edit()
                .putString("worker", worker)
                .putString("tunnel", tunnel)
                .apply()

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        layout.addView(title)
        layout.addView(workerLabel)
        layout.addView(workerSpinner)
        layout.addView(tunnelLabel)
        layout.addView(tunnelSpinner)
        layout.addView(loginButton)
        scrollView.addView(layout)
        setContentView(scrollView)
    }
}