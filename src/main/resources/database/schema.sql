-- Users table
CREATE TABLE IF NOT EXISTS Users
(
    id           bigserial PRIMARY KEY,
    name         varchar(30)  NOT NULL,
    surname      varchar(30)  NOT NULL,
    email        varchar(50)  NOT NULL UNIQUE,
    salt         bytea,
    passwordHash varchar(128) NOT NULL,
    avatarUrl    varchar(200)
);

ALTER TABLE IF EXISTS Users ALTER COLUMN salt DROP NOT NULL;

CREATE OR REPLACE FUNCTION all_users_chat_id() RETURNS BIGINT AS '
BEGIN
    RETURN 6;
END;
'
LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION add_user_to_all_users_chat() RETURNS TRIGGER AS '
BEGIN
    IF (TG_OP = ''INSERT'') THEN
        INSERT INTO UsersChats (userId, chatId) VALUES (NEW.id, all_users_chat_id());
        RETURN NEW;
    END IF;
END;
'
LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER add_user_to_all_users_chat_trg AFTER INSERT ON Users
    FOR EACH ROW EXECUTE PROCEDURE add_user_to_all_users_chat();


-- Chats table
CREATE TABLE IF NOT EXISTS Chats
(
    id        bigserial PRIMARY KEY,
    name      varchar(100) NOT NULL,
    avatarUrl varchar(200),
    ------------------------ Columns to have been added after initializing basic schema
    lastChanged timestamp -- supposed to be the time when last message was sent
);
-- Should we create index on LastChanged if it's changed often?
ALTER TABLE IF EXISTS Chats ADD COLUMN IF NOT EXISTS lastChanged timestamp NOT NULL DEFAULT now()::timestamp;
ALTER TABLE IF EXISTS Chats ADD COLUMN IF NOT EXISTS private boolean;

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


-- Message table
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

CREATE OR REPLACE FUNCTION update_chat_last_changed_column() RETURNS TRIGGER AS '
BEGIN
    IF (TG_OP = ''INSERT'') OR (TG_OP = ''UPDATE'') THEN
        UPDATE Chats
        SET lastChanged = NEW.creationTime
        WHERE id = NEW.chatId;
        RETURN NEW;
    ELSE IF (TG_OP = ''DELETE'') THEN
        UPDATE Chats
        SET lastChanged = OLD.creationTime
        WHERE id = NEW.chatId;
        RETURN OLD;
    end if;
    END IF;
END;
'LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER update_chat_last_changed_column_trg AFTER INSERT OR UPDATE OR DELETE on Messages
    FOR EACH ROW EXECUTE PROCEDURE update_chat_last_changed_column();


-- UsersChats table
CREATE TABLE IF NOT EXISTS UsersChats
(
    userId bigint NOT NULL references Users (id) ON DELETE CASCADE ON UPDATE CASCADE,
    chatId bigint NOT NULL references Chats (id) ON DELETE CASCADE ON UPDATE CASCADE,
    PRIMARY KEY (userId, chatId)
);



