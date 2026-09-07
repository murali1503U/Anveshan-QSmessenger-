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

#define MAX_CLIENTS 4   // up to 4 phones per ESP node

// Packet types (mirror Android app constants)
enum PacketType : uint8_t {
  PKT_MESSAGE   = 0x01,
  PKT_PING      = 0x02,
  PKT_ACK       = 0x03,
  PKT_SOS       = 0x04,
  PKT_AUTH_REQ  = 0x10,
  PKT_AUTH_RESP = 0x11,
  PKT_AUTH_OK   = 0x12,
  PKT_AUTH_FAIL = 0x13
};

WiFiServer server(TCP_PORT);
WiFiClient clients[MAX_CLIENTS];
bool       isAuthenticated[MAX_CLIENTS];
uint8_t    currentChallenge[MAX_CLIENTS][16];

uint8_t rxBuffer[300];
uint8_t txBuffer[300];
uint8_t seqCounter  = 0;
bool    loraOnline  = false;

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

// ── Send a packet to one specific client slot ─────────────────────────────
void sendPacketToClient(int idx, uint8_t type, uint16_t destId,
                        const uint8_t *payload, uint8_t payloadLen) {
  if (!clients[idx].connected()) return;
  txBuffer[0] = MAGIC_BYTE;
  txBuffer[1] = PROTOCOL_VERSION;
  txBuffer[2] = type;
  txBuffer[3] = (NODE_ID >> 8) & 0xFF;
  txBuffer[4] =  NODE_ID       & 0xFF;
  txBuffer[5] = (destId  >> 8) & 0xFF;
  txBuffer[6] =  destId        & 0xFF;
  txBuffer[7] = ++seqCounter;
  txBuffer[8] = 0x00; // flags
  txBuffer[9] = payloadLen;
  if (payloadLen > 0 && payload) memcpy(&txBuffer[10], payload, payloadLen);
  size_t   total = 10 + payloadLen;
  uint16_t crc   = calculateCrc16(txBuffer, total);
  txBuffer[total]     = (crc >> 8) & 0xFF;
  txBuffer[total + 1] =  crc       & 0xFF;
  clients[idx].write(txBuffer, total + 2);
  clients[idx].flush();
}

// ── LOCAL RELAY: forward a raw packet to every authenticated phone
//    on this ESP except the original sender.
//    Pass senderIdx = -1 when the source is LoRa (relay to ALL local phones).
void relayToLocalClients(int senderIdx, const uint8_t *rawPacket, size_t len) {
  for (int i = 0; i < MAX_CLIENTS; i++) {
    if (i == senderIdx)          continue; // skip original sender
    if (!clients[i].connected()) continue; // skip disconnected
    if (!isAuthenticated[i])     continue; // skip unauthenticated
    clients[i].write(rawPacket, len);
    clients[i].flush();
    Serial.print(F("[LOCAL] Relayed to slot "));
    Serial.println(i);
  }
}

// ── LORA TX: broadcast raw packet over RF to reach the other ESP ───────────
void forwardToLoRa(const uint8_t *rawPacket, size_t len) {
  if (!loraOnline) return;
  LoRa.beginPacket();
  LoRa.write(rawPacket, len);
  LoRa.endPacket(false); // non-blocking
  Serial.print(F("[LORA] TX "));
  Serial.print(len);
  Serial.println(F(" bytes over RF"));
}

// ── Start auth challenge for one slot ─────────────────────────────────────
void startAuthChallenge(int idx) {
  isAuthenticated[idx] = false;
  for (int i = 0; i < 16; i++) currentChallenge[idx][i] = (uint8_t)random(0, 256);
  sendPacketToClient(idx, PKT_AUTH_REQ, 0x00AA, currentChallenge[idx], 16);
  Serial.print(F("[AUTH] Challenge sent to slot "));
  Serial.println(idx);
}

