-- dest, seqnum, type
SELECT * from OUTGOING where dest=? and seqnum=? and type=? order by date asc limit 1;
