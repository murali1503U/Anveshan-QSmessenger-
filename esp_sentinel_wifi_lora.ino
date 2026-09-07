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

// ── Portable SHA-256 + HMAC-SHA-256 ──────────────────────────────────────────
// Works on both ESP32 and ESP8266 without any external library.
// (ESP32 has mbedtls but ESP8266 does not — so we use this instead.)
static const uint32_t SHA256_K[64] = {
  0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,0x3956c25b,0x59f111f1,0x923f82a4,0xab1c5ed5,
  0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,0x72be5d74,0x80deb1fe,0x9bdc06a7,0xc19bf174,
  0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,
  0x983e5152,0xa831c66d,0xb00327c8,0xbf597fc7,0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,
  0x27b70a85,0x2e1b2138,0x4d2c6dfc,0x53380d13,0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,
  0xa2bfe8a1,0xa81a664b,0xc24b8b70,0xc76c51a3,0xd192e819,0xd6990624,0xf40e3585,0x106aa070,
  0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,0x682e6ff3,
  0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,0x90befffa,0xa4506ceb,0xbef9a3f7,0xc67178f2
};
#define SHA256_ROTR(x,n) (((x)>>(n))|((x)<<(32-(n))))
#define SHA256_CH(x,y,z)  (((x)&(y))^(~(x)&(z)))
#define SHA256_MAJ(x,y,z) (((x)&(y))^((x)&(z))^((y)&(z)))
#define SHA256_S0(x) (SHA256_ROTR(x,2)^SHA256_ROTR(x,13)^SHA256_ROTR(x,22))
#define SHA256_S1(x) (SHA256_ROTR(x,6)^SHA256_ROTR(x,11)^SHA256_ROTR(x,25))
#define SHA256_G0(x) (SHA256_ROTR(x,7)^SHA256_ROTR(x,18)^((x)>>3))
#define SHA256_G1(x) (SHA256_ROTR(x,17)^SHA256_ROTR(x,19)^((x)>>10))

struct SHA256_CTX { uint32_t state[8]; uint64_t count; uint8_t buf[64]; };

static void sha256_transform(SHA256_CTX *ctx, const uint8_t *blk) {
  uint32_t w[64], a,b,c,d,e,f,g,h,t1,t2;
  for(int i=0;i<16;i++) w[i]=((uint32_t)blk[i*4]<<24)|((uint32_t)blk[i*4+1]<<16)|((uint32_t)blk[i*4+2]<<8)|blk[i*4+3];
  for(int i=16;i<64;i++) w[i]=SHA256_G1(w[i-2])+w[i-7]+SHA256_G0(w[i-15])+w[i-16];
  a=ctx->state[0];b=ctx->state[1];c=ctx->state[2];d=ctx->state[3];
  e=ctx->state[4];f=ctx->state[5];g=ctx->state[6];h=ctx->state[7];
  for(int i=0;i<64;i++){
    t1=h+SHA256_S1(e)+SHA256_CH(e,f,g)+SHA256_K[i]+w[i];
    t2=SHA256_S0(a)+SHA256_MAJ(a,b,c);
    h=g;g=f;f=e;e=d+t1;d=c;c=b;b=a;a=t1+t2;
  }
  ctx->state[0]+=a;ctx->state[1]+=b;ctx->state[2]+=c;ctx->state[3]+=d;
  ctx->state[4]+=e;ctx->state[5]+=f;ctx->state[6]+=g;ctx->state[7]+=h;
}
static void sha256_init(SHA256_CTX *ctx){
  ctx->state[0]=0x6a09e667;ctx->state[1]=0xbb67ae85;ctx->state[2]=0x3c6ef372;ctx->state[3]=0xa54ff53a;
  ctx->state[4]=0x510e527f;ctx->state[5]=0x9b05688c;ctx->state[6]=0x1f83d9ab;ctx->state[7]=0x5be0cd19;
  ctx->count=0;
}
static void sha256_update(SHA256_CTX *ctx, const uint8_t *data, size_t len){
  uint32_t i,j=ctx->count%64;
  ctx->count+=len;
  for(i=0;i<len;i++){
    ctx->buf[j++]=data[i];
    if(j==64){sha256_transform(ctx,ctx->buf);j=0;}
  }
}
static void sha256_final(SHA256_CTX *ctx, uint8_t *digest){
  uint32_t j=ctx->count%64; uint64_t bits=ctx->count*8;
  ctx->buf[j++]=0x80;
  if(j>56){while(j<64)ctx->buf[j++]=0;sha256_transform(ctx,ctx->buf);j=0;}
  while(j<56)ctx->buf[j++]=0;
  for(int i=7;i>=0;i--){ctx->buf[j++]=(bits>>(i*8))&0xFF;}
  sha256_transform(ctx,ctx->buf);
  for(int i=0;i<8;i++){digest[i*4]=(ctx->state[i]>>24)&0xFF;digest[i*4+1]=(ctx->state[i]>>16)&0xFF;digest[i*4+2]=(ctx->state[i]>>8)&0xFF;digest[i*4+3]=ctx->state[i]&0xFF;}
}
static void sha256(const uint8_t *data, size_t len, uint8_t *out){
  SHA256_CTX c; sha256_init(&c); sha256_update(&c,data,len); sha256_final(&c,out);
}

