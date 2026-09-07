/*
 * Sentinel Node - Phase 1 Wi-Fi TCP Transceiver Firmware
 * Compatible with ESP32 and ESP8266 (ESP-12E / NodeMCU)
 *
 * Provides:
 * 1. SoftAP (SSID: SENTINEL_ESP32 or SENTINEL_ESP12, IP: 192.168.4.1)
 * 2. Non-blocking TCP Server on Port 8266
 * 3. Mandatory PSK Challenge-Response Handshake (HMAC-SHA256)
 * 4. Sentinel Binary Packet Framing & CRC16 Validation
 * 5. Low-RAM optimization for ESP8266 (zero heap fragmentation)
 */

#if defined(ESP32)
  #include <WiFi.h>
  #define BOARD_TYPE 0x01 // ESP32 LoRa Gateway
  #define AP_SSID "SENTINEL_ESP32"
#elif defined(ESP8266)
  #include <ESP8266WiFi.h>
  #define BOARD_TYPE 0x02 // ESP-12E / ESP8266 Wi-Fi Node
  #define AP_SSID "SENTINEL_ESP12"
#else
  #error "Unsupported hardware architecture! Must compile for ESP32 or ESP8266."
#endif

#include <WiFiServer.h>
#include <WiFiClient.h>

// Pre-Shared Key (Must match Android app configuration)
const char DEFAULT_PSK[] = "12345678";
const uint16_t NODE_ID = 0x00E1; // Sentinel Node Hardware ID
const uint16_t TCP_PORT = 8266;

// Binary Protocol Constants
const uint8_t MAGIC_BYTE = 0xA5;
const uint8_t PROTOCOL_VERSION = 0x01;

enum PacketType {
  PKT_MESSAGE   = 0x01,
  PKT_PING      = 0x02,
  PKT_ACK       = 0x03,
  PKT_SOS       = 0x04,
  PKT_IDENTITY  = 0x05,
  PKT_AUTH_REQ  = 0x10,
  PKT_AUTH_RESP = 0x11,
  PKT_AUTH_OK   = 0x12,
  PKT_AUTH_FAIL = 0x13
};

WiFiServer server(TCP_PORT);
WiFiClient activeClient;

// Static Memory Buffers (Prevents Heap Fragmentation)
uint8_t rxBuffer[300];
uint8_t txBuffer[300];
uint8_t currentChallenge[16];
bool isAuthenticated = false;
uint8_t seqCounter = 0;
unsigned long lastHeapCheck = 0;

// CRC16-CCITT (Polynomial 0x1021, Init 0xFFFF)
uint16_t calculateCrc16(const uint8_t *data, size_t length) {
  uint16_t crc = 0xFFFF;
  for (size_t i = 0; i < length; i++) {
    crc ^= ((uint16_t)data[i]) << 8;
    for (uint8_t j = 0; j < 8; j++) {
      if (crc & 0x8000) {
        crc = ((crc << 1) ^ 0x1021) & 0xFFFF;
      } else {
        crc = (crc << 1) & 0xFFFF;
      }
    }
  }
  return crc;
}

// Constant-time memory comparison to prevent timing attacks
bool constantTimeCompare(const uint8_t *a, const uint8_t *b, size_t length) {
  uint8_t result = 0;
  for (size_t i = 0; i < length; i++) {
    result |= a[i] ^ b[i];
  }
  return (result == 0);
}

// Simple SHA256 / HMAC for embedded microcontrollers
// Fallback portable implementation for ESP32/ESP8266
#include <mbedtls/md.h> // Available on both ESP32 and ESP8266 cores

