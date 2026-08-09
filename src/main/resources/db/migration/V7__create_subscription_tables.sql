create table subscription (
    id bigserial primary key,
    company varchar(140) not null,
    company_email varchar(190) not null unique,
    plan_code varchar(120) not null,
    status varchar(40) not null,
    external_reference varchar(120),
    valid_until timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index idx_subscription_company_email on subscription(company_email);
create index idx_subscription_status on subscription(status);