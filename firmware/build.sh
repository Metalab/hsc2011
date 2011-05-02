#!/bin/sh
# if you don'thave arduino-cc:
# wget 'http://svn.clifford.at/tools/trunk/arduino-cc.sh' -O arduino-cc
# chmod +x arduino-cc
# export PATH=$PWD

( cd vmcode && ./build.py && ./build-example.sh )
arduino-cc firmware.cc hardware.cc RF12.cpp OneWire.cpp embedvm.c \
	firmware_vm.cc firmware_evt.cc firmware_ser.cc firmware_net.cc
arduino-cc ioexample.cc hardware.cc RF12.cpp OneWire.cpp
