/*
 * Sentinel Node - Phase 2 Wi-Fi TCP to SX1276 LoRa Bridge Firmware
 * Compatible with ESP32 and ESP8266 (ESP-12E) with SX1276/SX1278 SPI Module
 *
 * Pinout Configuration for SX1276/SX1278:
 * ESP32:   NSS=18, RST=14, DIO0=26, SCK=5, MISO=19, MOSI=27
 * ESP8266: NSS=15 (D8), RST=16 (D0), DIO0=4 (D2), SCK=14 (D5), MISO=12 (D6), MOSI=13 (D7)
 *
 * Requirements:
 * - Arduino LoRa library (sandeepmistry/arduino-LoRa)
 */

#if defined(ESP32)
  #include <WiFi.h>
  #include <SPI.h>
  #include <LoRa.h>
  #define BOARD_TYPE 0x01 // ESP32
  #define AP_SSID "SENTINEL_ESP32"
  #define LORA_SS 18
  #define LORA_RST 14
  #define LORA_DIO0 26
#elif defined(ESP8266)
  #include <ESP8266WiFi.h>
  #include <SPI.h>
  #include <LoRa.h>
  #define BOARD_TYPE 0x02 // ESP-12E / ESP8266
  #define AP_SSID "SENTINEL_ESP12"
  #define LORA_SS 15
  #define LORA_RST 16
  #define LORA_DIO0 4
#endif

#include <WiFiServer.h>
#include <WiFiClient.h>
#include <mbedtls/md.h>

const char DEFAULT_PSK[] = "12345678";
const uint16_t NODE_ID = 0x00E1;
const uint16_t TCP_PORT = 8266;
const long LORA_FREQUENCY = 868E6; // 868 MHz or 915E6 or 433E6

const uint8_t MAGIC_BYTE = 0xA5;
const uint8_t PROTOCOL_VERSION = 0x01;

WiFiServer server(TCP_PORT);
WiFiClient activeClient;

uint8_t rxBuffer[300];
uint8_t txBuffer[300];
uint8_t currentChallenge[16];
bool isAuthenticated = false;
uint8_t seqCounter = 0;

uint16_t calculateCrc16(const uint8_t *data, size_t length) {
  uint16_t crc = 0xFFFF;
  for (size_t i = 0; i < length; i++) {
    crc ^= ((uint16_t)data[i]) << 8;
    for (uint8_t j = 0; j < 8; j++) {
      if (crc & 0x8000) crc = ((crc << 1) ^ 0x1021) & 0xFFFF;
      else crc = (crc << 1) & 0xFFFF;
    }
  }
  return crc;
}

bool constantTimeCompare(const uint8_t *a, const uint8_t *b, size_t length) {
  uint8_t result = 0;
  for (size_t i = 0; i < length; i++) result |= a[i] ^ b[i];
  return (result == 0);
}

void computeHmacSha256(const uint8_t *key, size_t keyLen, const uint8_t *payload, size_t payloadLen, uint8_t *output) {
  mbedtls_md_context_t ctx;
  mbedtls_md_init(&ctx);
  mbedtls_md_setup(&ctx, mbedtls_md_info_from_type(MBEDTLS_MD_SHA256), 1);
  mbedtls_md_hmac_starts(&ctx, key, keyLen);
  mbedtls_md_hmac_update(&ctx, payload, payloadLen);
  mbedtls_md_hmac_finish(&ctx, output);
  mbedtls_md_free(&ctx);
}

void sendPacketToPhone(WiFiClient &client, uint8_t type, uint16_t destId, const uint8_t *payload, uint8_t payloadLen, uint8_t flags = 0) {
  if (!client.connected()) return;
  txBuffer[0] = MAGIC_BYTE;
  txBuffer[1] = PROTOCOL_VERSION;
  txBuffer[2] = type;
  txBuffer[3] = (NODE_ID >> 8) & 0xFF;
  txBuffer[4] = NODE_ID & 0xFF;
  txBuffer[5] = (destId >> 8) & 0xFF;
  txBuffer[6] = destId & 0xFF;
  txBuffer[7] = ++seqCounter;
  txBuffer[8] = flags;
  txBuffer[9] = payloadLen;
  if (payloadLen > 0 && payload != NULL) memcpy(&txBuffer[10], payload, payloadLen);
  size_t headerAndPayloadLen = 10 + payloadLen;
  uint16_t crc = calculateCrc16(txBuffer, headerAndPayloadLen);
  txBuffer[headerAndPayloadLen] = (crc >> 8) & 0xFF;
  txBuffer[headerAndPayloadLen + 1] = crc & 0xFF;
  client.write(txBuffer, headerAndPayloadLen + 2);
  client.flush();
}

