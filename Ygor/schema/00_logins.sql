CREATE TABLE LOGINS (
        src TEXT PRIMARY KEY,
	dest TEXT,
	ibutton TEXT,
	since DATE,
	accepted BOOL,
	ack BOOL
);
