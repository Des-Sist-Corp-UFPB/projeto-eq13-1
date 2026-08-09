-- Curadoria confirmada em 09/08/2026. Somente anúncios publicados de 03/08 a 07/08/2026.
-- As vagas anteriores são arquivadas, nunca removidas, preservando histórico e candidaturas.
with archived as (
    update job_posting
       set status = 'ARCHIVED', updated_at = now()
     where status = 'PUBLISHED'
       and (created_at < timestamptz '2026-08-03 00:00:00+00'
            or created_at >= timestamptz '2026-08-08 00:00:00+00')
    returning id
)
insert into audit_log (actor_email, action, entity_type, entity_id, description, created_at)
select 'curadoria@radartech.dev', 'JOB_CATALOG_ARCHIVED', 'JOB_POSTING', cast(id as varchar),
       'Vaga anterior preservada como arquivada na atualização do catálogo de agosto.', now()
from archived;

insert into job_posting
(title, company, company_email, location_type, city, seniority, contract_type, salary_range,
 description, requirements, apply_url, status, views, created_at, updated_at)
select v.title, v.company, 'curadoria@radartech.dev', v.location_type, v.city, v.seniority,
       v.contract_type, 'Não informado', v.description, v.requirements, v.apply_url,
       'PUBLISHED', 0, v.published_at, v.published_at
