package com.example.guardianapp

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import kotlin.concurrent.thread

class TunnelMapActivity : AppCompatActivity() {

    private lateinit var tunnelContainer: LinearLayout
    private lateinit var riskContainer: LinearLayout
    private lateinit var sensorContainer: LinearLayout
    private lateinit var exposureContainer: LinearLayout

    // 🌐 NGROK URLS
    private val PI_NGROK_URL = "https://clause-cold-condone.ngrok-free.dev"
    private val WINDOWS_NGROK_URL = "https://unfarcical-don-bilgiest.ngrok-free.dev"

    // 📡 LOCAL IP ADDRESSES
    private val PI_LOCAL_BASE_URL = "http://10.108.167.166:9000"
    private val WINDOWS_LOCAL_BASE_URL = "http://10.108.167.154:9001"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this)

        val mainLayout = LinearLayout(this)
        mainLayout.orientation = LinearLayout.VERTICAL
        mainLayout.setPadding(40, 60, 40, 40)

        val bg = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.parseColor("#020617"),
                Color.parseColor("#0F172A"),
                Color.parseColor("#111827")
            )
        )
        mainLayout.background = bg

        // ── MAIN TITLE ──
        val title = TextView(this)
        title.text = "🗺 LIVE TUNNEL MAP"
        title.textSize = 32f
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER
        title.setPadding(0, 0, 0, 40)
        mainLayout.addView(title)

        // ── SERVER CONTROLS SECTION ──
        mainLayout.addView(makeSectionTitle("⚙ SERVER CONTROLS", "#FFB300", 20))

        // --- PI CONTROLS ROW ---
        val piButtonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }

        val startPiButton = Button(this).apply {
            text = "START PI"
            setOnClickListener { sendStartCommand("$PI_LOCAL_BASE_URL/start-sensors") }
        }

        val stopPiButton = Button(this).apply {
            text = "STOP PI"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(20, 0, 0, 0)
            }
            setOnClickListener { sendStartCommand("$PI_LOCAL_BASE_URL/stop-sensors") }
        }

        piButtonContainer.addView(startPiButton)
        piButtonContainer.addView(stopPiButton)
        mainLayout.addView(piButtonContainer)

        // --- YOLO CONTROLS ROW ---
        val yoloButtonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }

        val startYoloButton = Button(this).apply {
            text = "START YOLO"
            setOnClickListener { sendStartCommand("$WINDOWS_LOCAL_BASE_URL/start-yolo") }
        }

        val stopYoloButton = Button(this).apply {
            text = "STOP YOLO"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(20, 0, 0, 0)
            }
            setOnClickListener { sendStartCommand("$WINDOWS_LOCAL_BASE_URL/stop-yolo") }
        }

        yoloButtonContainer.addView(startYoloButton)
        yoloButtonContainer.addView(stopYoloButton)
        mainLayout.addView(yoloButtonContainer)

        // ── SENSOR SECTION ──
        mainLayout.addView(makeSectionTitle("🌡 LIVE SENSOR NODE", "#38BDF8", 10))
        sensorContainer = LinearLayout(this)
        sensorContainer.orientation = LinearLayout.VERTICAL
        mainLayout.addView(sensorContainer)

        // ── EXPOSURE SECTION ──
        mainLayout.addView(makeSectionTitle("🚨 EXPOSURE ALERTS", "#EF4444", 40))
        exposureContainer = LinearLayout(this)
        exposureContainer.orientation = LinearLayout.VERTICAL
        mainLayout.addView(exposureContainer)

        // ── RISK SECTION ──
        mainLayout.addView(makeSectionTitle("🧠 AI RISK PREDICTION", "#F87171", 40))
        riskContainer = LinearLayout(this)
        riskContainer.orientation = LinearLayout.VERTICAL
        mainLayout.addView(riskContainer)

        // ── TUNNEL SECTION ──
        mainLayout.addView(makeSectionTitle("👷 TUNNEL STATUS", "#FFFFFF", 40))
        tunnelContainer = LinearLayout(this)
        tunnelContainer.orientation = LinearLayout.VERTICAL
        mainLayout.addView(tunnelContainer)

        scrollView.addView(mainLayout)
        setContentView(scrollView)

        startTunnelUpdates()
        startRiskUpdates()
        startSensorUpdates()
        startExposureUpdates()
    }

    // ── Section title helper ──
    private fun makeSectionTitle(text: String, colorHex: String, topPad: Int): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 24f
        tv.setTypeface(null, Typeface.BOLD)
        tv.setTextColor(Color.parseColor(colorHex))
        tv.setPadding(0, topPad, 0, 25)
        return tv
    }

    // ── Loops ──
    private fun startExposureUpdates() = loopEvery(3000) { fetchExposureAlerts() }
    private fun startSensorUpdates()   = loopEvery(3000) { fetchSensorData() }
    private fun startTunnelUpdates()   = loopEvery(3000) { fetchTunnelMap() }
    private fun startRiskUpdates()     = loopEvery(4000) { fetchRiskData() }

    private fun loopEvery(ms: Long, action: () -> Unit) {
        Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
            override fun run() {
                action()
                Handler(Looper.getMainLooper()).postDelayed(this, ms)
            }
        }, 1000)
    }

    // ── Fetch Exposure Alerts (PI) ──
    private fun fetchExposureAlerts() {
        thread {
            try {
                val obj    = JSONObject(URL("$PI_NGROK_URL/exposure_alerts").readText())
                val alerts = obj.getJSONArray("alerts")
                runOnUiThread {
                    exposureContainer.removeAllViews()
                    if (alerts.length() == 0) {
                        exposureContainer.addView(createGreenCard("✅ NO ACTIVE EXPOSURE ALERTS"))
                    } else {
                        for (i in 0 until alerts.length()) {
                            exposureContainer.addView(createExposureCard(alerts.getJSONObject(i)))
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // ── Fetch Sensor Data (PI) ──
    private fun fetchSensorData() {
        thread {
            try {
                val obj  = JSONObject(URL("$PI_NGROK_URL/data").readText())
                val keys = obj.keys()
                runOnUiThread {
                    sensorContainer.removeAllViews()
                    while (keys.hasNext()) {
                        val tunnelName = keys.next()
                        val sensor     = obj.getJSONObject(tunnelName)
                        sensorContainer.addView(
                            createSensorCard(
                                tunnelName,
                                sensor.getString("temperature"),
                                sensor.getString("co"),
                                sensor.getString("ch4"),
                                sensor.getString("humidity"),
                                sensor.getString("status"),
                                sensor.getString("advice")
                            )
                        )
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // ── Fetch Tunnel Map (WINDOWS) ──
    private fun fetchTunnelMap() {
        thread {
            try {
                val obj  = JSONObject(URL("$WINDOWS_NGROK_URL/map").readText())
                val keys = obj.keys()
                runOnUiThread {
                    tunnelContainer.removeAllViews()
                    while (keys.hasNext()) {
                        val tunnelName = keys.next()
                        tunnelContainer.addView(
                            createTunnelCard(tunnelName, obj.getJSONArray(tunnelName))
                        )
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // ── Fetch Risk (WINDOWS) ──
    private fun fetchRiskData() {
        thread {
            try {
                val obj   = JSONObject(URL("$WINDOWS_NGROK_URL/risk").readText())
                val risks = obj.getJSONArray("risks")
                runOnUiThread {
                    riskContainer.removeAllViews()
                    if (risks.length() == 0) {
                        riskContainer.addView(createGreenCard("✅ NO ACTIVE RISKS"))
                    } else {
                        for (i in 0 until risks.length()) {
                            riskContainer.addView(createRiskCard(risks.getJSONObject(i)))
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // ── Card builders ──

    private fun createGreenCard(msg: String): TextView {
        val tv = TextView(this)
        tv.text = msg
        tv.textSize = 20f
        tv.setTypeface(null, Typeface.BOLD)
        tv.setTextColor(Color.WHITE)
        tv.setPadding(40, 40, 40, 40)
        val bg = GradientDrawable()
        bg.cornerRadius = 40f
        bg.setColor(Color.parseColor("#16A34A"))
        tv.background = bg
        return tv
    }

    private fun createExposureCard(alert: JSONObject): LinearLayout {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(45, 45, 45, 45)
        val bg = GradientDrawable()
        bg.cornerRadius = 45f
        bg.setColor(Color.parseColor("#7F1D1D"))
        bg.setStroke(3, Color.parseColor("#EF4444"))
        card.background = bg

        val text = TextView(this)
        text.text = """
🚨 ${alert.getString("worker")}

☠ HAZARD:
${alert.getString("hazard")}

📍 LOCATION:
${alert.getString("location")}

⏱ EXPOSURE:
${alert.getInt("exposure_time")} sec

⚠ ${alert.getString("message")}
        """.trimIndent()
        text.textSize = 20f
        text.setTypeface(null, Typeface.BOLD)
        text.setTextColor(Color.WHITE)
        card.addView(text)

        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(0, 0, 0, 30)
        card.layoutParams = lp
        return card
    }

    private fun createSensorCard(
        tunnelName: String, temp: String, co: String,
        ch4: String, humidity: String, status: String, advice: String
    ): LinearLayout {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(45, 45, 45, 45)
        val bg = GradientDrawable()
        bg.cornerRadius = 45f
        bg.setColor(Color.parseColor("#0F172A"))
        bg.setStroke(3, Color.parseColor("#38BDF8"))
        card.background = bg

        val text = TextView(this)
        text.text = """
📍 $tunnelName

🌡 TEMPERATURE: $temp °C
☠ CO LEVEL:    $co ppm
🔥 CH4 LEVEL:  $ch4 ppm
💧 HUMIDITY:   $humidity %

━━━━━━━━━━━━━━━━━━
⚠ STATUS: $status
🧠 AI ADVICE: $advice
        """.trimIndent()
        text.textSize = 20f
        text.setTypeface(null, Typeface.BOLD)
        text.setTextColor(Color.WHITE)
        card.addView(text)

        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(0, 0, 0, 30)
        card.layoutParams = lp
        return card
    }

    private fun createRiskCard(risk: JSONObject): TextView {
        val tv = TextView(this)
        tv.text = "⚠ ${risk.getString("message")}"
        tv.textSize = 19f
        tv.setTypeface(null, Typeface.BOLD)
        tv.setTextColor(Color.WHITE)
        tv.setPadding(40, 40, 40, 40)
        val bg = GradientDrawable()
        bg.cornerRadius = 40f
        bg.setColor(Color.parseColor("#DC2626"))
        tv.background = bg
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(0, 0, 0, 20)
        tv.layoutParams = lp
        return tv
    }

    private fun createTunnelCard(tunnelName: String, workers: JSONArray): LinearLayout {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(45, 45, 45, 45)
        val bg = GradientDrawable()
        bg.cornerRadius = 45f
        bg.setColor(Color.parseColor("#1E293B"))
        bg.setStroke(3, Color.parseColor("#38BDF8"))
        card.background = bg

        val titleTv = TextView(this)
        titleTv.text = "📍 $tunnelName"
        titleTv.textSize = 24f
        titleTv.setTypeface(null, Typeface.BOLD)
        titleTv.setTextColor(Color.WHITE)
        card.addView(titleTv)

        if (workers.length() == 0) {
            val empty = TextView(this)
            empty.text = "⚪ NO WORKERS"
            empty.textSize = 18f
            empty.setTextColor(Color.parseColor("#CBD5E1"))
            empty.setPadding(0, 25, 0, 0)
            card.addView(empty)
        } else {
            for (i in 0 until workers.length()) {
                val worker = workers.getJSONObject(i)
                val wt = TextView(this)
                wt.text = "🟢 ${worker.getString("worker")}  🕒 ${worker.getString("last_seen")}"
                wt.textSize = 18f
                wt.setTextColor(Color.WHITE)
                wt.setPadding(0, 25, 0, 0)
                card.addView(wt)
            }
        }

        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(0, 0, 0, 30)
        card.layoutParams = lp
        return card
    }

    // ── Trigger Server Startup ──
    private fun sendStartCommand(urlStr: String) {
        thread {
            try {
                // This triggers the Python Flask listener
                val response = URL(urlStr).readText()
                runOnUiThread {
                    Toast.makeText(this@TunnelMapActivity, response, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(
                        this@TunnelMapActivity,
                        "Failed to connect. Check if devices are on the same Wi-Fi and IPs are correct.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}