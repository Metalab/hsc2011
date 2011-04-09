#ifndef FIRMWARE_SER_H
#define FIRMWARE_SER_H

#include <WProgram.h>

uint8_t ser_readbyte();
void ser_endecho();
void ser_readeol();
uint8_t ser_readhex();
uint16_t ser_readhex16();
void ser_readmac(uint8_t *buf);
bool ser_readbool();
int ser_readtri();
int ser_readeventtype();

void ser_printhex(uint8_t val);
void ser_printhex16(uint16_t val);
void ser_printmac(uint8_t *buf);
void ser_printpkt(struct pktbuffer_s *pkt);

void ser_poll();

#endif
