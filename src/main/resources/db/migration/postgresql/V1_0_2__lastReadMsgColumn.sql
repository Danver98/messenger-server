alter table "UsersChats" add column "lastReadMsg" UUID
references "Messages" (id) ON DELETE SET NULL ON UPDATE CASCADE;