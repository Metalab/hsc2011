-- id
update VMEVENTS set ack = "true" where ROWID=? and ack = "false"

