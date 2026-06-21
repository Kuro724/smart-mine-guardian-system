package com.example.guardianapp

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    // =====================================
    // 🚨 UPDATE THESE NGROK URLs EVERY RESTART 🚨
    // =====================================
    // YOLO/RFID Server (Port 5000)
    private val YOLO_SERVER = "https://unfarcical-don-bilgiest.ngrok-free.dev"

    // SENSOR/PI Server (Port 5001)
    private val PI_SERVER = "https://clause-cold-condone.ngrok-free.dev"

    // =====================================
    // GLOBALS
    // =====================================
    private lateinit var currentWorker: String
    private lateinit var statsText: TextView
    private lateinit var workerImage: ImageView

    private var emergencyShowing = false
    private var mediaPlayer: MediaPlayer? = null

    // =====================================
    // ON CREATE
    // =====================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("GuardianPrefs", Context.MODE_PRIVATE)
        currentWorker = prefs.getString("worker", "UNKNOWN") ?: "UNKNOWN"

        val scrollView = ScrollView(this)
        scrollView.isFillViewport = true

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(45, 75, 45, 60)

        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.parseColor("#020617"),
                Color.parseColor("#0F172A"),
                Color.parseColor("#111827"),
                Color.parseColor("#1E293B")
            )
        )
        layout.background = gradient

        // =====================================
        // TOP BAR
        // =====================================
        val topBar = LinearLayout(this)
        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.gravity = Gravity.CENTER_VERTICAL

        // =====================================
        // LOGO
        // =====================================
        val logo = ImageView(this)
        logo.setImageResource(R.drawable.guardian_logo)
        val logoParams = LinearLayout.LayoutParams(170, 170)
        logo.layoutParams = logoParams

        val pulse = ObjectAnimator.ofFloat(logo, "alpha", 1f, 0.6f, 1f)
        pulse.duration = 2500
        pulse.repeatCount = ObjectAnimator.INFINITE
        pulse.start()

        // =====================================
        // TITLE AREA
        // =====================================
        val titleArea = LinearLayout(this)
        titleArea.orientation = LinearLayout.VERTICAL
        titleArea.setPadding(35, 0, 0, 0)

        val title = TextView(this)
        title.text = "Guardian Platform"
        title.textSize = 34f
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(Color.WHITE)

        val subtitle = TextView(this)
        subtitle.text = "Industrial Safety Command"
        subtitle.textSize = 18f
        subtitle.setTextColor(Color.parseColor("#CBD5E1"))
        subtitle.setPadding(0, 10, 0, 0)

        titleArea.addView(title)
        titleArea.addView(subtitle)
        topBar.addView(logo)
        topBar.addView(titleArea)

        // =====================================
        // CHIP ROW
        // =====================================
        val chipRow = LinearLayout(this)
        chipRow.orientation = LinearLayout.HORIZONTAL
        chipRow.gravity = Gravity.CENTER
        chipRow.setPadding(0, 35, 0, 40)

        chipRow.addView(createChip("📶 RFID ONLINE", "#16A34A"))
        chipRow.addView(createChip("🧠 AI ACTIVE", "#2563EB"))
        chipRow.addView(createChip("🌐 SERVER ONLINE", "#7C3AED"))

        // =====================================
        // WORKER CARD
        // =====================================
        val workerCard = createGlassCard()
        val workerLayout = LinearLayout(this)
        workerLayout.orientation = LinearLayout.VERTICAL
        workerLayout.gravity = Gravity.CENTER_HORIZONTAL
        workerLayout.setPadding(55, 55, 55, 55)

        val workerTitle = TextView(this)
        workerTitle.text = "👷 LIVE WORKER STATUS"
        workerTitle.textSize = 28f
        workerTitle.setTypeface(null, Typeface.BOLD)
        workerTitle.setTextColor(Color.WHITE)

        workerImage = ImageView(this)
        val imgParams = LinearLayout.LayoutParams(360, 360)
        imgParams.setMargins(0, 50, 0, 50)
        workerImage.layoutParams = imgParams
        workerImage.setImageResource(R.drawable.guardian_logo)

        statsText = TextView(this)
        statsText.text = "\n👷 WAITING FOR RFID...\n"
        statsText.textSize = 21f
        statsText.gravity = Gravity.CENTER
        statsText.setTextColor(Color.parseColor("#E2E8F0"))

        workerLayout.addView(workerTitle)
        workerLayout.addView(workerImage)
        workerLayout.addView(statsText)
        workerCard.addView(workerLayout)

        // =====================================
        // PPE CARD
        // =====================================
        val ppeCard = createDashboardCard("📷 PPE Monitoring", "Helmet & Vest Detection", "#2563EB")
        ppeCard.setOnClickListener { openWebPage(YOLO_SERVER) }

        // =====================================
        // SENSOR CARD
        // =====================================
        val sensorCard = createDashboardCard("🌡 Sensor Dashboard", "Gas & Temperature Monitoring", "#059669")
        sensorCard.setOnClickListener { openWebPage(PI_SERVER) }

        // =====================================
        // TRACKING CARD
        // =====================================
        val trackingCard = createDashboardCard("👷 Worker Tracking", "Live Worker Positions", "#7C3AED")
        trackingCard.setOnClickListener {
            startActivity(Intent(this, WorkerTrackingActivity::class.java))
        }

        // =====================================
        // MAP CARD
        // =====================================
        val tunnelMapCard = createDashboardCard("🗺 LIVE TUNNEL MAP", "AI Mine Monitoring System", "#0EA5E9")
        tunnelMapCard.setOnClickListener {
            startActivity(Intent(this, TunnelMapActivity::class.java))
        }

        // =====================================
        // SOS CARD
        // =====================================
        val sosCard = createDashboardCard("🚨 WORKER SOS", "Immediate Rescue Alert", "#DC2626")
        sosCard.setOnClickListener { triggerWorkerSOS() }

        // =====================================
        // EVAC CARD
        // =====================================
        val evacCard = createDashboardCard("⚠ GENERAL EVACUATION", "Evacuate All Workers", "#B91C1C")
        evacCard.setOnClickListener { triggerEvacuation() }

        layout.addView(topBar)
        layout.addView(chipRow)
        layout.addView(workerCard)
        layout.addView(ppeCard)
        layout.addView(sensorCard)
        layout.addView(trackingCard)
        layout.addView(tunnelMapCard)
        layout.addView(sosCard)
        layout.addView(evacCard)

        scrollView.addView(layout)
        setContentView(scrollView)

        startStatusUpdates()
        startEmergencyCheck()
    }

    // =====================================
    // FETCH STATUS (YOLO SERVER)
    // =====================================
    private fun fetchStatus() {
        thread {
            try {
                val url = URL("$YOLO_SERVER/status")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000

                val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(jsonText)

                val worker = obj.getString("worker")
                val helmet = obj.getBoolean("helmet")
                val vest = obj.getBoolean("vest")
                val decision = obj.getString("decision")
                val location = obj.getString("location")
                val lastSeen = obj.getString("last_seen")

                val imageRes = when (worker.uppercase()) {
                    "KIMIYETO" -> R.drawable.kimiyeto
                    "HEONG" -> R.drawable.heong
                    "KOMAL" -> R.drawable.komal
                    "SCHWARZENEGGER" -> R.drawable.schwarzenegger
                    else -> R.drawable.guardian_logo
                }

                runOnUiThread {
                    workerImage.setImageResource(imageRes)
                    statsText.text = """
                        👷 ACTIVE WORKER
                        $worker

                        📍 LOCATION
                        $location

                        🕒 LAST SEEN
                        $lastSeen

                        ━━━━━━━━━━━━━━━━━━

                        ⛑ HELMET
                        ${if (helmet) "✅ SAFE" else "❌ MISSING"}

                        🦺 VEST
                        ${if (vest) "✅ SAFE" else "❌ MISSING"}

                        🚪 ACCESS
                        $decision
                    """.trimIndent()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startStatusUpdates() {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                fetchStatus()
                // --- FIXED: Now pulls new data every 1 second instead of 3 ---
                handler.postDelayed(this, 1000)
            }
        }, 500)
    }

    // =====================================
    // EMERGENCY CHECK (PI SERVER)
    // =====================================
    private fun startEmergencyCheck() {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                checkEmergencyStatus()
                // --- FIXED: Now checks for emergencies every 1 second instead of 2 ---
                handler.postDelayed(this, 1000)
            }
        }, 500)
    }

    private fun checkEmergencyStatus() {
        thread {
            try {
                val url = URL("$PI_SERVER/emergency")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 1500
                conn.readTimeout = 1500
                
                conn.setRequestProperty("ngrok-skip-browser-warning", "true")

                val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(jsonText)
                val emergency = obj.getBoolean("active")

                if (!emergency) {
                    runOnUiThread { emergencyShowing = false }
                    return@thread
                }

                val type = obj.getString("type")
                val worker = obj.getString("worker")
                val location = obj.getString("location")

                runOnUiThread {
                    if (!emergencyShowing) {
                        emergencyShowing = true
                        showEmergencyOverlay(type, worker, location)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // =====================================
    // SOS (PI SERVER)
    // =====================================
    private fun triggerWorkerSOS() {
        Toast.makeText(this, "🚨 SOS sending as: $currentWorker", Toast.LENGTH_SHORT).show()

        thread {
            try {
                val url = URL("$PI_SERVER/receive-sos")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.setRequestProperty("Content-Type", "application/json")

                val json = """{"worker":"$currentWorker"}"""
                conn.outputStream.use { output ->
                    output.write(json.toByteArray(Charsets.UTF_8))
                    output.flush()
                }

                // Force execution
                conn.responseCode
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // =====================================
    // EVACUATION (PI SERVER)
    // =====================================
    private fun triggerEvacuation() {
        Toast.makeText(this, "⚠ Broadcasting Evacuation Order...", Toast.LENGTH_SHORT).show()
        thread {
            try {
                val url = URL("$PI_SERVER/send-alert")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.setRequestProperty("Content-Type", "application/json")

                val json = """{"alert":"ALERT:EVACUATE"}"""
                conn.outputStream.use { output ->
                    output.write(json.toByteArray(Charsets.UTF_8))
                    output.flush()
                }

                // Force execution
                conn.responseCode
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    // =====================================
    // OVERLAY
    // =====================================
    private fun showEmergencyOverlay(type: String, worker: String, location: String) {
        val overlayLayout = LinearLayout(this)
        overlayLayout.orientation = LinearLayout.VERTICAL
        overlayLayout.gravity = Gravity.CENTER
        overlayLayout.setBackgroundColor(Color.parseColor("#B91C1C"))

        val warningTitle = TextView(this)
        warningTitle.text = "🚨 EMERGENCY"
        warningTitle.textSize = 40f
        warningTitle.setTypeface(null, Typeface.BOLD)
        warningTitle.setTextColor(Color.WHITE)

        val warningMessage = TextView(this)
        warningMessage.text = """
            👷 Worker:
            $worker

            📍 Location:
            $location

            ⚠ $type
        """.trimIndent()
        warningMessage.textSize = 28f
        warningMessage.setTextColor(Color.WHITE)
        warningMessage.gravity = Gravity.CENTER

        val closeButton = TextView(this)
        closeButton.text = "CLEAR EMERGENCY"
        closeButton.textSize = 28f
        closeButton.setTypeface(null, Typeface.BOLD)
        closeButton.setTextColor(Color.WHITE)
        closeButton.gravity = Gravity.CENTER
        closeButton.setPadding(60, 35, 60, 35)
        closeButton.setBackgroundColor(Color.parseColor("#7F1D1D"))

        overlayLayout.addView(warningTitle)
        overlayLayout.addView(warningMessage)
        overlayLayout.addView(closeButton)

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            .setView(overlayLayout)
            .setCancelable(false)
            .create()

        dialog.show()

        dialog.window?.decorView?.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        dialog.window?.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )

        // Start siren audio using global manager setup
        startSosSiren()

        closeButton.setOnClickListener {
            closeButton.isEnabled = false
            closeButton.text = "CLEARING..."

            // Stop audio immediately
            stopSosSiren()
            dialog.dismiss()

            thread {
                try {
                    val url = URL("$PI_SERVER/clear_emergency")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = false
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000

                    val responseCode = conn.responseCode

                    runOnUiThread {
                        if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                            emergencyShowing = false
                        } else {
                            emergencyShowing = false
                            Toast.makeText(this@MainActivity, "Server failed to clear alert status", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        emergencyShowing = false
                        Toast.makeText(this@MainActivity, "Network error: Failed to clear status", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // =====================================
    // AUDIO CONTROLS
    // =====================================
    private fun startSosSiren() {
        try {
            stopSosSiren()
            mediaPlayer = MediaPlayer.create(this, R.raw.siren).apply {
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                isLooping = true
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopSosSiren() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
        }
    }

    // =====================================
    // LIFECYCLE HOOKS
    // =====================================
    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSosSiren()
    }

    // =====================================
    // CHIP
    // =====================================
    private fun createChip(text: String, color: String): TextView {
        val chip = TextView(this)
        chip.text = text
        chip.textSize = 13f
        chip.setTextColor(Color.WHITE)
        chip.setPadding(42, 22, 42, 22)

        val bg = GradientDrawable()
        bg.cornerRadius = 100f
        bg.setColor(Color.parseColor(color))
        chip.background = bg

        return chip
    }

    // =====================================
    // GLASS CARD
    // =====================================
    private fun createGlassCard(): CardView {
        val card = CardView(this)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 45, 0, 45)
        card.layoutParams = params
        card.radius = 60f
        card.cardElevation = 28f
        card.setCardBackgroundColor(Color.parseColor("#1E293B"))
        return card
    }

    // =====================================
    // DASHBOARD CARD
    // =====================================
    private fun createDashboardCard(titleText: String, subtitleText: String, colorHex: String): CardView {
        val card = CardView(this)
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 340)
        params.setMargins(0, 0, 0, 50)
        card.layoutParams = params
        card.radius = 60f
        card.cardElevation = 30f
        card.setCardBackgroundColor(Color.parseColor(colorHex))

        val innerLayout = LinearLayout(this)
        innerLayout.orientation = LinearLayout.VERTICAL
        innerLayout.gravity = Gravity.CENTER_VERTICAL
        innerLayout.setPadding(65, 60, 65, 60)

        val cardTitle = TextView(this)
        cardTitle.text = titleText
        cardTitle.textSize = 32f
        cardTitle.setTypeface(null, Typeface.BOLD)
        cardTitle.setTextColor(Color.WHITE)

        val cardSubtitle = TextView(this)
        cardSubtitle.text = subtitleText
        cardSubtitle.textSize = 18f
        cardSubtitle.setTextColor(Color.parseColor("#F1F5F9"))
        cardSubtitle.setPadding(0, 24, 0, 0)

        innerLayout.addView(cardTitle)
        innerLayout.addView(cardSubtitle)
        card.addView(innerLayout)

        card.setOnTouchListener { v: View, event: MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }
            }
            false
        }

        return card
    }

    private fun openWebPage(url: String) {
        val intent = Intent(this, WebViewActivity::class.java)
        intent.putExtra("URL", url)
        startActivity(intent)
    }
}