from evm import userfunc as uf, Globals

gv = Globals()
gv.led = gv.array8u(0, length=4)
gv.leds = gv.int8u(4)
gv.rgb = gv.array8u(5, length=3)
gv.button = gv.array8u(8, length=4)
gv.buttons = gv.int8u(12)
gv.buzzer = gv.int16(13)

def show(which):
    gv.led[which] = 1
    gv.buzzer = 440 + 110 * which
    uf(1, 250) # sleep
    gv.led[which] = 0
    gv.buzzer = 0

def fail():
    gv.buzzer = 440
    gv.leds = 0x0f
    uf(1, 1000)
    gv.buzzer = 0
    gv.leds = 0

def yippie():
    for x in range(3):
        gv.buzzer = 880
        gv.leds = 0x05
        uf(1, 200)
        gv.buzzer = 660
        gv.leds = 0x0a
        uf(1, 200)
    gv.buzzer = 0
    gv.leds = 0

def main():
    gv.leds = 0
    while True:
        sequence = 0x0022 # uf(7) # random

        # show sequence
        for i in range(8):
            target = (sequence >> (2*i)) & 0x03
            show(target)
            uf(1, 250) # sleep

        for i in range(8):
            target = (sequence >> (2*i)) & 0x03

            nextkeys = 0
            while not nextkeys:
                nextkeys = gv.buttons

                if nextkeys == 1:
                    which = 0
                elif nextkeys == 2:
                    which = 1
                elif nextkeys == 4:
                    which = 2
                elif nextkeys == 8:
                    which = 3
                else:
                    continue
                break

            if which != target:
                fail()
                break
        else:
            yippie()