void computeHmacSha256(const uint8_t *key, size_t keyLen, const uint8_t *payload, size_t payloadLen, uint8_t *output) {
  mbedtls_md_context_t ctx;
  mbedtls_md_type_t md_type = MBEDTLS_MD_SHA256;
  mbedtls_md_init(&ctx);
  mbedtls_md_setup(&ctx, mbedtls_md_info_from_type(md_type), 1);
  mbedtls_md_hmac_starts(&ctx, key, keyLen);
  mbedtls_md_hmac_update(&ctx, payload, payloadLen);
  mbedtls_md_hmac_finish(&ctx, output);
  mbedtls_md_free(&ctx);
}

void sendPacket(WiFiClient &client, uint8_t type, uint16_t destId, const uint8_t *payload, uint8_t payloadLen, uint8_t flags = 0) {
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

  if (payloadLen > 0 && payload != NULL) {
    memcpy(&txBuffer[10], payload, payloadLen);
  }

  size_t headerAndPayloadLen = 10 + payloadLen;
  uint16_t crc = calculateCrc16(txBuffer, headerAndPayloadLen);
  txBuffer[headerAndPayloadLen] = (crc >> 8) & 0xFF;
  txBuffer[headerAndPayloadLen + 1] = crc & 0xFF;

  client.write(txBuffer, headerAndPayloadLen + 2);
  client.flush();
}

void startAuthChallenge(WiFiClient &client) {
  isAuthenticated = false;
  // Generate 16 bytes cryptographically pseudo-random challenge
  for (int i = 0; i < 16; i++) {
    currentChallenge[i] = (uint8_t)random(0, 256);
  }
  Serial.println(F("[SENTINEL] Sending PSK Challenge to client..."));
  sendPacket(client, PKT_AUTH_REQ, 0x00AA, currentChallenge, 16);
}

void processIncomingPacket(WiFiClient &client, uint8_t type, uint16_t senderId, const uint8_t *payload, uint8_t payloadLen, uint8_t flags) {
  // Authentication check
  if (!isAuthenticated) {
    if (type == PKT_AUTH_RESP && payloadLen == 32) {
      // Expected HMAC = HMAC-SHA256(PSK, challenge + NODE_ID + senderId)
      uint8_t hmacData[20];
      memcpy(hmacData, currentChallenge, 16);
      hmacData[16] = (NODE_ID >> 8) & 0xFF;
      hmacData[17] = NODE_ID & 0xFF;
      hmacData[18] = (senderId >> 8) & 0xFF;
      hmacData[19] = senderId & 0xFF;

      uint8_t expectedHmac[32];
      computeHmacSha256((const uint8_t *)DEFAULT_PSK, strlen(DEFAULT_PSK), hmacData, 20, expectedHmac);

      if (constantTimeCompare(payload, expectedHmac, 32)) {
        isAuthenticated = true;
        Serial.println(F("[SENTINEL] Client Authenticated Successfully!"));
        uint8_t okPayload[3] = { (uint8_t)BOARD_TYPE, (uint8_t)((NODE_ID >> 8) & 0xFF), (uint8_t)(NODE_ID & 0xFF) };
        sendPacket(client, PKT_AUTH_OK, senderId, okPayload, 3);
      } else {
        Serial.println(F("[SENTINEL] AUTH FAILED: Invalid PSK. Dropping client."));
        sendPacket(client, PKT_AUTH_FAIL, senderId, NULL, 0);
        client.stop();
      }
    } else {
      Serial.println(F("[SENTINEL] Rejected unauthenticated packet type."));
      client.stop();
    }
    return;
  }

  // Authenticated Packet Dispatch
  switch (type) {
    case PKT_PING: {
      Serial.println(F("[SENTINEL] Received PING -> Sending ACK/IDENTITY"));
      uint8_t ackPayload[3] = { (uint8_t)BOARD_TYPE, (uint8_t)((NODE_ID >> 8) & 0xFF), (uint8_t)(NODE_ID & 0xFF) };
      sendPacket(client, PKT_ACK, senderId, ackPayload, 3);
      break;
    }

    case PKT_MESSAGE:
    case PKT_SOS: {
      Serial.print(F("[SENTINEL] Chat Message Rx [Flags=0x"));
      Serial.print(flags, HEX);
      Serial.print(F("]: "));
      for (uint8_t i = 0; i < payloadLen; i++) {
        Serial.print((char)payload[i]);
      }
      Serial.println();
      
      // Send Acknowledgement back to Phone
      uint8_t ackVal[1] = { 0x00 };
      sendPacket(client, PKT_ACK, senderId, ackVal, 1);
      break;
    }

    default:
      Serial.print(F("[SENTINEL] Unknown Packet Type: 0x"));
      Serial.println(type, HEX);
      break;
  }
}

