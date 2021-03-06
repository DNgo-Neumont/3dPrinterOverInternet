from email.policy import default
from time import sleep
import serial
import serial.tools.list_ports
import serial.tools.list_ports_linux
#import serial.tools.list_ports_windows
import platform

#if(platform.system() == "Windows"):
#    ports = serial.tools.list_ports_windows.comports()
if(platform.system() == "Linux"):
    ports = serial.tools.list_ports_linux.comports()
else:
    ports = serial.tools.list_ports.comports()

print("Select a port: ")
num = 1
for i in ports:
    print(f"{num}. {i.name}")


selection = int(input()) - 1

portSelected = ports[selection]


portToUse = serial.Serial(portSelected.device)

portToUse.baudrate = 250000

portToUse.timeout = 4000

#portToUse.open()

sleep(10)

print(str(portToUse.read_all()))

printerInput = ""

while(printerInput != "exit"):
    printerInput = input() + "\n"
    print(f"Ascii encoding of input: {printerInput.encode('ascii')}")
    portToUse.write(printerInput.encode('ascii'))
    print(str(portToUse.read_all()))
