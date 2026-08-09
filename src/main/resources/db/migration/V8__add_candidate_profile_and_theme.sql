alter table app_user
    add column biography text,
    add column theme_preference varchar(20) not null default 'SYSTEM',
    add column photo_content bytea,
    add column photo_content_type varchar(100),
    add column resume_content bytea,
    add column resume_file_name varchar(255),
    add column resume_content_type varchar(100);

create table candidate_experience (
    id bigserial primary key,
    app_user_id bigint not null references app_user(id) on delete cascade,
    role_title varchar(140) not null,
    company varchar(140) not null,
    started_on date not null,
    ended_on date,
    description text,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint chk_candidate_experience_period check (ended_on is null or ended_on >= started_on)
);

create index idx_candidate_experience_user_started
    on candidate_experience(app_user_id, started_on desc);
