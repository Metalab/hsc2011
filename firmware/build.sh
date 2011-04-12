#!/bin/sh
( cd vmcode && ./build.sh; )
arduino-cc firmware.cc hardware.cc RF12.cpp OneWire.cpp embedvm.c \
	firmware_vm.cc firmware_evt.cc firmware_ser.cc firmware_net.cc
arduino-cc ioexample.cc hardware.cc RF12.cpp OneWire.cpp
