create table Events (
    timestamp char(28) primary key,
    mac char(5),
    button_green bool,
    button_red bool,
    button_yellow bool,
    button_blue bool
);
