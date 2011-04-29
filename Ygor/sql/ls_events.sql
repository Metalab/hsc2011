-- src
select e.ROWID, e.* from EVENTS e,LOGINS l where l.src=? and e.src=l.src and l.accepted="true"

