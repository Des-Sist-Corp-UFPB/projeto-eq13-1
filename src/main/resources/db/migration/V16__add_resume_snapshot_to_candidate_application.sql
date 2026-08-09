alter table candidate_application
    add column resume_content bytea,
    add column resume_file_name varchar(255),
    add column resume_content_type varchar(100);
