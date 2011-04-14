#ifndef FIRMWARE_NET_H
#define FIRMWARE_NET_H

#include "pktspec.h"

extern uint8_t sendbuf_len;
extern struct pktbuffer_s sendbuf;
extern struct pktbuffer_s recvbuf;

extern bool basestation;
extern uint8_t ib_addr[8];
extern uint8_t my_addr[8];
extern uint8_t base_addr[8];
extern uint8_t last_send_seq;
extern uint16_t last_ping;
#define NET_PING_TIMEOUT 30000

void net_proc();
bool net_poll();
void net_send();
bool net_send_until_acked(uint8_t ack_type);

void net_reset();

#endif
