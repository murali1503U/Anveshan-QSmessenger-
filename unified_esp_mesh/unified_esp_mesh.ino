
#if defined(ESP32)
  #include <WiFi.h>
  #include <SPI.h>
  #include <LoRa.h>
  #include <mbedtls/md.h> // ESP32 Hardware Crypto
  #define BOARD_TYPE 0x01
  #define AP_SSID "ESP-32 LoRa"
  #define LORA_SS 18
  #define LORA_RST 14
  #define LORA_DIO0 26
#elif defined(ESP8266)
  #include <ESP8266WiFi.h>
  #include <SPI.h>
  #include <LoRa.h>
  #include <bearssl/bearssl.h> // ESP8266 Crypto (BearSSL)
  #define BOARD_TYPE 0x02
  #define AP_SSID "ESP-12E LoRa"
  #define LORA_SS 15
  #define LORA_RST 16
  #define LORA_DIO0 4
#else
  #error "Unsupported hardware! Must be ESP32 or ESP8266."
#endif

#include <WiFiServer.h>
#include <WiFiClient.h>

const char DEFAULT_PSK[] = "12345678";
const uint16_t NODE_ID = 0x00E1;
const uint16_t TCP_PORT = 8266;
const long LORA_FREQUENCY = 868E6; // Change to 433E6 or 915E6 depending on your region

const uint8_t MAGIC_BYTE = 0xA5;
const uint8_t PROTOCOL_VERSION = 0x01;

WiFiServer server(TCP_PORT);
WiFiClient activeClient;

uint8_t rxBuffer[300];
uint8_t txBuffer[300];
uint8_t currentChallenge[16];
bool isAuthenticated = false;
uint8_t seqCounter = 0;

#define MSG_CACHE_SIZE 30
uint16_t recentMessages[MSG_CACHE_SIZE];
uint8_t cacheIndex = 0;

bool isDuplicatePacket(uint16_t crc) {
  for (int i = 0; i < MSG_CACHE_SIZE; i++) {
    if (recentMessages[i] == crc) return true;
  }
  recentMessages[cacheIndex] = crc;
  cacheIndex = (cacheIndex + 1) % MSG_CACHE_SIZE;
  return false;
}

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
#if defined(ESP32)
  mbedtls_md_context_t ctx;
  mbedtls_md_init(&ctx);
  mbedtls_md_setup(&ctx, mbedtls_md_info_from_type(MBEDTLS_MD_SHA256), 1);
  mbedtls_md_hmac_starts(&ctx, key, keyLen);
  mbedtls_md_hmac_update(&ctx, payload, payloadLen);
  mbedtls_md_hmac_finish(&ctx, output);
  mbedtls_md_free(&ctx);
#elif defined(ESP8266)
  br_hmac_key_context kc;
  br_hmac_context hc;
  br_hmac_key_init(&kc, &br_sha256_vtable, key, keyLen);
  br_hmac_init(&hc, &kc, 0);
  br_hmac_update(&hc, payload, payloadLen);
  br_hmac_out(&hc, output);
#endif
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

void forwardPacketToLoRa(const uint8_t *rawPacket, size_t packetLen, uint16_t crc) {
  if (isDuplicatePacket(crc)) {
    Serial.println(F("[LORA-MESH] Dropped ghost packet."));
    return;
  }
  LoRa.beginPacket();
  LoRa.write(rawPacket, packetLen);
  LoRa.endPacket();
  Serial.println(F("[LORA-MESH] Broadcasted packet over RF."));
}

void startAuthChallenge(WiFiClient &client) {
  isAuthenticated = false;
  for (int i = 0; i < 16; i++) currentChallenge[i] = (uint8_t)random(0, 256);
  sendPacketToPhone(client, 0x10, 0x00AA, currentChallenge, 16);
}

void setup() {
  Serial.begin(115200);
  delay(300);
  Serial.println(F("\n[SENTINEL] Initializing..."));

  #if defined(ESP32)
    SPI.begin(5, 19, 27, 18);
    LoRa.setSPI(SPI);
  #endif
  LoRa.setPins(LORA_SS, LORA_RST, LORA_DIO0);
  if (!LoRa.begin(LORA_FREQUENCY)) {
    Serial.println(F("[ERROR] LoRa failed."));
  } else {
    LoRa.setSpreadingFactor(7);
    LoRa.setSignalBandwidth(125E3);
    LoRa.setCodingRate4(5);
    LoRa.enableCrc();
  }

  for(int i=0; i<MSG_CACHE_SIZE; i++) recentMessages[i] = 0x0000;

  WiFi.mode(WIFI_AP);
  WiFi.softAPConfig(IPAddress(192, 168, 4, 1), IPAddress(192, 168, 4, 1), IPAddress(255, 255, 255, 0));
  WiFi.softAP(AP_SSID, "12345678");
  server.begin();
  server.setNoDelay(true);
  Serial.println(F("[SENTINEL] Ready! AP Started. Listening on port 8266."));
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

  int packetSize = LoRa.parsePacket();
  if (packetSize >= 12) {
    uint8_t loraBuf[300];
    int idx = 0;
    while (LoRa.available() && idx < 300) {
      loraBuf[idx++] = (uint8_t)LoRa.read();
    }
    
    uint16_t packetCrc = (loraBuf[idx-2] << 8) | loraBuf[idx-1];
    if (loraBuf[0] == MAGIC_BYTE) {
      if (!isDuplicatePacket(packetCrc) && activeClient && activeClient.connected() && isAuthenticated) {
         activeClient.write(loraBuf, idx);
         activeClient.flush();
      }
    }
  }

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
          rxBuffer[0] = MAGIC_BYTE; rxBuffer[1] = PROTOCOL_VERSION; rxBuffer[2] = type;
          rxBuffer[3] = (senderId >> 8) & 0xFF; rxBuffer[4] = senderId & 0xFF;
          rxBuffer[5] = (destId >> 8) & 0xFF; rxBuffer[6] = destId & 0xFF;
          rxBuffer[7] = seq; rxBuffer[8] = flags; rxBuffer[9] = payloadLen;

          uint16_t actualCrc = calculateCrc16(rxBuffer, 10 + payloadLen);
          if (actualCrc == expectedCrc) {
            if (!isAuthenticated && type == 0x11 && payloadLen == 32) {
              uint8_t hmacData[20];
              memcpy(hmacData, currentChallenge, 16);
              hmacData[16] = (NODE_ID >> 8) & 0xFF; hmacData[17] = NODE_ID & 0xFF;
              hmacData[18] = (senderId >> 8) & 0xFF; hmacData[19] = senderId & 0xFF;
              
              uint8_t expectedHmac[32];
              computeHmacSha256((const uint8_t *)DEFAULT_PSK, strlen(DEFAULT_PSK), hmacData, 20, expectedHmac);
              
              if (constantTimeCompare(&rxBuffer[10], expectedHmac, 32)) {
                isAuthenticated = true;
                uint8_t okPayload[3] = { (uint8_t)BOARD_TYPE, (uint8_t)((NODE_ID >> 8) & 0xFF), (uint8_t)(NODE_ID & 0xFF) };
                sendPacketToPhone(activeClient, 0x12, senderId, okPayload, 3);
              }
            } else if (isAuthenticated) {
              if (type == 0x01 || type == 0x04) {
                rxBuffer[10 + payloadLen] = (actualCrc >> 8) & 0xFF;
                rxBuffer[10 + payloadLen + 1] = actualCrc & 0xFF;
                forwardPacketToLoRa(rxBuffer, 12 + payloadLen, actualCrc);
              } else if (type == 0x02) {
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

