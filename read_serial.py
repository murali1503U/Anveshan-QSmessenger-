
import serial
import time
import threading

def read_port(port, baud, duration):
    try:
        ser = serial.Serial(port, baud, timeout=1)
        print(f'Opened {port}')
        end_time = time.time() + duration
        while time.time() < end_time:
            line = ser.readline()
            if line:
                print(f'[{port}] {line.decode('utf-8', errors='replace').strip()}')
        ser.close()
    except Exception as e:
        print(f'Error on {port}: {e}')

t1 = threading.Thread(target=read_port, args=('COM3', 115200, 5))
t2 = threading.Thread(target=read_port, args=('COM8', 115200, 5))
t1.start()
t2.start()
t1.join()
t2.join()

