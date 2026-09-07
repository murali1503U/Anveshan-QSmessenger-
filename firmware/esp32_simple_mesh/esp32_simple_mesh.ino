#include <WiFi.h>
#include <WebServer.h>
#include <SPI.h>
#include <LoRa.h>

const char* ssid = "ESP32_LORA";
const char* password = "12345678";

WebServer server(80);

#define LORA_SCK   18
#define LORA_MISO  19
#define LORA_MOSI  23
#define LORA_SS    5
#define LORA_RST   14
#define LORA_DIO0  26
#define LORA_FREQ 433E6 // Change to 868E6 if using 868MHz modules!

// --- SMART MESSAGE QUEUE ---
#define MSG_HISTORY 10
String msgHistory[MSG_HISTORY];
int currentMsgIndex = 0;
String lastSentMessage = "";

struct PhoneState {
  IPAddress ip;
  int lastSeenIndex;
};
PhoneState phones[10];

int getPhoneIndex(IPAddress ip) {
  for (int i = 0; i < 10; i++) if (phones[i].ip == ip) return i;
  for (int i = 0; i < 10; i++) {
    if (phones[i].ip == IPAddress(0,0,0,0)) {
      phones[i].ip = ip;
      phones[i].lastSeenIndex = currentMsgIndex;
      return i;
    }
  }
  return 0;
}

void pushMessage(String msg, IPAddress senderIp) {
  currentMsgIndex = (currentMsgIndex + 1) % MSG_HISTORY;
  msgHistory[currentMsgIndex] = msg;
  
  if (senderIp != IPAddress(0,0,0,0)) {
    int pIdx = getPhoneIndex(senderIp);
    phones[pIdx].lastSeenIndex = currentMsgIndex;
  }
}
// ----------------------------

String webpage() {
  String html = R"rawliteral(
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>ESP32 LoRa Terminal</title>
<style>
body { font-family: Arial; background: #111; color: white; text-align: center; margin: 0; padding: 20px; }
h1 { color: #00ff99; }
.container { max-width: 600px; margin: auto; }
.card { background: #222; padding: 20px; margin-top: 20px; border-radius: 15px; }
input { width: 80%; padding: 14px; font-size: 18px; border-radius: 8px; border: none; }
button { padding: 14px 25px; margin-top: 12px; font-size: 17px; border: none; border-radius: 8px; cursor: pointer; }
.send { background: #00cc77; color: white; }
.received { color: #00ff99; font-size: 22px; word-wrap: break-word; }
</style>
<script>
var msgLog = [];
function sendMessage() {
  let msg = document.getElementById("message").value;
  if(msg.length == 0) return;
  fetch("/send?msg=" + encodeURIComponent(msg));
  document.getElementById("message").value = "";
}
function updateMessage() {
  fetch("/message?t=" + new Date().getTime())
    .then(r => r.text())
    .then(data => {
      if(data.length > 0) {
        msgLog.push(data);
        if(msgLog.length > 20) msgLog.shift();
        document.getElementById("received").innerHTML = msgLog.join("<br>");
      }
    });
}
setInterval(updateMessage, 800);
</script>
</head>
<body>
<div class="container">
<h1>ESP32 LoRa Terminal</h1>
<div class="card">
<h2>Send Message</h2>
<input id="message" type="text" placeholder="Type message">
<br>
<button class="send" onclick="sendMessage()">SEND VIA LoRa</button>
</div>
<div class="card">
<h2>Received Messages</h2>
<div id="received" class="received">Waiting...</div>
</div>
</div>
</body>
</html>
)rawliteral";
  return html;
}

void handleRoot() {
  server.sendHeader("Cache-Control", "no-cache, no-store, must-revalidate");
  server.send(200, "text/html", webpage());
}

void handleSend() {
  if (!server.hasArg("msg")) {
    server.send(400, "text/plain", "No message");
    return;
  }
  String msg = server.arg("msg");
  
  pushMessage(msg, server.client().remoteIP());
  
  LoRa.beginPacket();
  LoRa.print(msg);
  LoRa.endPacket();
  LoRa.receive(); 
  
  lastSentMessage = msg;
  server.send(200, "text/plain", "Sent");
}

void handleMessage() {
  server.sendHeader("Cache-Control", "no-cache, no-store, must-revalidate");
  
  IPAddress ip = server.client().remoteIP();
  int pIdx = getPhoneIndex(ip);
  
  if (phones[pIdx].lastSeenIndex != currentMsgIndex) {
    phones[pIdx].lastSeenIndex = (phones[pIdx].lastSeenIndex + 1) % MSG_HISTORY;
    server.send(200, "text/plain", msgHistory[phones[pIdx].lastSeenIndex]);
  } else {
    server.send(200, "text/plain", "");
  }
}

void setup() {
  Serial.begin(115200);

  WiFi.mode(WIFI_AP);
  WiFi.softAP(ssid, password);
  Serial.println("ESP32 AP Started");

  SPI.begin(LORA_SCK, LORA_MISO, LORA_MOSI, LORA_SS);
  LoRa.setPins(LORA_SS, LORA_RST, LORA_DIO0);

  if (!LoRa.begin(LORA_FREQ)) {
    Serial.println("LoRa FAILED!");
    while (1) { delay(1000); }
  }

  LoRa.setSyncWord(0xA5); 
  LoRa.receive(); 
  Serial.println("LoRa initialized! SyncWord=0xA5");

  server.on("/", handleRoot);
  server.on("/send", handleSend);
  server.on("/message", handleMessage);
  server.begin();
}

void loop() {
  server.handleClient();

  int packetSize = LoRa.parsePacket();
  if (packetSize) {
    String incoming = "";
    while (LoRa.available()) {
      incoming += (char)LoRa.read();
    }
    if (incoming.length() > 0 && incoming != lastSentMessage) {
      lastSentMessage = incoming;
      pushMessage(incoming, IPAddress(0,0,0,0)); 
      Serial.print("Received from LoRa: ");
      Serial.println(incoming);
    }
  }
}
