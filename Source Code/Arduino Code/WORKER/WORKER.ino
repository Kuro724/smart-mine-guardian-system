#include <SPI.h>
#include <LoRa.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

// =====================================
// WORKER & LOCATION INFO
// =====================================
#define WORKER_NAME "KIMIYETO"
#define NODE1_LOCATION "Entrance"
#define NODE2_LOCATION "TUNNEL 1"

// =====================================
// OLED DISPLAY
// =====================================
#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 32
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, -1);

// =====================================
// LoRa Pins
// =====================================
#define SS    5
#define RST   14
#define DIO0  2

// =====================================
// Outputs & Buttons
// =====================================
#define VIBRATION_PIN 25
#define BUZZER_PIN    26
#define SOS_BUTTON    27
#define ACK_BUTTON    33

// =====================================
// State Variables
// =====================================
bool alertActive = false;
String currentAlert = "";
unsigned long lastPulse = 0;
bool pulseState = false;

// Intercepted Sensor Data
float n1_mag  = 0.0;
int   n1_vib  = 0;
float n1_temp = 0.0; 
float n1_hum  = 0.0; 
float n1_co   = 0.0; 
float n1_ch4  = 0.0;

float n2_temp = 0.0;
float n2_hum  = 0.0;
float n2_co   = 0.0;
float n2_ch4  = 0.0;

// Dashboard Rotation Variables
int currentDashboard = 1; // 1=N1-STR, 2=N1-ENV, 3=N2-ENV
unsigned long lastDashboardSwitch = 0;
const int switchInterval = 4000; // 4 seconds per screen

// =====================================
// DISPLAY FUNCTIONS
// =====================================
void showMessage(String line1, String line2 = "")
{
  display.clearDisplay();
  display.setTextSize(2); 
  display.setTextColor(SSD1306_WHITE);
  display.setCursor(0, 0);
  display.println(line1);
  display.setCursor(0, 16);
  display.println(line2);
  display.display();
}

void updateDashboard()
{
  // 1. Gateway Emergency Alert Override
  if (alertActive) return;

  display.clearDisplay();
  display.setTextColor(SSD1306_WHITE);

  // 2. LOCAL VIBRATION OVERRIDE
  if (n1_vib == 1)
  {
    display.setTextSize(2); 
    display.setCursor(0, 0);
    display.print("VIBRATION!"); 
    display.setCursor(0, 16);
    display.setTextSize(1);
    display.print("EVACUATE: ");
    display.print(NODE1_LOCATION);
    display.display();
    return; // Stop drawing the normal dashboard
  }

  // 3. Normal Dashboard Rotation
  if (currentDashboard == 1)
  {
    // Screen 1: Node 1 Structure
    display.setTextSize(1);
    display.setCursor(0, 0);
    display.print("[ "); display.print(NODE1_LOCATION); display.print(" ] STR");
    
    display.setTextSize(2); // BIG TEXT
    display.setCursor(0, 16);
    display.print("MAG: "); display.print(n1_mag, 1);
  }
  else if (currentDashboard == 2)
  {
    // Screen 2: Node 1 Environment
    // Top Row: T:25 H:60
    display.setTextSize(1);
    display.setCursor(0, 0);
    display.print("["); display.print(NODE1_LOCATION); display.print("] T:"); 
    display.print((int)n1_temp); display.print(" H:"); display.print((int)n1_hum);
    
    // Bottom Row: C:10 CH4:0 (C = CO)
    display.setTextSize(2); // BIG TEXT
    display.setCursor(0, 16);
    display.print("C:"); display.print((int)n1_co); 
    display.print(" CH4:"); display.print((int)n1_ch4);
  }
  else if (currentDashboard == 3)
  {
    // Screen 3: Node 2 Environment
    // Top Row: T:25 H:60
    display.setTextSize(1);
    display.setCursor(0, 0);
    display.print("["); display.print(NODE2_LOCATION); display.print("] T:"); 
    display.print((int)n2_temp); display.print(" H:"); display.print((int)n2_hum);
    
    // Bottom Row: C:10 CH4:0
    display.setTextSize(2); // BIG TEXT
    display.setCursor(0, 16);
    display.print("C:"); display.print((int)n2_co); 
    display.print(" CH4:"); display.print((int)n2_ch4);
  }

  display.display();
}

// =====================================
// SETUP
// =====================================
void setup()
{
  Serial.begin(115200);
  Wire.begin(21, 22);

  if (!display.begin(SSD1306_SWITCHCAPVCC, 0x3C))
  {
    Serial.println("OLED Failed");
    while (true);
  }

  showMessage(" MINE", " GUARDIAN");

  pinMode(VIBRATION_PIN, OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);
  digitalWrite(VIBRATION_PIN, LOW);
  digitalWrite(BUZZER_PIN, LOW);
  pinMode(SOS_BUTTON, INPUT_PULLUP);
  pinMode(ACK_BUTTON, INPUT_PULLUP);

  SPI.begin(18, 19, 23, SS);
  LoRa.setPins(SS, RST, DIO0);

  if (!LoRa.begin(433E6)) {
    showMessage("LoRa", "Failed");
    while (true);
  }

  LoRa.setSpreadingFactor(7);
  LoRa.setSignalBandwidth(125E3);
  LoRa.setCodingRate4(5);
  LoRa.setSyncWord(0x12);
  LoRa.receive();

  delay(2000);
  updateDashboard();
}

