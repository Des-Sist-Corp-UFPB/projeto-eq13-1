create table app_user (
    id bigserial primary key,
    name varchar(140) not null,
    email varchar(190) not null,
    username varchar(80),
    password_hash varchar(120) not null,
    role varchar(40) not null,
    provider varchar(40) not null,
    enabled boolean not null default true,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create unique index idx_app_user_email on app_user(email);
create unique index idx_app_user_username on app_user(username);
create index idx_app_user_role on app_user(role);

alter table candidate_application
    add column app_user_id bigint references app_user(id) on delete set null;

create index idx_candidate_application_user on candidate_application(app_user_id);

create table audit_log (
    id bigserial primary key,
    actor_id bigint,
    actor_email varchar(190),
    action varchar(80) not null,
    entity_type varchar(80),
    entity_id varchar(80),
    description text,
    ip_address varchar(80),
    user_agent varchar(500),
    created_at timestamptz not null
);

create index idx_audit_log_created_at on audit_log(created_at desc);
create index idx_audit_log_action on audit_log(action);
create index idx_audit_log_actor_email on audit_log(actor_email);
create index idx_audit_log_entity on audit_log(entity_type, entity_id);
