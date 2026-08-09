alter table app_user
    add column photo_position_x integer not null default 50,
    add column photo_position_y integer not null default 50,
    add column cover_content bytea,
    add column cover_content_type varchar(100),
    add column cover_position_x integer not null default 50,
    add column cover_position_y integer not null default 50;

alter table app_user
    add constraint chk_app_user_photo_position_x check (photo_position_x between 0 and 100),
    add constraint chk_app_user_photo_position_y check (photo_position_y between 0 and 100),
    add constraint chk_app_user_cover_position_x check (cover_position_x between 0 and 100),
    add constraint chk_app_user_cover_position_y check (cover_position_y between 0 and 100);
