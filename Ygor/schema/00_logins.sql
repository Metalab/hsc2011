CREATE TABLE LOGINS (
        src TEXT PRIMARY KEY,
	dest TEXT,
	seqnum TEXT,
	ibutton TEXT,
	since TIME,
	accepted BOOL,
	ack BOOL
);
