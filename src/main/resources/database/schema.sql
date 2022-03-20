CREATE TABLE IF NOT EXISTS Users
(
    id           bigserial PRIMARY KEY,
    name         varchar(30)  NOT NULL,
    surname      varchar(30)  NOT NULL,
    email        varchar(50)  NOT NULL UNIQUE,
    salt         bytea        NOT NULL,
    passwordHash varchar(128) NOT NULL,
    avatarUrl    varchar(200)
);

CREATE TABLE IF NOT EXISTS Chats
(
    id        bigserial PRIMARY KEY,
    name      varchar(100) NOT NULL,
    avatarUrl varchar(200)
);

--Create type
DO
'
    DECLARE
    BEGIN
        CREATE TYPE message_type AS ENUM (
            ''DEFAULT'',
            ''TEXT'',
            ''IMAGE'',
            ''VIDEO'',
            ''FILE''
            );
    EXCEPTION
        WHEN duplicate_object THEN null;
    END' LANGUAGE PLPGSQL;

CREATE TABLE IF NOT EXISTS Messages
(
    id           uuid PRIMARY KEY,
    chatId       bigint    NOT NULL references Chats (id) ON DELETE CASCADE ON UPDATE CASCADE,
    authorId     bigint    NOT NULL references Users (id) ON DELETE CASCADE ON UPDATE CASCADE,
    creationTime timestamp NOT NULL,
    type         message_type default 'DEFAULT',
    -- Currently we only store TEXT
    value        text      NOT NULL
);

CREATE TABLE IF NOT EXISTS UsersChats
(
    userId bigint NOT NULL references Users (id) ON DELETE CASCADE ON UPDATE CASCADE,
    chatId bigint NOT NULL references Chats (id) ON DELETE CASCADE ON UPDATE CASCADE,
    PRIMARY KEY (userId, chatId)
)