void setup() {
  Serial.begin(115200);
  delay(500);

  Serial.println(F("\n======================================"));
  Serial.println(F("    SENTINEL SECURE NODE FIRMWARE     "));
  Serial.println(F("======================================"));

  // Configure SoftAP
  IPAddress apIP(192, 168, 4, 1);
  IPAddress gateway(192, 168, 4, 1);
  IPAddress subnet(255, 255, 255, 0);

  WiFi.mode(WIFI_AP);
  WiFi.softAPConfig(apIP, gateway, subnet);
  WiFi.softAP(AP_SSID, "12345678");

  Serial.print(F("[SENTINEL] AP Started: "));
  Serial.println(F(AP_SSID));
  Serial.print(F("[SENTINEL] AP IP Address: "));
  Serial.println(WiFi.softAPIP());

  server.begin();
  server.setNoDelay(true);
  Serial.print(F("[SENTINEL] TCP Server listening on port "));
  Serial.println(TCP_PORT);
}

void loop() {
  // Check for new client connections
  if (!activeClient || !activeClient.connected()) {
    WiFiClient newClient = server.available();
    if (newClient) {
      activeClient = newClient;
      activeClient.setNoDelay(true);
      Serial.println(F("[SENTINEL] New Phone Connected to TCP Socket."));
      startAuthChallenge(activeClient);
    }
  }

  // Handle incoming data stream from connected phone
  if (activeClient && activeClient.connected() && activeClient.available() >= 12) {
    // Look for MAGIC byte 0xA5
    if (activeClient.read() == MAGIC_BYTE) {
      uint8_t ver = activeClient.read();
      if (ver == PROTOCOL_VERSION) {
        uint8_t type = activeClient.read();
        uint16_t senderId = (activeClient.read() << 8) | activeClient.read();
        uint16_t destId = (activeClient.read() << 8) | activeClient.read();
        uint8_t seq = activeClient.read();
        uint8_t flags = activeClient.read();
        uint8_t payloadLen = activeClient.read();

        // Read Payload
        size_t bytesRead = 0;
        unsigned long timeout = millis() + 500;
        while (bytesRead < payloadLen && millis() < timeout) {
          if (activeClient.available()) {
            rxBuffer[10 + bytesRead] = activeClient.read();
            bytesRead++;
          }
        }

        // Read CRC16
        if (activeClient.available() >= 2) {
          uint16_t expectedCrc = (activeClient.read() << 8) | activeClient.read();

          // Reconstruct header in rxBuffer to verify CRC
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
            processIncomingPacket(activeClient, type, senderId, &rxBuffer[10], payloadLen, flags);
          } else {
            Serial.println(F("[SENTINEL] CRC Mismatch! Dropping packet."));
          }
        }
      }
    }
  }

  // Free Heap Telemetry
  if (millis() - lastHeapCheck > 10000) {
    lastHeapCheck = millis();
#if defined(ESP8266)
    Serial.print(F("[TELEMETRY] Free Heap: "));
    Serial.print(ESP.getFreeHeap());
    Serial.println(F(" bytes"));
#elif defined(ESP32)
    Serial.print(F("[TELEMETRY] Free Heap: "));
    Serial.print(ESP.getFreeHeap());
    Serial.println(F(" bytes"));
#endif
  }

  delay(2); // Prevent watchdog timeout on ESP8266
}