void forwardPacketToLoRa(const uint8_t *rawPacket, size_t packetLen) {
  LoRa.beginPacket();
  LoRa.write(rawPacket, packetLen);
  LoRa.endPacket();
  Serial.print(F("[LORA-MESH] Broadcasted packet over RF (bytes="));
  Serial.print(packetLen);
  Serial.println(F(")"));
}

void startAuthChallenge(WiFiClient &client) {
  isAuthenticated = false;
  for (int i = 0; i < 16; i++) currentChallenge[i] = (uint8_t)random(0, 256);
  sendPacketToPhone(client, 0x10, 0x00AA, currentChallenge, 16);
}

void setup() {
  Serial.begin(115200);
  delay(300);
  Serial.println(F("\n[SENTINEL] Initializing Wi-Fi + LoRa Bridge..."));

  // Start LoRa SPI
  LoRa.setPins(LORA_SS, LORA_RST, LORA_DIO0);
  if (!LoRa.begin(LORA_FREQUENCY)) {
    Serial.println(F("[ERROR] LoRa module initialization failed! Running in Wi-Fi standalone mode."));
  } else {
    LoRa.setSpreadingFactor(7);
    LoRa.setSignalBandwidth(125E3);
    LoRa.setCodingRate4(5);
    LoRa.enableCrc();
    Serial.println(F("[LORA] Radio Online (868 MHz)"));
  }

  // Start SoftAP & TCP Server
  WiFi.mode(WIFI_AP);
  WiFi.softAPConfig(IPAddress(192, 168, 4, 1), IPAddress(192, 168, 4, 1), IPAddress(255, 255, 255, 0));
  WiFi.softAP(AP_SSID, "12345678");
  server.begin();
  server.setNoDelay(true);
  Serial.println(F("[SENTINEL] Ready for phone connection on port 8266."));
}

