CREATE TABLE LOGINS (
	id INTEGER PRIMARY KEY,
        src TEXT,
	dest TEXT,
	seqnum TEXT,
	ibutton TEXT,
	since DATE,
	accepted BOOL,
	ack BOOL
);
