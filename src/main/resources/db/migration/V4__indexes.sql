-- src/main/resources/db/migration/V4__indexes.sql
ALTER TABLE matches ADD INDEX idx_matches_room (room_id);
ALTER TABLE room_members ADD INDEX idx_room_members_user (user_id);
