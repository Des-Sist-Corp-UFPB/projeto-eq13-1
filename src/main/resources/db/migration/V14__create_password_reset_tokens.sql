create table password_reset_token (
    id bigserial primary key,
    app_user_id bigint not null references app_user(id) on delete cascade,
    token_hash varchar(64) not null,
    expires_at timestamptz not null,
    used_at timestamptz,
    created_at timestamptz not null
);

create unique index idx_password_reset_token_hash on password_reset_token(token_hash);
create index idx_password_reset_token_user_active on password_reset_token(app_user_id, used_at);
create index idx_password_reset_token_expiry on password_reset_token(expires_at);
