-- Curadoria verificada em 17/07/2026. Os links apontam para as publicações originais.
insert into job_posting
(title, company, company_email, location_type, city, seniority, contract_type, salary_range,
 description, requirements, apply_url, status, views, created_at, updated_at)
select 'Desenvolvedor(a) de Software Pleno | Frontend', 'Smartspace', 'curadoria@radartechpb.dev',
       'PRESENTIAL_PB', 'João Pessoa', 'MID_LEVEL', 'CLT', 'Não informado',
       'Atuação no desenvolvimento de interfaces modernas, acessíveis e escaláveis para soluções SaaS.',
       'JavaScript ES6+, TypeScript, desenvolvimento front-end, acessibilidade e trabalho colaborativo.',
       'https://br.linkedin.com/jobs/view/desenvolvedor-a-de-software-pleno-frontend-at-smartspace-4429272000',
       'PUBLISHED', 0, now() - interval '6 days', now() - interval '6 days'
where not exists (
    select 1 from job_posting where apply_url = 'https://br.linkedin.com/jobs/view/desenvolvedor-a-de-software-pleno-frontend-at-smartspace-4429272000'
);

insert into job_posting
(title, company, company_email, location_type, city, seniority, contract_type, salary_range,
 description, requirements, apply_url, status, views, created_at, updated_at)
select 'Arquiteto(a) de Software | Especialista | Tech Lead', 'Accenture Brasil',
       'curadoria@radartechpb.dev', 'PRESENTIAL_PB', 'Campina Grande', 'LEAD', 'CLT', 'Não informado',
       'Liderança técnica e arquitetura de soluções em projetos de transformação digital de alta complexidade.',
       'Experiência em arquitetura de software, liderança técnica, integração de sistemas e soluções escaláveis.',
       'https://br.linkedin.com/jobs/view/arquiteto-a-de-software-especialista-tech-lead-at-accenture-brasil-4328049652',
       'PUBLISHED', 0, now() - interval '7 days', now() - interval '7 days'
where not exists (
    select 1 from job_posting where apply_url = 'https://br.linkedin.com/jobs/view/arquiteto-a-de-software-especialista-tech-lead-at-accenture-brasil-4328049652'
);

insert into job_posting
(title, company, company_email, location_type, city, seniority, contract_type, salary_range,
 description, requirements, apply_url, status, views, created_at, updated_at)
select 'Desenvolvedor React Junior - Trabalho Remoto', 'BairesDev', 'curadoria@radartechpb.dev',
       'REMOTE', null, 'JUNIOR', 'CLT', 'Não informado',
       'Oportunidade 100% remota para desenvolvimento de aplicações React em uma equipe global de tecnologia.',
       'Um ano de experiência com React, algoritmos, estruturas de dados, Git, aprendizado rápido e inglês.',
       'https://br.linkedin.com/jobs/view/desenvolvedor-react-junior-trabalho-remoto-at-bairesdev-4394711048',
       'PUBLISHED', 0, now() - interval '1 day', now() - interval '1 day'
where not exists (
    select 1 from job_posting where apply_url = 'https://br.linkedin.com/jobs/view/desenvolvedor-react-junior-trabalho-remoto-at-bairesdev-4394711048'
);

insert into job_posting
(title, company, company_email, location_type, city, seniority, contract_type, salary_range,
 description, requirements, apply_url, status, views, created_at, updated_at)
select 'Desenvolvedor(a) Front-End React', 'Stefanini Brasil', 'curadoria@radartechpb.dev',
       'REMOTE', null, 'MID_LEVEL', 'CLT', 'Não informado',
       'Atuação 100% remota em projetos de front-end, com ambiente colaborativo e oportunidade de crescimento.',
       'React.js, JavaScript, TypeScript, HTML5, CSS3, APIs REST, Git e metodologias ágeis.',
       'https://br.linkedin.com/jobs/view/desenvolvedor-de-front-end-react-at-stefanini-brasil-4430966510',
       'PUBLISHED', 0, now() - interval '1 day', now() - interval '1 day'
where not exists (
    select 1 from job_posting where apply_url = 'https://br.linkedin.com/jobs/view/desenvolvedor-de-front-end-react-at-stefanini-brasil-4430966510'
);

insert into job_posting
(title, company, company_email, location_type, city, seniority, contract_type, salary_range,
 description, requirements, apply_url, status, views, created_at, updated_at)
select 'Desenvolvedor Fullstack Angular / IA', 'BRQ Digital Solutions', 'curadoria@radartechpb.dev',
       'REMOTE', null, 'MID_LEVEL', 'CLT', 'Não informado',
       'Vaga remota para desenvolvimento full stack em projetos de alta complexidade com aplicação de inteligência artificial.',
       'Angular, desenvolvimento de APIs, integração de sistemas, práticas ágeis e fundamentos de IA.',
       'https://br.linkedin.com/jobs/view/desenvolvedor-fullstack-angular-ia-at-brq-digital-solutions-4440595464',
       'PUBLISHED', 0, now() - interval '1 day', now() - interval '1 day'
where not exists (
    select 1 from job_posting where apply_url = 'https://br.linkedin.com/jobs/view/desenvolvedor-fullstack-angular-ia-at-brq-digital-solutions-4440595464'
);

insert into audit_log
(actor_email, action, entity_type, entity_id, description, created_at)
select 'curadoria@radartechpb.dev', 'JOB_CURATED', 'JOB_POSTING', cast(j.id as varchar),
       'Vaga recente verificada e adicionada pela curadoria em 17/07/2026.', now()
from job_posting j
where j.apply_url in (
    'https://br.linkedin.com/jobs/view/desenvolvedor-a-de-software-pleno-frontend-at-smartspace-4429272000',
    'https://br.linkedin.com/jobs/view/arquiteto-a-de-software-especialista-tech-lead-at-accenture-brasil-4328049652',
    'https://br.linkedin.com/jobs/view/desenvolvedor-react-junior-trabalho-remoto-at-bairesdev-4394711048',
    'https://br.linkedin.com/jobs/view/desenvolvedor-de-front-end-react-at-stefanini-brasil-4430966510',
    'https://br.linkedin.com/jobs/view/desenvolvedor-fullstack-angular-ia-at-brq-digital-solutions-4440595464'
)
and not exists (
    select 1 from audit_log a
    where a.action = 'JOB_CURATED' and a.entity_id = cast(j.id as varchar)
);
