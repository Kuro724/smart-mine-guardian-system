package com.example.guardianapp

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity   // ← FIXED: was ComponentActivity
import org.json.JSONArray
import java.net.URL
import kotlin.concurrent.thread

class WorkerTrackingActivity : AppCompatActivity() {

    private lateinit var workerContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 60, 40, 40)
        layout.setBackgroundColor(Color.parseColor("#020617"))

        val title = TextView(this)
        title.text = "👷 LIVE WORKER TRACKING"
        title.textSize = 30f
        title.setTypeface(null, android.graphics.Typeface.BOLD)
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER
        title.setPadding(0, 0, 0, 40)

        workerContainer = LinearLayout(this)
        workerContainer.orientation = LinearLayout.VERTICAL

        layout.addView(title)
        layout.addView(workerContainer)
        scrollView.addView(layout)
        setContentView(scrollView)

        // ── Poll every 3 seconds so tracking stays live ──
        startTrackingUpdates()
    }

    private fun startTrackingUpdates() {
        Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
            override fun run() {
                fetchWorkers()
                Handler(Looper.getMainLooper()).postDelayed(this, 3000)
            }
        }, 500)
    }

    private fun fetchWorkers() {
        thread {
            try {
                val jsonText = URL("https://unfarcical-don-bilgiest.ngrok-free.dev/workers").readText()
                val arr = JSONArray(jsonText)

                runOnUiThread {
                    workerContainer.removeAllViews()

                    if (arr.length() == 0) {
                        val empty = TextView(this)
                        empty.text = "⚪ NO ACTIVE WORKERS"
                        empty.textSize = 20f
                        empty.setTextColor(Color.parseColor("#CBD5E1"))
                        empty.gravity = Gravity.CENTER
                        workerContainer.addView(empty)
                        return@runOnUiThread
                    }

                    for (i in 0 until arr.length()) {
                        val obj      = arr.getJSONObject(i)
                        val worker   = obj.getString("worker")
                        val location = obj.getString("location")
                        val lastSeen = obj.getString("last_seen")

                        val card = TextView(this)
                        card.text = """
👷 $worker

📍 LOCATION
$location

🕒 LAST SEEN
$lastSeen
                        """.trimIndent()
                        card.textSize = 20f
                        card.setTextColor(Color.WHITE)
                        card.setPadding(40, 40, 40, 40)

                        val bg = GradientDrawable()
                        bg.cornerRadius = 40f
                        bg.setColor(Color.parseColor("#1E293B"))
                        bg.setStroke(2, Color.parseColor("#38BDF8"))
                        card.background = bg

                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        params.setMargins(0, 0, 0, 40)
                        card.layoutParams = params

                        workerContainer.addView(card)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}