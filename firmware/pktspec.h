
#ifndef PKTSPEC_H
#define PKTSPEC_H

//
// Protocol details:
//
//   - each buzzer does only communicate to its base station
//
//   - there is a maximum of one pkt in transistion for each
//     buzzer-basestation pair and direction
//
//   - pkts are retransmitted after a timeout. the timeout incrases
//     using an exponential function and has a random component to
//     avoid repeated collisions
//
//   - the login and login_ack pkts are using the sequence number 0.
//     each station incrases the sequence number for each pkt it sends.
//     the sequence numbers are only used to detect duplicates on lost
//     ack packages. in this case the last response is resent but no
//     additional action is performed.
//
//   - the login may be sent to 0:0:0:0:0:0:0:0 in which case the first
//     basestation that acks the pkt is further used as basestation for
//     this session.
//

enum pkttype_e
{
	/* initiated by buzzer, acked by basestation */

	PKTT_LOGIN = 100,
	PKTT_LOGIN_ACK = 101,

	PKTT_EVENT = 110,
	PKTT_EVENT_ACK = 111,

	/* initiated by basestation, acked by buzzer */

	PKTT_STATUS = 200,
	PKTT_STATUS_ACK = 201,

	PKTT_WRITE = 210,
	PKTT_WRITE_ACK = 211,

	PKTT_READ = 220,
	PKTT_READ_ACK = 221
};

enum event_e
{
	ET_BUTTON_DOWN = 1,
	ET_BUTTON_UP = 2,
	ET_USER = 3
};

struct pktbuffer_s
{
	uint8_t pkttype;
	uint16_t seqnum;

	uint8_t src[8]; // sender
	uint8_t dst[8]; // reciever

	union {
		struct {
			int using_ibutton : 1;
		} pkt_login;

		struct {
			/* no payload */
		} pkt_login_ack;

		struct {
			uint8_t event_type;
			uint16_t event_payload; // num of button or user value
		} pkt_event;

		struct {
			/* no payload */
		} pkt_event_ack;

		struct {
			int vm_stop : 1;
			int vm_start : 1;
			int set_ip : 1;
			int set_rgb : 1;
			int set_buzzer : 1;
			int set_leds : 4;
			int clear_leds : 4;
			uint16_t ip_val;
			uint16_t buzzer_val;
			uint8_t rgb_val[3];
			// eventmask: [ MSB keyup3, .., keyup0, keydown3, .., keydown0 LSB ]
			uint8_t eventmask_val;
			uint8_t eventmask_setbits;
		} pkt_status;

		struct {
			int vm_running : 1;
			int leds : 4;
			int button : 4;
			uint16_t ip;
			uint16_t buzzer;
			uint16_t rgb[3];
			uint8_t eventmask;
		} pkt_status_ack;

		struct {
			int datamem : 1;
			int length : 6;
			uint16_t addr;
			uint8_t data[64];
		} pkt_write;

		struct {
			/* no payload */
		} pkt_write_ack;

		struct {
			int datamem : 1;
			int length : 6;
			uint16_t addr;
		} pkt_read;

		struct {
			int datamem : 1;
			int length : 6;
			uint16_t addr;
			uint8_t data[64];
		} pkt_read_ack;
	};
};

#endif

