-- src
select EVENTS.* from EVENTS e,LOGINS l where l.src=? and e.src=l.src and l.accepted=true

