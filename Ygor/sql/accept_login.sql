-- src, dest, seqnum
update LOGINS set accepted="true" where src = ? and dest = ? and seqnum = ? and accepted="false"
