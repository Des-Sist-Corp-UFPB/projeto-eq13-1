alter table app_user alter column theme_preference set default 'LIGHT';

-- Prefer the white RadarTech identity for accounts created before theme preferences existed.
update app_user set theme_preference = 'LIGHT' where theme_preference = 'SYSTEM';
