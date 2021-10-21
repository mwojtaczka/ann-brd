CREATE KEYSPACE IF NOT EXISTS announcement_board WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

CREATE TYPE IF NOT EXISTS announcement_board.comment
(
    author_id uuid,
    content text,
    creation_time timestamp
);

CREATE TABLE IF NOT EXISTS announcement_board.announcement
(
    author_id  uuid,
    creation_time timestamp,
    content text,
    comments list<FROZEN <comment>>,
    PRIMARY KEY ((author_id), creation_time)
);

CREATE TABLE IF NOT EXISTS announcement_board.user
(
    id  uuid,
    nickname  text,
    name text,
    surname text,
    PRIMARY KEY (id)
);
