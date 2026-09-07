#include <ESP8266WiFi.h>
#include <bearssl/bearssl_hmac.h>
#include <bearssl/bearssl_hash.h>
void setup() {
  Serial.begin(115200);
  br_hmac_key_context kc;
  br_hmac_context hc;
  const uint8_t key[] = "12345678";
  const uint8_t data[] = "hello";
  uint8_t out[32];
  br_hmac_key_init(&kc, &br_sha256_vtable, key, 8);
  br_hmac_init(&hc, &kc, 0);
  br_hmac_update(&hc, data, 5);
  br_hmac_out(&hc, out);
  Serial.println("OK!");
}
void loop() {}
