# Edge-Enabled Smart Mine Guardian System 🛡️

> An edge-enabled **Cyber-Physical System (CPS)** for IIoT-based underground mine safety — real-time environmental monitoring, AI hazard detection, multi-camera PPE compliance, LoRa worker alerts, and a supervisor mobile app.

Developed under the **TEXMiN Mining CPS Centre of Excellence** fellowship at **NIT Nagaland** (Project Ref: `TEXMiN-CoE/UG-PG-Fellowship/2025/09-005`). Lab prototype validated at **TRL-5**.

## Overview

The system fuses four capabilities into one low-latency, edge-first pipeline:

1. **Environmental AI** — Isolation Forest / Random Forest models on Raspberry Pi detect gas (CO, CH₄), temperature, humidity, and structural-vibration anomalies **without cloud dependency**.
2. **PPE computer vision** — A YOLOv5 model runs on two live cameras (entrance + tunnel) to detect helmet/vest compliance; RFID then grants or denies access.
3. **LoRa worker devices** — Battery-powered ESP32 + SX1278 wearables show telemetry on an OLED, trigger buzzer + haptic alerts, and carry an **SOS button** for bidirectional emergency comms.
4. **Supervisor app** — A Kotlin/Android dashboard (plus Flask web UI) for live telemetry, worker tracking, AI warnings (human-in-the-loop), and one-tap evacuation.

## System architecture

```
 ESP32 sensor nodes (MQ7/MQ4/DHT22/MPU6050/SW420)
            │  LoRa 433 MHz (SX1278)
            ▼
 Raspberry Pi Gateway ──► Flask Edge AI Server (port 5001)
   (LoRa RX + joblib models)        │
            │                       │ HTTP/ngrok
            ▼                       ▼
 ESP32 Worker Wearable      Windows YOLO/RFID Server (port 5000)
   (OLED + buzzer +               │
    haptic + SOS button)          ▼
                          Android Supervisor App (Kotlin)
```

## Repository layout

```
smart-mine-guardian-system/
├── GuardianApp/                      # Android supervisor app (Kotlin)
│   └── app/src/main/java/com/example/guardianapp/
│       ├── MainActivity.kt           # Dashboard: worker status, PPE, sensors, SOS, evacuate
│       ├── TunnelMapActivity.kt      # Live tunnel map, AI risk, exposure alerts
│       ├── WorkerTrackingActivity.kt # RFID worker locations + last-seen
│       ├── LoginActivity.kt · SplashActivity.kt · WebViewActivity.kt
│       └── res/                      # UI, drawables, siren.mp3
│
├── edge-server/                      # Raspberry Pi Edge AI server
│   ├── pi_server.py                  # Flask server (port 5001): hazard rules, AI warnings,
│   │                                 #   exposure tracking, SOS/ACK, supervisor commands
│   ├── lora_gateway.py               # SX127x receiver/transmitter, telemetry + SOS + ACK
│   ├── building_health_model.pkl     # Random Forest structural model
│   ├── *.pkl                         # CO / methane (Isolation Forest) models + scaler
│   └── guardian_history.db           # SQLite telemetry log (auto-created)
│
├── yolo-server/                      # Windows PPE/RFID server
│   ├── yolo_server.py                # Flask (port 5000): dual-camera YOLO inference,
│   │                                 #   RFID access control, worker tracking
│   ├── windows_listener.py           # Remote start/stop of YOLO + ngrok (port 9001)
│   ├── best.pt                       # Trained YOLOv5 PPE weights
│   └── yolov5/                       # YOLOv5 source (local torch hub load)
│
├── firmware/                         # Arduino / ESP32 sketches
│   ├── WORKER/WORKER.ino             # Worker wearable: OLED, buzzer, haptic, SOS, ACK
│   ├── node1vib/node1vib.ino         # Structural node (MPU6050 + SW420 + env sensors)
│   ├── node2withoutvib/              # Environment-only node
│   └── rfidyolo/rfidyolo.ino         # RFID reader → YOLO server
│
└── docs/
    ├── Project_Closure_Report.pdf    # Full 19-page project report (TEXMiN CoE)
    ├── Project_Guardian_Arduino_Manual.docx
    └── Project_Guardian_Lab_Manual.docx
```

## Requirements

**Raspberry Pi (Edge AI + LoRa gateway):** Python 3.10+, Flask, joblib, scikit-learn, requests, sqlite3, `SX127x` LoRa library, Raspberry Pi GPIO.

**Windows (YOLO/RFID server):** Python 3.10+, Flask, PyTorch, OpenCV, pandas, YOLOv5 (local clone), a CUDA-capable GPU recommended.

**Android app:** Android Studio, Kotlin 2.0, minSdk 26 / targetSdk 35, JDK 11.