void loop() {
  if (!activeClient || !activeClient.connected()) {
    WiFiClient newClient = server.available();
    if (newClient) {
      activeClient = newClient;
      activeClient.setNoDelay(true);
      startAuthChallenge(activeClient);
    }
  }

  // Check LoRa RF Incoming Packet -> Forward to Phone
  int packetSize = LoRa.parsePacket();
  if (packetSize >= 12) {
    uint8_t loraBuf[300];
    int idx = 0;
    while (LoRa.available() && idx < 300) {
      loraBuf[idx++] = (uint8_t)LoRa.read();
    }
    if (loraBuf[0] == MAGIC_BYTE && activeClient && activeClient.connected() && isAuthenticated) {
      activeClient.write(loraBuf, idx);
      activeClient.flush();
      Serial.println(F("[BRIDGE] Relayed LoRa packet to connected Android Phone via TCP"));
    }
  }

  // Check Phone Incoming Packet -> Forward to LoRa
  if (activeClient && activeClient.connected() && activeClient.available() >= 12) {
    if (activeClient.read() == MAGIC_BYTE) {
      uint8_t ver = activeClient.read();
      if (ver == PROTOCOL_VERSION) {
        uint8_t type = activeClient.read();
        uint16_t senderId = (activeClient.read() << 8) | activeClient.read();
        uint16_t destId = (activeClient.read() << 8) | activeClient.read();
        uint8_t seq = activeClient.read();
        uint8_t flags = activeClient.read();
        uint8_t payloadLen = activeClient.read();

        size_t bytesRead = 0;
        unsigned long timeout = millis() + 500;
        while (bytesRead < payloadLen && millis() < timeout) {
          if (activeClient.available()) rxBuffer[10 + bytesRead++] = activeClient.read();
        }

        if (activeClient.available() >= 2) {
          uint16_t expectedCrc = (activeClient.read() << 8) | activeClient.read();
          rxBuffer[0] = MAGIC_BYTE;
          rxBuffer[1] = PROTOCOL_VERSION;
          rxBuffer[2] = type;
          rxBuffer[3] = (senderId >> 8) & 0xFF;
          rxBuffer[4] = senderId & 0xFF;
          rxBuffer[5] = (destId >> 8) & 0xFF;
          rxBuffer[6] = destId & 0xFF;
          rxBuffer[7] = seq;
          rxBuffer[8] = flags;
          rxBuffer[9] = payloadLen;

          uint16_t actualCrc = calculateCrc16(rxBuffer, 10 + payloadLen);
          if (actualCrc == expectedCrc) {
            if (!isAuthenticated && type == 0x11 && payloadLen == 32) {
              uint8_t hmacData[20];
              memcpy(hmacData, currentChallenge, 16);
              hmacData[16] = (NODE_ID >> 8) & 0xFF;
              hmacData[17] = NODE_ID & 0xFF;
              hmacData[18] = (senderId >> 8) & 0xFF;
              hmacData[19] = senderId & 0xFF;
              uint8_t expectedHmac[32];
              computeHmacSha256((const uint8_t *)DEFAULT_PSK, strlen(DEFAULT_PSK), hmacData, 20, expectedHmac);
              if (constantTimeCompare(&rxBuffer[10], expectedHmac, 32)) {
                isAuthenticated = true;
                uint8_t okPayload[3] = { (uint8_t)BOARD_TYPE, (uint8_t)((NODE_ID >> 8) & 0xFF), (uint8_t)(NODE_ID & 0xFF) };
                sendPacketToPhone(activeClient, 0x12, senderId, okPayload, 3);
              }
            } else if (isAuthenticated) {
              if (type == 0x01 || type == 0x04) {
                // Forward chat payload to LoRa
                rxBuffer[10 + payloadLen] = (actualCrc >> 8) & 0xFF;
                rxBuffer[10 + payloadLen + 1] = actualCrc & 0xFF;
                forwardPacketToLoRa(rxBuffer, 12 + payloadLen);
              } else if (type == 0x02) {
                // PING -> Send ACK
                uint8_t ackPayload[3] = { (uint8_t)BOARD_TYPE, (uint8_t)((NODE_ID >> 8) & 0xFF), (uint8_t)(NODE_ID & 0xFF) };
                sendPacketToPhone(activeClient, 0x03, senderId, ackPayload, 3);
              }
            }
          }
        }
      }
    }
  }

  delay(2);
}

/*
 * Sentinel Node - Phase 2 Wi-Fi TCP to SX1276 LoRa Bridge Firmware
 * Compatible with ESP32 and ESP8266 (ESP-12E) with SX1276/SX1278 SPI Module
 *
 * Pinout Configuration for SX1276/SX1278:
 * ESP32:   NSS=18, RST=14, DIO0=26, SCK=5, MISO=19, MOSI=27
 * ESP8266: NSS=15 (D8), RST=16 (D0), DIO0=4 (D2), SCK=14 (D5), MISO=12 (D6), MOSI=13 (D7)
 *
 * Requirements:
 * - Arduino LoRa library (sandeepmistry/arduino-LoRa)
 */

#if defined(ESP32)
  #include <WiFi.h>
  #include <SPI.h>
  #include <LoRa.h>
  #define BOARD_TYPE 0x01 // ESP32
  #define AP_SSID "SENTINEL_ESP32"
  #define LORA_SS 18
  #define LORA_RST 14
  #define LORA_DIO0 26
#elif defined(ESP8266)
  #include <ESP8266WiFi.h>
  #include <SPI.h>
  #include <LoRa.h>
  #define BOARD_TYPE 0x02 // ESP-12E / ESP8266
  #define AP_SSID "SENTINEL_ESP12"
  #define LORA_SS 15
  #define LORA_RST 16
  #define LORA_DIO0 4
#endif

#include <WiFiServer.h>
#include <WiFiClient.h>
#include <mbedtls/md.h>

const char DEFAULT_PSK[] = "12345678";
const uint16_t NODE_ID = 0x00E1;
const uint16_t TCP_PORT = 8266;
const long LORA_FREQUENCY = 868E6; // 868 MHz or 915E6 or 433E6

const uint8_t MAGIC_BYTE = 0xA5;
const uint8_t PROTOCOL_VERSION = 0x01;

WiFiServer server(TCP_PORT);
WiFiClient activeClient;

uint8_t rxBuffer[300];
uint8_t txBuffer[300];
uint8_t currentChallenge[16];
