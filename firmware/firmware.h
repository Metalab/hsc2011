#ifndef FIRMWARE_H
#define FIRMWARE_H

#include "firmware_vm.h"
#include "firmware_evt.h"
#include "firmware_ser.h"
#include "firmware_net.h"

extern bool do_soft_reset;
extern bool pending_login;
extern bool got_ibutton;

bool poll_ibutton();

#endif
