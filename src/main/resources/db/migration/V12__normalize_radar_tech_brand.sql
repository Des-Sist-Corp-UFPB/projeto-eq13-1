-- Mantém o histórico das migrations anteriores intacto e normaliza a marca
-- nos registros que já possam existir em produção.
update job_posting
set company_email = 'curadoria@radartech.dev'
where lower(company_email) = 'curadoria@radartechpb.dev';

update audit_log
set actor_email = 'curadoria@radartech.dev'
where lower(actor_email) = 'curadoria@radartechpb.dev';
