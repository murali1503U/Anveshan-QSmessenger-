
import serial
import time

try:
    ser = serial.Serial('COM3', 115200, timeout=1)
    end_time = time.time() + 5
    while time.time() < end_time:
        line = ser.readline()
        if line:
            print(line)
    ser.close()
except Exception as e:
    print(e)