from (values
    ('Engenheiro de Software - IT Cash Management', 'BTG Pactual', 'PRESENTIAL_PB', 'Rio de Janeiro - RJ', 'MID_LEVEL', 'CLT',
     'Desenvolvimento e evolução de serviços financeiros, APIs e microsserviços de alta disponibilidade. Fonte: LinkedIn.',
     'C#/.NET, APIs REST, microsserviços, bancos relacionais e não relacionais, Git e AWS.',
     'https://br.linkedin.com/jobs/view/4450761147', timestamptz '2026-08-07 12:00:00+00'),
    ('Analista de Segurança da Informação', 'Amo Promo', 'HYBRID_PB', 'Belo Horizonte - MG', 'MID_LEVEL', 'CLT',
     'Atuação em segurança da informação, análise de riscos e proteção dos ambientes corporativos. Fonte: LinkedIn.',
     'Fundamentos de segurança, gestão de vulnerabilidades, controles de acesso e resposta a incidentes.',
     'https://br.linkedin.com/jobs/view/4450775581', timestamptz '2026-08-07 12:05:00+00'),
    ('Estágio em Tecnologia da Informação', 'Universia Brasil', 'PRESENTIAL_PB', 'São Paulo - SP', 'INTERNSHIP', 'INTERNSHIP',
     'Oportunidade de aprendizagem e apoio às rotinas de tecnologia da informação. Fonte: LinkedIn.',
     'Graduação em tecnologia em andamento, interesse por suporte e disponibilidade para estágio.',
     'https://br.linkedin.com/jobs/view/4447588835', timestamptz '2026-08-07 12:10:00+00'),
    ('Estágio em Desenvolvimento de Software', 'Samsung Brasil', 'HYBRID_PB', 'Campinas - SP', 'INTERNSHIP', 'INTERNSHIP',
     'Participação no desenvolvimento e na validação de soluções de software em ambiente de inovação. Fonte: LinkedIn.',
     'Curso superior em computação ou área relacionada, lógica de programação e vontade de aprender.',
     'https://br.linkedin.com/jobs/view/4450297759', timestamptz '2026-08-06 12:00:00+00'),
    ('Estágio em Suporte Técnico', 'ctasmart', 'HYBRID_PB', 'Porto Alegre - RS', 'INTERNSHIP', 'INTERNSHIP',
     'Apoio técnico a usuários, registro de chamados e manutenção do ambiente de trabalho. Fonte: LinkedIn.',
     'Formação em TI em andamento, comunicação clara e conhecimentos básicos de hardware e sistemas.',
     'https://br.linkedin.com/jobs/view/4449139641', timestamptz '2026-08-05 12:00:00+00'),
    ('Técnico de Suporte', 'Casa Thomas Jefferson', 'PRESENTIAL_PB', 'Brasília - DF', 'JUNIOR', 'CLT',
     'Suporte a usuários e equipamentos, diagnóstico de incidentes e apoio à infraestrutura local. Fonte: LinkedIn.',
     'Conhecimentos de suporte, redes, sistemas operacionais e atendimento ao usuário.',
     'https://br.linkedin.com/jobs/view/4449693556', timestamptz '2026-08-05 12:05:00+00'),
    ('Estágio em Tecnologia SAP', 'T-Systems do Brasil', 'HYBRID_PB', 'São Paulo - SP', 'INTERNSHIP', 'INTERNSHIP',
     'Formação prática em consultoria funcional e desenvolvimento de soluções empresariais SAP. Fonte: LinkedIn.',
     'Graduação em tecnologia ou negócios em andamento, raciocínio lógico e interesse por SAP.',
     'https://br.linkedin.com/jobs/view/4448539070', timestamptz '2026-08-04 12:00:00+00'),
    ('Desenvolvedor Full Stack Júnior', 'Claro Brasil', 'HYBRID_PB', 'Recife - PE', 'JUNIOR', 'CLT',
     'Desenvolvimento e manutenção de aplicações web em equipe multidisciplinar. Fonte: LinkedIn.',
     'Fundamentos de front-end, back-end, APIs, bancos de dados e controle de versão.',
     'https://br.linkedin.com/jobs/view/4449060757', timestamptz '2026-08-04 12:05:00+00'),
    ('Senior Cloud Platform Engineer', 'StoneX Group', 'HYBRID_PB', 'São Paulo - SP', 'SENIOR', 'CLT',
     'Engenharia de plataforma cloud com foco em confiabilidade, automação e escala. Fonte: LinkedIn.',
     'Experiência em cloud, infraestrutura como código, containers, CI/CD e observabilidade.',
     'https://br.linkedin.com/jobs/view/4446215807', timestamptz '2026-08-03 12:00:00+00'),
    ('Estágio de Desenvolvimento', 'Grupo Pibernat', 'PRESENTIAL_PB', 'Canoas - RS', 'INTERNSHIP', 'INTERNSHIP',
     'Apoio ao desenvolvimento de sistemas e evolução de funcionalidades internas. Fonte: LinkedIn.',
     'Curso de tecnologia em andamento, lógica de programação e familiaridade com desenvolvimento web.',
     'https://br.linkedin.com/jobs/view/4448623461', timestamptz '2026-08-03 12:05:00+00'),
    ('Product Designer Sênior', 'Sympla', 'REMOTE', null, 'SENIOR', 'CLT',
     'Design de produtos digitais B2B, da descoberta à entrega, em colaboração com produto e engenharia. Fonte: Remotar/InHire.',
     'Pesquisa, prototipação, design systems, métricas de produto e portfólio consistente.',
     'https://sympla.inhire.app/vagas/a0dc9139-611f-4600-9b09-d4c5a64a85ad/product-designer-senior-or-b2b-or-remoto', timestamptz '2026-08-07 13:00:00+00'),
    ('Desenvolvedor Front-End Júnior ou Pleno', 'Cuponomia', 'REMOTE', null, 'JUNIOR', 'CLT',
     'Desenvolvimento de experiências web e evolução do produto em ambiente remoto. Fonte: Remotar/Gupy.',
     'JavaScript ou TypeScript, HTML, CSS, framework de front-end, Git e integração com APIs.',
     'https://cuponomia.gupy.io/job/eyJqb2JJZCI6MTE5ODc0NzgsInNvdXJjZSI6InJlbW90YXIifQ==?jobBoardSource=remotar', timestamptz '2026-08-07 13:05:00+00'),
    ('Desenvolvedor Back-end Sênior', 'Dotz', 'REMOTE', null, 'SENIOR', 'CLT',
     'Construção e evolução de serviços back-end escaláveis para produtos digitais. Fonte: Remotar/Gupy.',
     'Experiência sólida em APIs, arquitetura de serviços, bancos de dados, testes e práticas de entrega contínua.',
     'https://dotz.gupy.io/job/eyJqb2JJZCI6MTE5NDMyOTEsInNvdXJjZSI6InJlbW90YXIifQ==?jobBoardSource=remotar', timestamptz '2026-08-07 13:10:00+00'),
    ('Analista de Automação de Testes Sênior', 'Stefanini Group', 'REMOTE', null, 'SENIOR', 'CLT',
     'Automação de testes e melhoria contínua da qualidade em produtos digitais. Fonte: Remotar/Gupy.',
     'Automação de testes, testes de API e interface, integração contínua e estratégia de qualidade.',
     'https://stefanini.gupy.io/job/eyJqb2JJZCI6MTE5ODI3MTIsInNvdXJjZSI6InJlbW90YXIifQ==?jobBoardSource=remotar', timestamptz '2026-08-07 13:15:00+00'),
    ('Data Engineer Sênior', 'Avenue', 'REMOTE', null, 'SENIOR', 'CLT',
     'Desenvolvimento de pipelines e plataformas de dados confiáveis para produtos financeiros. Fonte: Remotar/InHire.',
     'Engenharia de dados, SQL, pipelines, cloud, modelagem e observabilidade de dados.',
     'https://avenue.inhire.app/vagas/668128ba-b746-4629-a40b-5c0cb2fee6f7/data-engineer-senior', timestamptz '2026-08-06 13:00:00+00'),
    ('Engenheiro Cloud Pleno', 'MTP Métodos e Tecnologia', 'REMOTE', null, 'MID_LEVEL', 'CLT',
     'Evolução de plataforma cloud, automação de infraestrutura e suporte aos times de engenharia. Fonte: Remotar/Gupy.',
     'Cloud, infraestrutura como código, containers, redes, CI/CD e monitoramento.',
     'https://mtpbrasil.gupy.io/job/eyJqb2JJZCI6MTEyNzM1NzYsInNvdXJjZSI6InJlbW90YXIifQ==?jobBoardSource=remotar', timestamptz '2026-08-06 13:05:00+00'),
    ('UI/UX Designer', 'MJV', 'REMOTE', null, 'MID_LEVEL', 'CLT',
     'Design de interfaces web e mobile com foco em experiência, consistência e design system. Fonte: Remotar/InHire.',
     'Pesquisa, arquitetura de informação, prototipação, acessibilidade e ferramentas de design.',
     'https://mjv.inhire.app/vagas/4d6b50fa-dfd3-4dae-89d6-2ca455012df0/uiux-designer-foco-em-webmobile-and-design-system', timestamptz '2026-08-06 13:10:00+00'),
    ('Desenvolvedor Mobile Flutter', 'MED-REVIEW', 'REMOTE', null, 'MID_LEVEL', 'PJ',
     'Desenvolvimento e manutenção de aplicativo móvel multiplataforma. Fonte: Remotar/Sólides.',
     'Flutter, Dart, consumo de APIs, gerenciamento de estado, Git e publicação de aplicativos.',
     'https://medreview.vagas.solides.com.br/vaga/898042?origem=remotar', timestamptz '2026-08-05 13:00:00+00'),
    ('Senior Full Stack Developer', 'Galaxies', 'REMOTE', null, 'SENIOR', 'CLT',
     'Construção de produtos web completos com participação em decisões técnicas e de arquitetura. Fonte: Remotar/InHire.',
     'Experiência full stack, APIs, banco de dados, testes automatizados e boas práticas de engenharia.',
     'https://galaxies.inhire.app/vagas/c2db8cb8-e139-439b-a46c-313ef51672f6/senior-full-stack-developer', timestamptz '2026-08-04 13:00:00+00'),
    ('Remote Senior Fullstack JavaScript Developer', 'Scopic Software', 'REMOTE', null, 'SENIOR', 'CLT',
     'Desenvolvimento full stack remoto para produtos internacionais e equipes distribuídas. Fonte: Remotar/Zoho Recruit.',
     'JavaScript, front-end e back-end modernos, inglês, Git, testes e colaboração remota.',
     'https://scopicsoftware.zohorecruit.com/jobs/Careers/741923000041731045/Remote-Senior-Fullstack-JavaScript-Developer?source=Remotar', timestamptz '2026-08-03 13:00:00+00')
) as v(title, company, location_type, city, seniority, contract_type, description, requirements, apply_url, published_at)
where not exists (select 1 from job_posting j where j.apply_url = v.apply_url);

