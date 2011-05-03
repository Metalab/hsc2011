from embedvm.runtime import Globals, UserfuncWrapper
import time
'''
from embedvm.runtime import trampolined
'''

memmapped = Globals()
memmapped.led = memmapped.array8u(0, length=4)
memmapped.leds = memmapped.int8u(4)
memmapped.rgb = memmapped.array8u(5, length=3)
memmapped.button = memmapped.array8u(8, length=4)
memmapped.buttons = memmapped.int8u(12)
memmapped.buzzer = memmapped.int16(13)
memmapped.padding = memmapped.int8u(15)

@UserfuncWrapper(which=1)
def sleep(millis):
    time.sleep(millis/1000.0)

'''
@trampolined(address=0x3000)
def kitt():
    print "Doing a KITT style animation over the LEDs."
    '''
