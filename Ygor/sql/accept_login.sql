-- rowid
update LOGINS set accepted="true" where ROWID = ? and accepted="false"
