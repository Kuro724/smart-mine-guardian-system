#include <WiFi.h>
#include <HTTPClient.h>
#include <SPI.h>
#include <MFRC522.h>

// ============================================
// 📶 WIFI
// ============================================
const char* ssid = "Kiana";
const char* password = "Kiana Kaslana";

// ============================================
// 🌐 FLASK SERVER
// ============================================
const char* serverUrl =
"https://unfarcical-don-bilgiest.ngrok-free.dev/rfid";

// ============================================
// 📡 RFID PINS
// ============================================
#define SS_ENTRY 5
#define RST_ENTRY 22

#define SS_TUNNEL1 4
#define RST_TUNNEL1 15

// ============================================
// SPI PINS
// ============================================
#define SCK_PIN 18
#define MISO_PIN 19
#define MOSI_PIN 23

// ============================================
// RFID OBJECTS
// ============================================
MFRC522 rfidEntry(
    SS_ENTRY,
    RST_ENTRY
);

MFRC522 rfidTunnel1(
    SS_TUNNEL1,
    RST_TUNNEL1
);

// ============================================
// 🔔 OUTPUTS
// ============================================
#define GREEN_LED 2
#define RED_LED 16
#define BUZZER 17

// ============================================
// 🚀 SETUP
// ============================================
void setup() {

  Serial.begin(115200);

  // ============================================
  // SPI START
  // ============================================
  SPI.begin(
      SCK_PIN,
      MISO_PIN,
      MOSI_PIN
  );

  // ============================================
  // SS PINS
  // ============================================
  pinMode(SS_ENTRY, OUTPUT);

  pinMode(SS_TUNNEL1, OUTPUT);

  digitalWrite(SS_ENTRY, HIGH);

  digitalWrite(SS_TUNNEL1, HIGH);

  // ============================================
  // RFID INIT
  // ============================================
  rfidEntry.PCD_Init();

  delay(100);

  rfidTunnel1.PCD_Init();

  delay(100);

  // ============================================
  // OUTPUTS
  // ============================================
  pinMode(GREEN_LED, OUTPUT);

  pinMode(RED_LED, OUTPUT);

  pinMode(BUZZER, OUTPUT);

  digitalWrite(GREEN_LED, LOW);

  digitalWrite(RED_LED, LOW);

  digitalWrite(BUZZER, LOW);

  Serial.println("================================");

  Serial.println(
      "🛡 GUARDIAN RFID TRACKING READY"
  );

  Serial.println("================================");

  // ============================================
  // WIFI CONNECT
  // ============================================
  WiFi.begin(ssid, password);

  Serial.print("📶 Connecting WiFi");

  while (WiFi.status() != WL_CONNECTED) {

    delay(500);

    Serial.print(".");
  }

  Serial.println("\n✅ WIFI CONNECTED");

  Serial.print("📌 ESP32 IP: ");

  Serial.println(WiFi.localIP());
}

// ============================================
// ✅ ACCESS GRANTED
// ============================================
void accessGranted() {

  digitalWrite(GREEN_LED, HIGH);

  tone(BUZZER, 1200);

  delay(300);

  noTone(BUZZER);

  delay(200);

  tone(BUZZER, 1500);

  delay(300);

  noTone(BUZZER);

  delay(1000);

  digitalWrite(GREEN_LED, LOW);
}

// ============================================
// ❌ ACCESS DENIED
// ============================================
void accessDenied() {

  digitalWrite(RED_LED, HIGH);

  for(int i = 0; i < 3; i++) {

    tone(BUZZER, 500);

    delay(300);

    noTone(BUZZER);

    delay(200);
  }

  digitalWrite(RED_LED, LOW);
}

