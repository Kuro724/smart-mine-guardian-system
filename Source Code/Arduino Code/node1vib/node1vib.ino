#include <Wire.h>
#include <SPI.h>
#include <LoRa.h>
#include <DHT.h>

// =====================================================
// NODE ID
// =====================================================
#define NODE_ID 1

// =====================================================
// MPU6050
// =====================================================
const int MPU = 0x68;
#define SDA_PIN 25
#define SCL_PIN 26

float AcX, AcY, AcZ;
float vibrationMagnitude;
float accelHistory[5];
int historyIndex = 0;
float rollingMean = 0;

// =====================================================
// SW420 (VIBRATION INTERRUPT)
// =====================================================
#define SW420_PIN 27

// This variable changes in the background the exact millisecond a vibration happens
volatile bool vibrationDetectedFlag = false;

// The Interrupt Service Routine (Runs in the background)
void IRAM_ATTR detectVibration() {
    vibrationDetectedFlag = true;
}

// =====================================================
// DHT & MQ SENSORS
// =====================================================
#define DHTPIN   21
#define DHTTYPE  DHT22
DHT dht(DHTPIN, DHTTYPE);

#define MQ7_PIN   34
#define MQ4_PIN   32

// =====================================================
// LORA PINS
// =====================================================
#define SS    5
#define RST   14
#define DIO0  2

// =====================================================
// NON-BLOCKING TIMER
// =====================================================
unsigned long previousMillis = 0;
const long interval = 5000; // Send packet every 5 seconds

void setup() {
    Serial.begin(115200);
    delay(1000);

    Serial.println("=================================");
    Serial.println("SMART STRUCTURAL HEALTH NODE");
    Serial.println("=================================");

    dht.begin();

    // Setup SW420 as an interrupt to catch split-second vibrations
    pinMode(SW420_PIN, INPUT);
    attachInterrupt(digitalPinToInterrupt(SW420_PIN), detectVibration, RISING);

    // Setup MPU6050 with a Timeout so it doesn't freeze the ESP32!
    Wire.begin(SDA_PIN, SCL_PIN);
    Wire.setTimeOut(1000); // 1-second timeout prevents infinite hanging
    Wire.beginTransmission(MPU);
    Wire.write(0x6B);
    Wire.write(0);
    byte error = Wire.endTransmission(true);

    if (error == 0) {
        Serial.println("MPU6050 Connected!");
    } else {
        Serial.println("MPU6050 Connection Failed! Check Wiring.");
    }

    // Setup LoRa
    SPI.begin(18, 19, 23, SS);
    LoRa.setPins(SS, RST, DIO0);

    if (!LoRa.begin(433E6)) {
        Serial.println("LoRa Init Failed");
        while (1);
    }

    LoRa.setTxPower(17);
    LoRa.setSpreadingFactor(7);
    LoRa.setSignalBandwidth(125E3);
    LoRa.setCodingRate4(5);
    LoRa.setSyncWord(0x12);

    Serial.println("LoRa Ready!");
}

void loop() {
    unsigned long currentMillis = millis();

    // Check if 5 seconds have passed (NON-BLOCKING)
    if (currentMillis - previousMillis >= interval) {
        previousMillis = currentMillis;

        // =================================================
        // READ MPU6050 safely
        // =================================================
        Wire.beginTransmission(MPU);
        Wire.write(0x3B);
        Wire.endTransmission(false);
        
        // Only read if the MPU actually responds with 6 bytes
        if (Wire.requestFrom(MPU, 6, true) == 6) {
            AcX = (Wire.read() << 8 | Wire.read()) / 16384.0 * 9.81;
            AcY = (Wire.read() << 8 | Wire.read()) / 16384.0 * 9.81;
            AcZ = (Wire.read() << 8 | Wire.read()) / 16384.0 * 9.81;
        } else {
            Serial.println("Warning: MPU6050 Read Error!");
        }

        vibrationMagnitude = sqrt((AcX * AcX) + (AcY * AcY) + (AcZ * AcZ));

        // Rolling Mean
        accelHistory[historyIndex] = vibrationMagnitude;
        historyIndex++;
        if (historyIndex >= 5) historyIndex = 0;

        rollingMean = 0;
        for (int i = 0; i < 5; i++) {
            rollingMean += accelHistory[i];
        }
        rollingMean /= 5.0;

        // =================================================
        // READ SENSORS
        // =================================================
        float temp = dht.readTemperature();
        float humidity = dht.readHumidity();
        float co = analogRead(MQ7_PIN) / 100.0;
        float ch4 = analogRead(MQ4_PIN) / 100.0;

        // Check our background flag to see if a vibration happened in the last 5 secs
        int finalVibrationStatus = 0;
        if (vibrationDetectedFlag) {
            finalVibrationStatus = 1;
            vibrationDetectedFlag = false; // Reset the flag for the next 5 seconds
        }

        // =================================================
        // SERIAL OUTPUT
        // =================================================
        Serial.println("=================================");
        Serial.print("Accel X: "); Serial.println(AcX);
        Serial.print("Accel Y: "); Serial.println(AcY);
        Serial.print("Accel Z: "); Serial.println(AcZ);
        Serial.print("Magnitude: "); Serial.println(vibrationMagnitude);
        Serial.print("Rolling Mean: "); Serial.println(rollingMean);
        Serial.print("SW420 (Latched): "); Serial.println(finalVibrationStatus);
        Serial.print("Temperature: "); Serial.println(temp);
        Serial.print("Humidity: "); Serial.println(humidity);
        Serial.print("CO: "); Serial.println(co);
        Serial.print("CH4: "); Serial.println(ch4);

        // =================================================
        // CREATE PACKET
        // =================================================
        char packet[200];
        snprintf(
            packet,
            sizeof(packet),
            "%d,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%.1f,%.1f,%.1f,%.1f",
            NODE_ID,
            AcX, AcY, AcZ,
            vibrationMagnitude,
            rollingMean,
            finalVibrationStatus,
            temp,
            humidity,
            co,
            ch4
        );

        // =================================================
        // SEND LORA
        // =================================================
        Serial.print("Sending Packet: ");
        Serial.println(packet);

        LoRa.beginPacket();
        LoRa.print(packet);
        LoRa.endPacket();

        Serial.println("LoRa TX Success");
    }
}