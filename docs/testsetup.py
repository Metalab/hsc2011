"""Tool to configure a connected edubuzzer device to use our testing base
station ID and a given client ID.

Usage example: python testsetup.py /dev/ttyUSB0 01"""

import sys
import serial
import time

if len(sys.argv) != 3 or len(sys.argv[2]) != 2:
	print >>sys.stderr, __doc__
	sys.exit(1)

s = serial.Serial(sys.argv[1], 57600, timeout=0.1)

s.setDTR(True)
s.setDTR(False)

while True:
	l = s.readline()
	if not l:
		continue;
	l = l.strip('\0\r\n')
	print l
	if l.startswith("=== 3.1415"):
		break;

def consume_serinput():
	while True:
		l = s.readline()
		if not l:
			continue;
		l = l.rstrip('\0\r\n')
		print l
		if l.startswith('-'):
			break;

def send_serial(cmd):
	for c in cmd:
		# print "<%s>"%c
		s.write(c)
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