// ============================================
// 📤 SEND TO SERVER
// ============================================
void sendToServer(
    String uid,
    String location
) {

  if (WiFi.status() != WL_CONNECTED) {

    Serial.println("❌ WIFI LOST");

    return;
  }

  HTTPClient http;

  Serial.println(
      "🌐 Connecting Flask..."
  );

  if (!http.begin(serverUrl)) {

    Serial.println(
        "❌ HTTP BEGIN FAILED"
    );

    return;
  }

  http.addHeader(
      "Content-Type",
      "application/json"
  );

  // ============================================
  // JSON
  // ============================================
  String json =
      "{\"uid\":\"" + uid +
      "\",\"location\":\"" +
      location + "\"}";

  Serial.println("📤 Sending:");

  Serial.println(json);

  int code = http.POST(json);

  Serial.print("📡 HTTP CODE: ");

  Serial.println(code);

  // ============================================
  // SUCCESS
  // ============================================
  if (code > 0) {

    String response =
        http.getString();

    Serial.println("📥 RESPONSE:");

    Serial.println(response);

    // ============================================
    // ACCESS ALLOW
    // ============================================
    if (
        response.indexOf("ALLOW")
        >= 0
    ) {

      Serial.println(
          "✅ ACCESS APPROVED"
      );

      accessGranted();
    }

    // ============================================
    // ACCESS DENY
    // ============================================
    else {

      Serial.println(
          "❌ ACCESS DENIED"
      );

      accessDenied();
    }
  }

  // ============================================
  // SERVER FAIL
  // ============================================
  else {

    Serial.println(
        "❌ SERVER OFFLINE"
    );

    Serial.print("ERROR: ");

    Serial.println(code);
  }

  http.end();
}

// ============================================
// 🪪 RFID CHECK
// ============================================
void checkRFID(
    MFRC522 &rfid,
    String location
) {

  // ============================================
  // DISABLE BOTH
  // ============================================
  digitalWrite(SS_ENTRY, HIGH);

  digitalWrite(SS_TUNNEL1, HIGH);

  // ============================================
  // ENABLE CURRENT
  // ============================================
  if (location == "ENTRY") {

    digitalWrite(
        SS_ENTRY,
        LOW
    );
  }

  else if (location == "TUNNEL 1") {

    digitalWrite(
        SS_TUNNEL1,
        LOW
    );
  }

  // ============================================
  // CARD PRESENT?
  // ============================================
  if (!rfid.PICC_IsNewCardPresent())
    return;

  if (!rfid.PICC_ReadCardSerial())
    return;

  // ============================================
  // UID
  // ============================================
  String uid = "";

  for (
      byte i = 0;
      i < rfid.uid.size;
      i++
  ) {

    if (rfid.uid.uidByte[i] < 0x10)

      uid += "0";

    uid += String(
        rfid.uid.uidByte[i],
        HEX
    );

    if (i != rfid.uid.size - 1)

      uid += " ";
  }

  uid.toUpperCase();

  // ============================================
  // DISPLAY
  // ============================================
  Serial.println(
      "\n======================"
  );

  Serial.println(
      "🪪 CARD DETECTED"
  );

  Serial.println(
      "UID: " + uid
  );

  Serial.println(
      "📍 LOCATION: " + location
  );

  Serial.println(
      "======================"
  );

  // ============================================
  // SEND DATA
  // ============================================
  sendToServer(
      uid,
      location
  );

  // ============================================
  // STOP RFID
  // ============================================
  rfid.PICC_HaltA();

  rfid.PCD_StopCrypto1();

  // ============================================
  // DISABLE AGAIN
  // ============================================
  digitalWrite(SS_ENTRY, HIGH);

  digitalWrite(SS_TUNNEL1, HIGH);

  delay(3000);
}

// ============================================
// 🔁 LOOP
// ============================================
void loop() {

  // ============================================
  // 🚪 ENTRY RFID
  // PPE CHECK REQUIRED
  // ============================================
  checkRFID(
      rfidEntry,
      "ENTRY"
  );

  // ============================================
  // ⛏ TUNNEL 1 RFID
  // TRACK WORKER
  // ============================================
  checkRFID(
      rfidTunnel1,
      "TUNNEL 1"
  );
}