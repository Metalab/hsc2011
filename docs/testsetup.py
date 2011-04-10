# Usage example: python testsetup.py /dev/ttyUSB0 01

import sys
import serial
import time

s = serial.Serial(sys.argv[1], 57600, timeout=0.1)

s.setDTR(True)
s.setDTR(False)

while True:
	l = s.readline()
	if not l:
		continue;
	l = l.lstrip('\0\r\n')
	l = l.rstrip('\0\r\n')
	print "%s"%l
	if len(l) > 0 and l[0:10] == "=== 3.1415":
		break;

def consume_serinput():
	while True:
		l = s.readline()
		if not l:
			continue;
		l = l.rstrip('\0\r\n')
		print "%s"%l
		if len(l) > 0 and l[0] == '-':
			break;

def send_serial(cmd):
	for i in range(len(cmd)):
		# print "<%c>"%cmd[i]
		s.write(cmd[i])
		time.sleep(0.01)

send_serial("M05\n")
consume_serinput()
send_serial("M01 c01dc0ffebeef0%s\n"%sys.argv[2])
consume_serinput()
send_serial("M02 c01dc0ffebeeffff\n")
consume_serinput()
send_serial("M03\n")
consume_serinput()
send_serial("M04\n")
consume_serinput()
send_serial("M00\n")
consume_serinput()
send_serial("\n")
consume_serinput()