void computeHmacSha256(const uint8_t *key, size_t keyLen, const uint8_t *payload, size_t payloadLen, uint8_t *output) {
  uint8_t k[64]={0}, ipad[64], opad[64], inner[32];
  if(keyLen>64){sha256(key,keyLen,k);}else{memcpy(k,key,keyLen);}
  for(int i=0;i<64;i++){ipad[i]=k[i]^0x36;opad[i]=k[i]^0x5C;}
  SHA256_CTX c;
  sha256_init(&c); sha256_update(&c,ipad,64); sha256_update(&c,payload,payloadLen); sha256_final(&c,inner);
  sha256_init(&c); sha256_update(&c,opad,64); sha256_update(&c,inner,32); sha256_final(&c,output);
}
// ─────────────────────────────────────────────────────────────────────────────

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

// ── Ghost Message Fix ─────────────────────────────────────────────────────────
// Tracks the last 32 (senderID, seq) pairs we forwarded to the phone.
// Any duplicate arriving within DEDUP_WINDOW_MS is silently dropped.
#define DEDUP_SLOTS 32
#define DEDUP_WINDOW_MS 8000UL   // 8 s window – raise if range > ~5 km

struct SeenPacket {
  uint16_t senderId;
  uint8_t  seq;
  unsigned long seenAt;
};
SeenPacket seenPackets[DEDUP_SLOTS];
uint8_t seenHead = 0; // circular write pointer

// Returns true if this (senderId, seq) was already forwarded recently
bool isDuplicate(uint16_t senderId, uint8_t seq) {
  unsigned long now = millis();
  for (uint8_t i = 0; i < DEDUP_SLOTS; i++) {
    if (seenPackets[i].senderId == senderId &&
        seenPackets[i].seq      == seq      &&
        (now - seenPackets[i].seenAt) < DEDUP_WINDOW_MS) {
      return true;
    }
  }
  return false;
}

void markSeen(uint16_t senderId, uint8_t seq) {
  seenPackets[seenHead] = { senderId, seq, millis() };
  seenHead = (seenHead + 1) % DEDUP_SLOTS;
}

// TX cooldown: ignore LoRa packets arriving within 300 ms of our own TX
// (self-echo from shared antenna / nearby reflections)
unsigned long lastTxTime = 0;
#define TX_ECHO_GUARD_MS 300UL
// ─────────────────────────────────────────────────────────────────────────────

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
  lastTxTime = millis(); // GHOST FIX: start echo-guard timer
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
    LoRa.setSyncWord(0xA5); // Network ID: only talk to other Sentinel nodes
    Serial.println(F("[LORA] Radio Online (868 MHz, SyncWord=0xA5)"));
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
    // GHOST FIX #1: Ignore anything arriving within TX_ECHO_GUARD_MS of our own transmit
    if ((millis() - lastTxTime) < TX_ECHO_GUARD_MS) {
      // Drain the FIFO so it doesn't linger
      while (LoRa.available()) LoRa.read();
      Serial.println(F("[GHOST] Dropped self-echo (TX echo guard)"));
    } else {
      uint8_t loraBuf[300];
      int idx = 0;
      while (LoRa.available() && idx < 300) {
        loraBuf[idx++] = (uint8_t)LoRa.read();
      }

      // Packet must start with magic byte
      if (loraBuf[0] == MAGIC_BYTE && idx >= 12) {
        uint16_t pktSenderId = ((uint16_t)loraBuf[3] << 8) | loraBuf[4];
        uint8_t  pktSeq      = loraBuf[7];

        // GHOST FIX #2: Drop if we already forwarded this (senderId, seq) recently
        if (isDuplicate(pktSenderId, pktSeq)) {
          Serial.print(F("[GHOST] Dropped duplicate LoRa packet seq=0x"));
          Serial.println(pktSeq, HEX);
        } else {
          markSeen(pktSenderId, pktSeq);
          if (activeClient && activeClient.connected() && isAuthenticated) {
            activeClient.write(loraBuf, idx);
            activeClient.flush();
            Serial.println(F("[BRIDGE] Relayed LoRa packet to Android Phone via TCP"));
          }
        }
      } else {
        Serial.println(F("[GHOST] Dropped malformed LoRa packet (bad magic/size)"));
      }
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
