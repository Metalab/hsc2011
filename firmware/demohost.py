"""A very simple proof-of-concept server application"""

import sys
import serial
import time
from collections import defaultdict

DELAYS = [0.5, 2.5, 4.5, 6, 10]

def send_line(line):
    if not line.endswith('\n'):
        line += '\n'
    print "sending line %r"%line
    s.write(line)

class CommandToDevice(object):
    def __init__(self, commandletter, destination, payload, ack_letter):
        perdevice_seqnums[destination] += 1
        self.destination = destination
        self.seqnum = perdevice_seqnums[destination]
        self.string = '%s %02x * %016x%s'%(commandletter, self.seqnum, destination, payload)
        self.leftover_delays = DELAYS[:]
        self.ack_letter = ack_letter

        if not pending_commands[destination]:
            self.init_time = time.time()
            send_line(self.string)
        else:
            self.init_time = None
        pending_commands[destination].append(self)

    def offer_resend(self):
        # it's actually sending the first time
        if self.init_time is None:
            print "sending packet for the first time"
            self.init_time = time.time()
            send_line(self.string)
        else:
            if time.time() - self.init_time > self.leftover_delays[0]:
                print "next resend is due for %r"%self
                self.leftover_delays.pop(0)
                if self.leftover_delays:
                    send_line(self.string)
                else:
                    print "Giving up waiting for reply, removing device"
                    connected_buzzers.remove(self.destination)
                    pending_commands[self.destination].remove(self)

    def __repr__(self):
        return "<CommandToDevice %r>"%self.string

WELCOME = "=== 3.14159265358979323846264338327950288419716939937510 ==="

s = serial.Serial(sys.argv[1], 57600, timeout=0.1)

s.setDTR(True)
s.setDTR(False)

connected_buzzers = set()

pending_commands = defaultdict(lambda:[])
perdevice_seqnums = defaultdict(lambda:0)
perdevice_button = defaultdict(lambda:None)

def action1():
    print "check macs"
    send_line('M00')

def action2():
    print "become a base station"
    send_line('M05')

actions = [action2, action1]

pre_read = ""
while True:
    pre_read += s.read(1)
    if pre_read.endswith(WELCOME):
        break

EMPTY_LINE_DIVIDER_MOD = 20
empty_line_divider = 0
tock_counter = 0
while True:
    while actions:
        actions.pop(0)()

    l = s.readline()
    l = l.lstrip('\0') # we never send 0, if there are any, they are from the boot loader
    if l:
        print repr(l)
        empty_line_divider = 0
    else:
        if empty_line_divider == 0:
            print "=========== tock (%s clients, %s) ==========="%(len(connected_buzzers), [perdevice_button[d] for d in connected_buzzers])
            tock_counter += 1
            if tock_counter >= 5:
                for b in connected_buzzers:
                    CommandToDevice('V', b, 'zzn n nn nnnn', 'v')
                tock_counter = 0

        empty_line_divider += 1
        empty_line_divider %= EMPTY_LINE_DIVIDER_MOD

    if l and l[0] not in '*-\r\n':
        # it's a package we can parse

        seqnum = int(l[2:4], 16)
        src = int(l[5:21], 16)
        dst = int(l[22:38], 16)

        if l[0] == 'L':
            if src in connected_buzzers:
                # it was reset or didn't get our 'l'
                connected_buzzers.remove(src)
                pending_commands[src] = []
            print "we have a new guest"
            connected_buzzers.add(src)

            time.sleep(0.01)
            send_line('l %02x * %016x'%(seqnum, src))
            # send a warm welcome
            CommandToDevice('S', src, 'nn nyffffff n nnnn ff 00', 's') # ff 0f to play the original game
            # don't do that -- sets error state!
            #CommandToDevice('R', src, ' 12 0000', 'r')
            #CommandToDevice('W', src, ' 01 0004 02', 'w')
            #CommandToDevice('W', src, ' 02 0010 23 42', 'w')
            #CommandToDevice('R', src, ' 12 0000', 'r')

            try:
                data = open('./vmcode/simon.py.bin').read()
            except IOError:
                print "not sending custom byte code for lack of file"
            else:
                CommandToDevice('S', src, 'nnzzzz0000', 's') # stop vm

                CommandToDevice('V', src, 'zzn y%04x nn nnnn'%(512-len(data)), 'v')
                snippletsize = 30
                for k in range(16, len(data), snippletsize):
                    snipplet = data[k:k+snippletsize]
                    CommandToDevice('W', src, ' %02x %04x '%(len(snipplet), k) + snipplet.encode('hex'), 'w')

                (entrypoint, ) = [int(l.split()[0], 16) for l in open('./vmcode/simon.py.sym') if l.endswith(' main\n')]

                CommandToDevice('V', src, 'zzn n nn nnnn', 'v')
                CommandToDevice('V', src, 'yzn n yn%04x nnnn'%entrypoint, 'v')

        if l[0] == 'E':
            if src not in connected_buzzers:
                # kill it with fire
                CommandToDevice('X', src, '', 'x')
            else:
                if l[39] == 'b':
                    print "hey, the did something -- i better inform the app that button mask is now %02x"
                    button = int(l[42:44], 16)
                    pressed = None
                    if button == 1<<0:
                        pressed = 0
                    elif button == 1<<1:
                        pressed = 1
                    elif button == 1<<2:
                        pressed = 2
                    elif button == 1<<3:
                        pressed = 3

                    if pressed is not None:
                        if pressed == perdevice_button[src]:
                            perdevice_button[src] = None
                        else:
                            perdevice_button[src] = pressed

                        CommandToDevice('S', src, 'nn nnn '+"".join('y' if perdevice_button[src] == i else 'n' for i in range(4))+'00 00', 's')

                else:
                    print "who the hell is interested in non-button events?"

        if pending_commands[src] and pending_commands[src][0].ack_letter == l[0] and pending_commands[src][0].seqnum == seqnum:
            # it's the pending command's ack!
            print "ack received, removing pending command from queue"
            pending_commands[src].pop(0)

    for per_host_pending in pending_commands.values():
        if per_host_pending:
            per_host_pending[0].offer_resend()
