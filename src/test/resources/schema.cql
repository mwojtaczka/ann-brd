CREATE KEYSPACE announcement_board WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

CREATE TYPE announcement_board.comment
(
    author_id uuid,
    content text,
    creation_time timestamp
);

CREATE TABLE announcement_board.announcement
(
    announcer_id  uuid,
    creation_time timestamp,
    content text,
    comments list<FROZEN <comment>>,
    PRIMARY KEY ((announcer_id), creation_time)
);

CREATE TABLE announcement_board.user
(
    id  uuid,
    nickname  text,
    name text,
    surname text,
    PRIMARY KEY (id)
);
