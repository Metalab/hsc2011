CREATE TABLE EVENTS (
        src TEXT PRIMARY KEY,
	dest TEXT,
	type TEXT,
	ibutton TEXT,
	since DATE,
	accepted BOOL,
	ack BOOL
);
