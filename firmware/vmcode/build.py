#!/usr/bin/env python
import subprocess

TRAMPOLINE_START = 0x3000
FLASH_START = 0x4000

EVMFILE = 'flash.evm'
BINFILE = 'flash.bin'
HEADERFILE = 'flash.hdr'
ROMFILE = 'flash.rom'
TRAMPOLINEFILE = 'flash.trampoline'

############ sanity checks to make sure we're talking about the same constants

evmcode = open(EVMFILE).read()
memexpect = "$memaddr %#04x"%FLASH_START
assert memexpect in evmcode, "Expected %r in %s"%(memexpect, EVMFILE)
trampolineexpect = "$trampoline %#04x"%TRAMPOLINE_START
assert trampolineexpect in evmcode, "Expected %r in %s"%(trampolineexpect, EVMFILE)

############ compilation

subprocess.check_call(['evmcomp', 'flash.evm'])

############ reformatting for c file inclusion

bytecode = open(BINFILE).read()
assert bytecode[:TRAMPOLINE_START].strip('\0') == '', "Non-null bytecode outside trampoline and flash areas"

trampoline_data = bytecode[TRAMPOLINE_START:FLASH_START].rstrip('\0')
rom_data = bytecode[FLASH_START:]

with open(ROMFILE, 'w') as f:
    f.write(", ".join("%#02x"%ord(x) for x in rom_data))

with open(TRAMPOLINEFILE, 'w') as f:
    f.write(", ".join("%#02x"%ord(x) for x in trampoline_data))

with open(HEADERFILE, 'a') as f:
    f.write("/* added by build.py */\n")
    f.write("#define VMMEM_FLASH_SIZE %d\n"%len(rom_data))
    f.write("#define VMMEM_TRAMPOLINE_SIZE %d\n"%len(trampoline_data))
