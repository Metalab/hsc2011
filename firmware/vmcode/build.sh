#!/bin/sh

set -e

PROGMEM_START=0x310
evmcomp flash.evm
python -c "of = open('flash.progmem', 'w'); of.write(', '.join('%#02x'%ord(x) for x in open('flash.bin').read()[${PROGMEM_START}:]) + '\\n'); of.close()"