// ── Setup ──────────────────────────────────────────────────────────────────
void setup() {
  Serial.begin(115200);
  delay(300);
  Serial.println(F("\n[SENTINEL] Multi-Client Mesh Node Starting..."));

  for (int i = 0; i < MAX_CLIENTS; i++) isAuthenticated[i] = false;

  // Init LoRa
  LoRa.setPins(LORA_SS, LORA_RST, LORA_DIO0);
  if (!LoRa.begin(LORA_FREQUENCY)) {
    Serial.println(F("[LORA] Init FAILED — Wi-Fi-only local relay still works."));
    loraOnline = false;
  } else {
    LoRa.setSpreadingFactor(7);
    LoRa.setSignalBandwidth(125E3);
    LoRa.setCodingRate4(5);
    LoRa.enableCrc();
    loraOnline = true;
    Serial.println(F("[LORA] Radio online (868 MHz, SF7, BW125)"));
  }

  // Start SoftAP + TCP server
  WiFi.mode(WIFI_AP);
  WiFi.softAPConfig(IPAddress(192,168,4,1), IPAddress(192,168,4,1), IPAddress(255,255,255,0));
  WiFi.softAP(AP_SSID, DEFAULT_PSK);
  server.begin();
  server.setNoDelay(true);

  Serial.print(F("[SENTINEL] AP: "));       Serial.println(F(AP_SSID));
  Serial.print(F("[SENTINEL] TCP Port: ")); Serial.println(TCP_PORT);
  Serial.println(F("[SENTINEL] Ready — up to 4 phones per node."));
}

