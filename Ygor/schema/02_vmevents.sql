create table VMEVENTS (
	src TEXT,
	dest TEXT,
	seqnum TEXT,

	start BOOL,
	stop BOOL,
	
	set_ip BOOL,
	ip TEXT,

	set_rgb BOOL,
	rgb TEXT,

	set_buzzer BOOL,
	buzzer TEXT,

	led0 TEXT,
	led1 TEXT,
	led2 TEXT,
	led3 TEXT,

	eventmask TEXT,
	eventmaskmak TEXT,
	date TIME,
	ack BOOL
);
