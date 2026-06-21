#include <SPI.h>
#include <LoRa.h>
#include <DHT.h>

// ============================================
// NODE ID
// ============================================
#define NODE_ID 2

// ============================================
// DHT22
// ============================================
#define DHTPIN 21
#define DHTTYPE DHT22

DHT dht(DHTPIN, DHTTYPE);

// ============================================
// MQ PINS
// ============================================
#define MQ7_PIN   34
#define MQ4_PIN   32

// ============================================
// LORA PINS
// ============================================
#define SS    5
#define RST   14
#define DIO0  2

void setup()
{
    Serial.begin(9600);

    dht.begin();

    // SPI
    SPI.begin(18, 19, 23, SS);

    // LORA PINS
    LoRa.setPins(SS, RST, DIO0);

    // START LORA
    if (!LoRa.begin(433E6))
    {
        Serial.println("LoRa Init Failed");

        while (1);
    }

    // STABLE SETTINGS
    LoRa.setTxPower(17);

    LoRa.setSpreadingFactor(7);

    LoRa.setSignalBandwidth(125E3);

    LoRa.setCodingRate4(5);

    LoRa.setSyncWord(0x12);

    Serial.println("ESP32 LORA READY");
}

void loop()
{
    // ============================================
    // READ SENSORS
    // ============================================
    float t = dht.readTemperature();

    float h = dht.readHumidity();

    int coRaw  = analogRead(MQ7_PIN);

    int ch4Raw = analogRead(MQ4_PIN);

    // SIMPLE SCALING
    float co  = coRaw / 100.0;

    float ch4 = ch4Raw / 100.0;

    // ============================================
    // CREATE SMALL PACKET
    // ============================================
    char packet[64];

    snprintf(
        packet,
        sizeof(packet),
        "%d,%.1f,%.1f,%.1f,%.1f",
        NODE_ID,
        t,
        h,
        co,
        ch4
    );

    // ============================================
    // SEND PACKET
    // ============================================
    Serial.print("Sending: ");

    Serial.println(packet);

    LoRa.beginPacket();

    LoRa.print(packet);

    LoRa.endPacket();

    Serial.println("TX OK");

    delay(5000);
}