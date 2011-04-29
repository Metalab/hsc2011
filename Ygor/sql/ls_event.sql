-- loginid
select EVENTS.* from EVENTS e,LOGINS l where l.ROWID=? and e.src=l.src and l.accepted=true

