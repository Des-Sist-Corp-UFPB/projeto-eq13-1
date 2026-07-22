create table job_posting (
    id bigserial primary key,
    title varchar(160) not null,
    company varchar(140) not null,
    company_email varchar(190) not null,
    location_type varchar(40) not null,
    city varchar(80),
    seniority varchar(40) not null,
    contract_type varchar(40) not null,
    salary_range varchar(80),
    description text not null,
    requirements text,
    apply_url varchar(500),
    status varchar(40) not null,
    views integer not null default 0,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table candidate_application (
    id bigserial primary key,
    job_id bigint not null references job_posting(id) on delete cascade,
    applicant_name varchar(140) not null,
    applicant_email varchar(190) not null,
    linkedin_url varchar(500),
    message text,
    created_at timestamptz not null
);

create index idx_job_posting_status_created_at on job_posting(status, created_at desc);
create index idx_job_posting_location_status on job_posting(location_type, status);
create index idx_candidate_application_job on candidate_application(job_id);
