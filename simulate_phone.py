import socket
import struct
import hashlib
import hmac
import threading

MAGIC_BYTE = 0xA5
PROTOCOL_VERSION = 0x01
NODE_ID = 0x00E1          # The ESP's hardware ID
SIMULATED_PHONE_ID = 0x00F1 # Our Python script's fake ID
TCP_PORT = 8266
DEFAULT_PSK = b"12345678"

# Packet Types
PKT_MESSAGE   = 0x01
PKT_PING      = 0x02
PKT_ACK       = 0x03
PKT_AUTH_REQ  = 0x10
PKT_AUTH_RESP = 0x11
PKT_AUTH_OK   = 0x12
PKT_AUTH_FAIL = 0x13

def calculate_crc16(data: bytes) -> int:
    crc = 0xFFFF
    for byte in data:
        crc ^= (byte << 8)
        for _ in range(8):
            if crc & 0x8000:
                crc = ((crc << 1) ^ 0x1021) & 0xFFFF
            else:
                crc = (crc << 1) & 0xFFFF
    return crc & 0xFFFF

def send_packet(sock, pkt_type, dest_id, payload=b"", seq=1, flags=0):
    # Header: Magic(1), Version(1), Type(1), Sender(2), Dest(2), Seq(1), Flags(1), PayloadLen(1)
    header = struct.pack(">BBBHBBBB", 
                         MAGIC_BYTE, PROTOCOL_VERSION, pkt_type, 
                         SIMULATED_PHONE_ID, dest_id, seq, flags, len(payload))
    packet_no_crc = header + payload
    crc = calculate_crc16(packet_no_crc)
    full_packet = packet_no_crc + struct.pack(">H", crc)
    sock.sendall(full_packet)

def receive_packet(sock):
    header = sock.recv(10)
    if not header or len(header) < 10:
        return None
    magic, version, pkt_type, sender_id, dest_id, seq, flags, payload_len = struct.unpack(">BBBHBBBB", header)
    
    if magic != MAGIC_BYTE:
        print("[ERROR] Invalid magic byte received.")
        return None
        
    payload = b""
    if payload_len > 0:
        # Read exactly payload_len bytes
        bytes_read = 0
        while bytes_read < payload_len:
            chunk = sock.recv(payload_len - bytes_read)
            if not chunk: return None
            payload += chunk
            bytes_read += len(chunk)
            
    crc_bytes = sock.recv(2)
    if len(crc_bytes) < 2: return None
    expected_crc, = struct.unpack(">H", crc_bytes)
    
    actual_crc = calculate_crc16(header + payload)
    if actual_crc != expected_crc:
        print("[ERROR] CRC mismatch!")
        return None
        
    return pkt_type, sender_id, dest_id, payload

def listen_loop(sock):
    while True:
        try:
            pkt = receive_packet(sock)
            if not pkt:
                print("\n[DISCONNECTED] Connection closed by ESP.")
                break
            pkt_type, sender_id, dest_id, payload = pkt
            
            if pkt_type == PKT_MESSAGE:
                msg = payload.decode('utf-8', errors='ignore')
                print(f"\n[MESH-RELAY] Message from {hex(sender_id)}: {msg}")
            elif pkt_type == PKT_ACK:
                pass # print(f"[ACK] Received from {hex(sender_id)}")
            elif pkt_type == PKT_PING:
                print(f"\n[PING] Received from {hex(sender_id)}")
            else:
                print(f"\n[SYS] Type: {hex(pkt_type)}, Payload: {payload.hex()}")
        except Exception as e:
            print(f"\n[ERROR] Listen loop crashed: {e}")
            break

def main():
    print("========================================")
    print(" Sentinel Mesh Simulator (Python Client) ")
    print("========================================")
    
    ip = input("Enter ESP IP Address (Press Enter for 192.168.4.1): ").strip()
    if not ip:
        ip = "192.168.4.1"
        
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(10.0)
    
    print(f"\n[*] Connecting to {ip}:{TCP_PORT}...")
    try:
        sock.connect((ip, TCP_PORT))
    except Exception as e:
        print(f"[!] Connection failed: {e}")
        print("    Are you connected to the SENTINEL_ESP32 or SENTINEL_ESP12 Wi-Fi network?")
        return

    sock.settimeout(None) # Make blocking again for normal I/O
    print("[*] Connected! Waiting for AUTH challenge from ESP...")
    
    # 1. Wait for AUTH_REQ
    pkt = receive_packet(sock)
    if not pkt: return
    pkt_type, sender_id, dest_id, payload = pkt
    if pkt_type != PKT_AUTH_REQ:
        print(f"[!] Expected AUTH_REQ (0x10), got {hex(pkt_type)}")
        return
        
    challenge = payload
    print(f"[*] Received Challenge: {challenge.hex()}")
    
    # 2. Compute HMAC-SHA256 response
    # ESP computes: hmacData = challenge(16) + ESP_NODE_ID(2) + SENDER_ID(2)
    hmac_data = challenge + struct.pack(">H", NODE_ID) + struct.pack(">H", SIMULATED_PHONE_ID)
    hmac_calc = hmac.new(DEFAULT_PSK, hmac_data, hashlib.sha256).digest()
    
    # 3. Send AUTH_RESP
    print("[*] Sending AUTH_RESP HMAC...")
    send_packet(sock, PKT_AUTH_RESP, NODE_ID, hmac_calc)
    
    # 4. Wait for AUTH_OK
    pkt = receive_packet(sock)
    if not pkt: return
    pkt_type, sender_id, dest_id, payload = pkt
    if pkt_type == PKT_AUTH_OK:
        print("\n[+] AUTHENTICATED SUCCESSFULLY! Secure mesh tunnel established.")
    else:
        print(f"\n[!] AUTH FAILED: Expected AUTH_OK (0x12), got {hex(pkt_type)}")
        return

    # Start listening thread for relayed messages
    t = threading.Thread(target=listen_loop, args=(sock,), daemon=True)
    t.start()
    
    print("\n---------------------------------------------------------")
    print(" You can now chat! Type a message and press Enter.")
    print(" Messages sent here will broadcast to all real Android apps")
    print(" connected to the ESP. Any message sent from the Android")
    print(" app will be relayed and printed here.")
    print(" Type 'exit' to quit.")
    print("---------------------------------------------------------")
    
    seq = 2
    while True:
        try:
            msg = input("")
            if msg.lower() == 'exit':
                break
            if msg:
                send_packet(sock, PKT_MESSAGE, 0xFFFF, msg.encode('utf-8'), seq=seq)
                seq += 1
        except KeyboardInterrupt:
            break

    print("[*] Closing connection.")
    sock.close()

if __name__ == '__main__':
    main()
