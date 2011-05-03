from edubuzzer import memmapped as mm, sleep

def show(which):
    mm.led[which] = 1
    mm.buzzer = 440 + 110 * which
    sleep(250)
    mm.led[which] = 0
    mm.buzzer = 0

def fail():
    mm.buzzer = 440
    mm.leds = 0x0f
    sleep(1000)
    mm.buzzer = 0
    mm.leds = 0

def yippie():
    for x in range(3):
        mm.buzzer = 880
        mm.leds = 0x05
        sleep(200)
        mm.buzzer = 660
        mm.leds = 0x0a
        sleep(200)
    sleep(500)
    mm.buzzer = 0
    mm.leds = 0

    # kitt()

def main():
    mm.leds = 0
    while True:
        sequence = 0x0022 # uf(7) # random

        # show sequence
        for i in range(8):
            target = (sequence >> (2*i)) & 0x03
            show(target)
            sleep(250)

        for i in range(8):
            target = (sequence >> (2*i)) & 0x03

            while True:
                nextkeys = mm.buttons

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

            while mm.buttons:
                pass

            show(which);
        else:
            yippie()

if __name__ == "__main__":
    main()