// ── Main loop ──────────────────────────────────────────────────────────────
void loop() {

  // 1. Accept new phones into a free slot ───────────────────────────────────
  WiFiClient newClient = server.available();
  if (newClient) {
    bool placed = false;
    for (int i = 0; i < MAX_CLIENTS; i++) {
      if (!clients[i] || !clients[i].connected()) {
        clients[i] = newClient;
        clients[i].setNoDelay(true);
        isAuthenticated[i] = false;
        startAuthChallenge(i);
        Serial.print(F("[SENTINEL] Phone connected → slot ")); Serial.println(i);
        placed = true;
        break;
      }
    }
    if (!placed) {
      newClient.stop();
      Serial.println(F("[SENTINEL] All slots full — connection refused."));
    }
  }

  // 2. LoRa RX: packet from the other ESP → relay to ALL local phones ────────
  if (loraOnline) {
    int packetSize = LoRa.parsePacket();
    if (packetSize >= 12 && packetSize <= 300) {
      uint8_t loraBuf[300];
      int idx = 0;
      while (LoRa.available() && idx < 300) loraBuf[idx++] = (uint8_t)LoRa.read();
      if (loraBuf[0] == MAGIC_BYTE) {
        Serial.print(F("[LORA] RX ")); Serial.print(idx);
        Serial.println(F(" bytes → relaying to all local phones"));
        relayToLocalClients(-1, loraBuf, idx); // -1 = source is LoRa, send to ALL
      }
    }
  }

  // 3. Process each connected phone ─────────────────────────────────────────
  for (int i = 0; i < MAX_CLIENTS; i++) {
    if (!clients[i] || !clients[i].connected()) continue;
    if (clients[i].available() < 12)            continue;

    // Parse binary frame header
    if (clients[i].read() != MAGIC_BYTE)        continue;
    if (clients[i].read() != PROTOCOL_VERSION)  continue;

    uint8_t  type       = clients[i].read();
    uint16_t senderId   = ((uint16_t)clients[i].read() << 8) | clients[i].read();
    uint16_t destId     = ((uint16_t)clients[i].read() << 8) | clients[i].read();
    uint8_t  seq        = clients[i].read();
    uint8_t  flags      = clients[i].read();
    uint8_t  payloadLen = clients[i].read();

    // Read payload with timeout
    size_t bytesRead = 0;
    unsigned long deadline = millis() + 500;
    while (bytesRead < payloadLen && millis() < deadline) {
      if (clients[i].available()) rxBuffer[10 + bytesRead++] = clients[i].read();
    }

    // Read CRC
    if (clients[i].available() < 2) continue;
    uint16_t expectedCrc = ((uint16_t)clients[i].read() << 8) | clients[i].read();

    // Rebuild full header in rxBuffer for CRC verification
    rxBuffer[0] = MAGIC_BYTE; rxBuffer[1] = PROTOCOL_VERSION;
    rxBuffer[2] = type;
    rxBuffer[3] = (senderId >> 8) & 0xFF; rxBuffer[4] = senderId & 0xFF;
    rxBuffer[5] = (destId  >> 8) & 0xFF;  rxBuffer[6] = destId  & 0xFF;
    rxBuffer[7] = seq; rxBuffer[8] = flags; rxBuffer[9] = payloadLen;

    uint16_t actualCrc = calculateCrc16(rxBuffer, 10 + payloadLen);
    if (actualCrc != expectedCrc) {
      Serial.println(F("[CRC] Mismatch — packet dropped"));
      continue;
    }

    // Append CRC bytes so we have the full raw frame ready for relay/LoRa
    rxBuffer[10 + payloadLen]     = (actualCrc >> 8) & 0xFF;
    rxBuffer[10 + payloadLen + 1] =  actualCrc       & 0xFF;
    size_t fullLen = 12 + payloadLen;

    // ── AUTH HANDSHAKE ──────────────────────────────────────────────────────
    if (!isAuthenticated[i]) {
      if (type == PKT_AUTH_RESP && payloadLen == 32) {
        uint8_t hmacData[20];
        memcpy(hmacData, currentChallenge[i], 16);
        hmacData[16] = (NODE_ID  >> 8) & 0xFF; hmacData[17] = NODE_ID  & 0xFF;
        hmacData[18] = (senderId >> 8) & 0xFF; hmacData[19] = senderId & 0xFF;
        uint8_t expectedHmac[32];
        computeHmacSha256((const uint8_t*)DEFAULT_PSK, strlen(DEFAULT_PSK),
                          hmacData, 20, expectedHmac);
        if (constantTimeCompare(&rxBuffer[10], expectedHmac, 32)) {
          isAuthenticated[i] = true;
          uint8_t ok[3] = { BOARD_TYPE,
                            (uint8_t)((NODE_ID >> 8) & 0xFF),
                            (uint8_t)( NODE_ID        & 0xFF) };
          sendPacketToClient(i, PKT_AUTH_OK, senderId, ok, 3);
          Serial.print(F("[AUTH] Slot ")); Serial.print(i); Serial.println(F(" authenticated ✓"));
        } else {
          sendPacketToClient(i, PKT_AUTH_FAIL, senderId, NULL, 0);
          clients[i].stop();
          Serial.print(F("[AUTH] Slot ")); Serial.print(i); Serial.println(F(" REJECTED — wrong PSK"));
        }
      } else {
        clients[i].stop(); // reject any non-auth packet before handshake completes
      }
      continue;
    }

    // ── AUTHENTICATED MESSAGE ROUTING ───────────────────────────────────────
    switch (type) {

      case PKT_MESSAGE:
      case PKT_SOS:
        Serial.print(F("[MSG] From slot ")); Serial.print(i);
        Serial.println(F(" → local relay + LoRa TX"));

        // Step 1: deliver to all OTHER phones connected to THIS same ESP
        relayToLocalClients(i, rxBuffer, fullLen);

        // Step 2: broadcast over LoRa so the remote ESP (and its phones) gets it
        forwardToLoRa(rxBuffer, fullLen);
        break;

      case PKT_PING: {
        uint8_t ack[3] = { BOARD_TYPE,
                           (uint8_t)((NODE_ID >> 8) & 0xFF),
                           (uint8_t)( NODE_ID        & 0xFF) };
        sendPacketToClient(i, PKT_ACK, senderId, ack, 3);
        break;
      }

      default:
        Serial.print(F("[SENTINEL] Unknown packet type: 0x"));
        Serial.println(type, HEX);
        break;
    }
  }

  delay(2); // watchdog-safe yield
}