insert into audit_log
(actor_email, action, entity_type, entity_id, description, created_at)
select 'curadoria@radartech.dev', 'JOB_CURATED', 'JOB_POSTING', cast(j.id as varchar),
       'Vaga de tecnologia verificada, publicada entre 03/08/2026 e 07/08/2026 e adicionada à curadoria.', now()
from job_posting j
where j.apply_url in (
    'https://br.linkedin.com/jobs/view/4450761147',
    'https://br.linkedin.com/jobs/view/4450775581',
    'https://br.linkedin.com/jobs/view/4447588835',
    'https://br.linkedin.com/jobs/view/4450297759',
    'https://br.linkedin.com/jobs/view/4449139641',
    'https://br.linkedin.com/jobs/view/4449693556',
    'https://br.linkedin.com/jobs/view/4448539070',
    'https://br.linkedin.com/jobs/view/4449060757',
    'https://br.linkedin.com/jobs/view/4446215807',
    'https://br.linkedin.com/jobs/view/4448623461',
    'https://sympla.inhire.app/vagas/a0dc9139-611f-4600-9b09-d4c5a64a85ad/product-designer-senior-or-b2b-or-remoto',
    'https://cuponomia.gupy.io/job/eyJqb2JJZCI6MTE5ODc0NzgsInNvdXJjZSI6InJlbW90YXIifQ==?jobBoardSource=remotar',
    'https://dotz.gupy.io/job/eyJqb2JJZCI6MTE5NDMyOTEsInNvdXJjZSI6InJlbW90YXIifQ==?jobBoardSource=remotar',
    'https://stefanini.gupy.io/job/eyJqb2JJZCI6MTE5ODI3MTIsInNvdXJjZSI6InJlbW90YXIifQ==?jobBoardSource=remotar',
    'https://avenue.inhire.app/vagas/668128ba-b746-4629-a40b-5c0cb2fee6f7/data-engineer-senior',
    'https://mtpbrasil.gupy.io/job/eyJqb2JJZCI6MTEyNzM1NzYsInNvdXJjZSI6InJlbW90YXIifQ==?jobBoardSource=remotar',
    'https://mjv.inhire.app/vagas/4d6b50fa-dfd3-4dae-89d6-2ca455012df0/uiux-designer-foco-em-webmobile-and-design-system',
    'https://medreview.vagas.solides.com.br/vaga/898042?origem=remotar',
    'https://galaxies.inhire.app/vagas/c2db8cb8-e139-439b-a46c-313ef51672f6/senior-full-stack-developer',
    'https://scopicsoftware.zohorecruit.com/jobs/Careers/741923000041731045/Remote-Senior-Fullstack-JavaScript-Developer?source=Remotar'
)
and not exists (
    select 1 from audit_log a
    where a.action = 'JOB_CURATED' and a.entity_id = cast(j.id as varchar)
);