**Firmware:** Arduino IDE with `LoRa`, `Adafruit_SSD1306`, `Adafruit_GFX`, `DHT` sensor libraries; ESP32 boards package.

## How to run

> The system runs as **three cooperating servers** plus the Android app. Start them in this order. Every server exposes itself over [ngrok](https://ngrok.com/) so the phone can reach it off-LAN — copy the new ngrok URLs into the app's `MainActivity.kt` / `TunnelMapActivity.kt` each session.

### 1 · Raspberry Pi edge server (port 5001)

```bash
cd edge-server
pip install flask joblib scikit-learn requests
# LoRa gateway runs in parallel in its own terminal
python lora_gateway.py   # receives telemetry + SOS/ACK over SX1278
python pi_server.py      # Flask on 0.0.0.0:5001
```
Endpoints used: `/predict` (sensor → AI verdict), `/data`, `/risk`, `/map`, `/emergency`, `/receive-sos`, `/receive-ack`, `/send-alert`, `/elevate_warning`, `/dismiss_warning`, `/clear_emergency`.

### 2 · Windows YOLO + RFID server (port 5000)

```bash
cd yolo-server
pip install flask torch opencv-python pandas
# best.pt + a local yolov5/ folder must be present
python yolo_server.py    # opens 2 cameras, serves 0.0.0.0:5000
python windows_listener.py   # optional: remote start/stop + ngrok launcher (port 9001)
```
Endpoints: `/rfid` (UID → access decision), `/status`, `/workers`, `/risk`, `/map`.

### 3 · Expose both servers with ngrok

```bash
ngrok http 5000   # YOLO/RFID server → copy the https URL
ngrok http 5001   # Pi edge server      → copy the https URL
```

### 4 · Android supervisor app

1. Open `GuardianApp/` in Android Studio and run on a device (minSdk 26).
2. Edit the two constants at the top of `app/src/main/java/com/example/guardianapp/MainActivity.kt` (and `TunnelMapActivity.kt`):

   ```kotlin
   private val YOLO_SERVER = "https://<your-yolo-ngrok-url>.ngrok-free.dev"
   private val PI_SERVER   = "https://<your-pi-ngrok-url>.ngrok-free.dev"
   ```
3. Launch the app → select a worker + tunnel → the dashboard polls `/status` and `/emergency` every second.

### 5 · ESP32 firmware

Flash each board from `firmware/`:

- **`node1vib`** → structural node (entrance): MPU6050 + SW420 + DHT22 + MQ7 + MQ4, LoRa @ 433 MHz.
- **`node2withoutvib`** → environment node (tunnel 1): DHT22 + MQ7 + MQ4.
- **`WORKER`** → worker wearable: set `WORKER_NAME`, pins are OLED(21/22), LoRa(18/19/23/5/14/2), buzzer 26, haptic 25, SOS 27, ACK 33.
- **`rfidyolo`** → RFID reader posting UIDs to the YOLO server.

LoRa radio config must match on gateway and all nodes: `433 MHz`, SF7, BW 125 kHz, CR 4/5, sync word `0x12`.

## AI models

| Model | Task | Algorithm | Inputs |
| --- | --- | --- | --- |
| `building_health_model.pkl` | Structural health | Random Forest | Accel X/Y/Z, strain, temp, rolling stats |
| `co_rf_model.pkl` | CO hazard | Random Forest | CO concentration |
| `isolation_forest_ch4_temp.pkl` | Methane anomaly | Isolation Forest | CH₄ + temperature |

Training notebooks/scripts for each are in the `Source Code/*.txt` guides — re-train in Google Colab and drop the `.pkl` files into `edge-server/`.

## Key features

- ⚡ **Edge-first** — hazard verdicts returned locally; no cloud round-trip.
- 🧠 **Human-in-the-loop** — AI raises a *warning* the supervisor must elevate to an evacuation before the LoRa alert fires.
- 📊 **Exposure tracking** — per-worker time-in-hazard with 20-second critical-alert threshold.
- 🔁 **Bidirectional SOS** — worker SOS → gateway → dashboard; supervisor evacuate → gateway → wearable.
- 🎥 **Dual-camera PPE** — entrance gate *access control* + tunnel-zone *violation flagging*.

## Status & roadmap

- ✅ Prototype validated in the lab (TRL-5). Manuscript in preparation.
- 🔜 Field testing under simulated mine conditions, low-light/dust robustness, predictive analytics, patent filing.

## License

Released for academic and research use. The PPE-detection model builds on the [Eagle-eyes](https://github.com/83Gh0st/Eagle-eyes) project (MIT) — see the separate `eagle-eyes-ppe-detection` repo.

## Acknowledgements

Mentored by **Dr. Daijiry Narzary**, with support from **Dr. M. Prakash** (HoD, EIE), **Dr. P. Chinnamuthu** and **Mr. Karthikeyan** (TEXMiN CPS CoE), NIT Nagaland.