// =====================================
// LOOP
// =====================================
void loop()
{
  // =====================================
  // DASHBOARD ROTATION TIMER
  // =====================================
  if (!alertActive && n1_vib == 0) // Only rotate if no emergencies
  {
    if (millis() - lastDashboardSwitch > switchInterval)
    {
      lastDashboardSwitch = millis();
      currentDashboard++;
      if (currentDashboard > 3) currentDashboard = 1;
      updateDashboard();
    }
  }

  // =====================================
  // RECEIVE ALERTS & TELEMETRY
  // =====================================
  int packetSize = LoRa.parsePacket();

  if (packetSize)
  {
    String msg = "";
    while (LoRa.available()) { msg += (char)LoRa.read(); }
    msg.trim();
    Serial.println("RX: " + msg);

    // 1. IS IT AN ALERT?
    if (msg.startsWith("ALERT:"))
    {
      currentAlert = msg;
      currentAlert.replace("ALERT:", "");
      alertActive = true;

      display.clearDisplay();
      display.setTextSize(2);
      display.setCursor(0, 0);
      display.println("EMERGENCY"); 
      display.setTextSize(1); 
      display.setCursor(0, 16);
      if (currentAlert.length() > 21) display.println(currentAlert.substring(0, 21));
      else display.println(currentAlert);
      display.display();
    }
    // 2. IS IT NODE 1 (STRUCTURAL)?
    else if (msg.startsWith("1,"))
    {
      int c[10]; int count = 0;
      for (int i = 0; i < msg.length(); i++) {
        if (msg.charAt(i) == ',') { c[count] = i; count++; if (count >= 10) break; }
      }
      if (count >= 9) {
        n1_mag  = msg.substring(c[3] + 1, c[4]).toFloat(); 
        n1_vib  = msg.substring(c[5] + 1, c[6]).toInt();   
        n1_temp = msg.substring(c[6] + 1, c[7]).toFloat(); 
        n1_hum  = msg.substring(c[7] + 1, c[8]).toFloat(); 
        n1_co   = msg.substring(c[8] + 1, c[9]).toFloat(); 
        n1_ch4  = msg.substring(c[9] + 1).toFloat(); // Reads from 10th comma to end
        
        // VIBRATION JOLT
        if (n1_vib == 1) {
          digitalWrite(BUZZER_PIN, HIGH);
          digitalWrite(VIBRATION_PIN, HIGH);
          delay(300); // 300ms jolt
          digitalWrite(BUZZER_PIN, LOW);
          digitalWrite(VIBRATION_PIN, LOW);
        }
        updateDashboard(); 
      }
    }
    // 3. IS IT NODE 2 (ENVIRONMENT)?
    else if (msg.startsWith("2,"))
    {
      int c[4]; int count = 0;
      for (int i = 0; i < msg.length(); i++) {
        if (msg.charAt(i) == ',') { c[count] = i; count++; if (count >= 4) break; }
      }
      if (count >= 3) {
        n2_temp = msg.substring(c[0] + 1, c[1]).toFloat();
        n2_hum  = msg.substring(c[1] + 1, c[2]).toFloat();
        n2_co   = msg.substring(c[2] + 1, c[3]).toFloat();
        n2_ch4  = msg.substring(c[3] + 1).toFloat(); // Reads from 4th comma to end
        
        updateDashboard();
      }
    }
  }

  // =====================================
  // ALARM PULSES
  // =====================================
  if (alertActive) {
    if (millis() - lastPulse > 500) {
      lastPulse = millis();
      pulseState = !pulseState;
      digitalWrite(BUZZER_PIN, pulseState);
      digitalWrite(VIBRATION_PIN, pulseState);
    }
  } else if (n1_vib == 0) {
    digitalWrite(BUZZER_PIN, LOW);
    digitalWrite(VIBRATION_PIN, LOW);
  }

  // =====================================
  // ACK BUTTON
  // =====================================
  if (digitalRead(ACK_BUTTON) == LOW)
  {
    alertActive = false;
    digitalWrite(BUZZER_PIN, LOW);
    digitalWrite(VIBRATION_PIN, LOW);
    showMessage("  ALERT", " CLEARED");
    LoRa.idle();
    LoRa.beginPacket(); LoRa.print(String("ACK:") + WORKER_NAME); LoRa.endPacket();
    LoRa.receive();
    delay(1000);
    lastDashboardSwitch = millis(); 
    updateDashboard();
    while (digitalRead(ACK_BUTTON) == LOW) delay(10);
  }

  // =====================================
  // SOS BUTTON
  // =====================================
  if (digitalRead(SOS_BUTTON) == LOW)
  {
    showMessage("   SOS", "   SENT");
    String sosMessage = String("SOS:") + WORKER_NAME;
    LoRa.idle();
    LoRa.beginPacket(); LoRa.print(sosMessage); LoRa.endPacket();
    LoRa.receive();
    digitalWrite(BUZZER_PIN, HIGH); delay(200); digitalWrite(BUZZER_PIN, LOW);
    while (digitalRead(SOS_BUTTON) == LOW) delay(10);
    delay(1000);
    if (alertActive) {
      display.clearDisplay(); display.setTextSize(2); display.setCursor(0, 0);
      display.println("EMERGENCY"); display.setTextSize(1); display.setCursor(0, 16);
      display.println(currentAlert); display.display();
    } else {
      lastDashboardSwitch = millis(); 
      updateDashboard();
    }
  }
}