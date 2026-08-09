# Radar Tech

O Radar Tech é uma plataforma de oportunidades de tecnologia que conecta candidatos, empresas e curadores de vagas em uma experiência única: descoberta de oportunidades, candidatura interna, perfil profissional, divulgação patrocinada e moderação administrativa.

A aplicação está disponível em [https://eq13.dsc.rodrigor.com](https://eq13.dsc.rodrigor.com).

## O produto

O sistema foi projetado em torno de três jornadas:

- **Candidatos** pesquisam vagas, criam conta, personalizam o perfil, registram experiências, enviam currículo em PDF, candidatam-se e usam um assistente de carreira.
- **Empresas e divulgadores** autenticados contratam um plano pelo Stripe e enviam oportunidades para moderação.
- **Administradores** acompanham indicadores, moderam vagas, consultam candidaturas, usuários e a trilha de auditoria.

Isso transforma o Radar Tech em uma plataforma de recrutamento e curadoria completa. Autenticação, autorização por papéis, pagamentos, IA, auditoria persistente, armazenamento de perfil, observabilidade e análise de uso fazem parte do fluxo do produto.

## Experiência visual

O frontend usa um design system próprio em azul-marinho e turquesa, com bastante espaço em branco, tipografia profissional, cards consistentes e componentes responsivos. A mesma identidade cobre home, busca e detalhes de vagas, autenticação, perfil profissional, assinatura e toda a administração.

- hero corporativo com busca real por cargo, tecnologia, empresa e modelo de trabalho;
- cabeçalho responsivo com menu móvel, conta, administração e publicação de vagas;
- componentes reutilizáveis para vagas, formulários, filtros, indicadores, tabelas e estados vazios;
- alternância direta entre temas claro e escuro, preservada em recarregamentos e sincronizada entre abas;
- ícones SVG locais e imagens WebP otimizadas;
- navegação por teclado, foco visível, labels e atributos ARIA;
- CSS modular e JavaScript ES Modules, sem framework ou etapa adicional de build.

## Principais funcionalidades

### Descoberta e candidatura

- home com oportunidades em destaque;
- listagem pública com pesquisa e filtros;
- curadoria de oportunidades com data e link de candidatura verificáveis;
- detalhes e contador de visualizações;
- vagas remotas, híbridas e presenciais na Paraíba;
- candidatura interna, associada à conta quando o candidato está autenticado;
- divulgação de vagas mediante assinatura ativa.

### Curadoria de vagas

O catálogo público foi atualizado pela migration `V13__refresh_verified_tech_jobs_august.sql` com oportunidades de tecnologia publicadas entre **03/08/2026 e 07/08/2026**. A seleção combina anúncios públicos do LinkedIn e da Remotar com páginas de candidatura das empresas em Gupy, InHire, Recrutei, Sólides e Zoho Recruit.

Cada item mantém o link original, a data da fonte e uma descrição resumida. Links sem data confirmável, vagas repetidas e anúncios indisponíveis são descartados. As oportunidades anteriores não são apagadas: ficam arquivadas, preservando histórico, candidaturas e auditoria. A atualização registra `JOB_CATALOG_ARCHIVED` e `JOB_CURATED` em `audit_log`.

### Conta e perfil profissional

- cadastro e login tradicional com senha BCrypt;
- login e cadastro automático por Google OAuth2;
- papéis `ROLE_USER` e `ROLE_ADMIN`;
- perfil com nome, biografia, foto reposicionável, capa personalizada e currículo PDF;
- histórico de experiências profissionais;
- preferência persistida de tema claro ou escuro no navegador e na conta;
- assistente de carreira integrado ao LiteLLM.

Arquivos de perfil são validados por tamanho, tipo MIME e assinatura binária. A foto é alterada diretamente no avatar e a capa no cabeçalho do perfil, ambas com prévia e ajuste de enquadramento. Fotos aceitas: JPEG, PNG e WebP, até 3 MB; capas nos mesmos formatos, até 5 MB. Currículos: PDF válido, até 5 MB.

### Administração e governança

- dashboard com indicadores de vagas, candidaturas e usuários;
- busca e filtros por status, modelo de trabalho, cargo e empresa;
- publicação, retorno para pendência, arquivamento e remoção;
- consulta de candidaturas e usuários;
- auditoria administrativa com filtros por ação, ator, entidade e período.

A área administrativa fica em `/admin` e exige `ROLE_ADMIN`. Usuários comuns recebem acesso negado mesmo quando autenticados.

## Arquitetura

O projeto preserva uma arquitetura Spring MVC em camadas:

```text
HTTP / Thymeleaf
       ↓
Controllers e Spring Security
       ↓
Serviços e regras de negócio
       ↓
Spring Data JPA
       ↓
PostgreSQL + Flyway
```

Tecnologias principais:

- Java 21 e Spring Boot;
- Spring MVC, Thymeleaf e Spring Security;
- OAuth2 Client para Google;
- PostgreSQL em produção e H2 nos testes;
- Flyway para evolução segura do banco;
- Stripe Checkout e webhooks assinados;
- LiteLLM com API compatível com OpenAI;
- OpenTelemetry Java Agent e Umami;
- JUnit 5, MockMvc, Mockito e JaCoCo;
- Docker e GitHub Actions.

## Autenticação

### Login tradicional

Acesse `/login` e informe usuário ou e-mail e senha. Novos usuários podem usar `/cadastro`. As senhas nunca são armazenadas em texto puro: o sistema usa BCrypt.

### Recuperação de senha

O link **Esqueceu sua senha?** abre `/esqueci-senha`. O sistema responde de forma genérica para não revelar quais e-mails possuem conta. Para endereços cadastrados, é criado um token aleatório de uso único, armazenado no banco somente como hash SHA-256 e válido por 30 minutos. A nova senha é gravada com BCrypt e todos os tokens ativos do usuário são invalidados.

O envio utiliza SMTP pelo Spring Mail. Configure no ambiente de produção:

```text
PASSWORD_RESET_EMAIL_ENABLED=true
APP_PUBLIC_URL=https://eq13.dsc.rodrigor.com
MAIL_HOST=<servidor-smtp>
MAIL_PORT=587
MAIL_USERNAME=<usuario-smtp>
MAIL_PASSWORD=<senha-smtp>
MAIL_FROM=<remetente-verificado>
MAIL_SMTP_AUTH=true
MAIL_STARTTLS_ENABLE=true
PASSWORD_RESET_TTL_MINUTES=30
```

Sem essas variáveis, o restante da aplicação continua funcionando, mas nenhum link é enviado. Solicitações e redefinições concluídas geram os eventos `PASSWORD_RESET_REQUESTED` e `PASSWORD_RESET_COMPLETED` na auditoria.

No ambiente de desenvolvimento existe um administrador inicial:

```text
usuário: admin
senha: admin123
```

Essa senha é apenas local. Em produção, `ADMIN_PASSWORD` deve ser fornecida fora do repositório. Sem ela, uma conta já existente continua disponível, mas o sistema não cria administrador nem altera sua senha. Ao configurá-la, o bootstrap cria a conta ausente ou rotaciona a senha da conta administrativa existente.

### Google OAuth2

O botão **Continuar com Google** aparece tanto no login quanto no cadastro. No primeiro acesso, o e-mail recebido do Google cria uma conta `ROLE_USER`; em acessos posteriores a conta existente é reutilizada. Uma conta administrativa já existente conserva `ROLE_ADMIN`.

Variáveis necessárias:

```text
GOOGLE_CLIENT_ID=<google-client-id>
GOOGLE_CLIENT_SECRET=<google-client-secret>
```

Callbacks autorizados no Google Cloud Console:

```text
http://localhost:8080/login/oauth2/code/google
https://eq13.dsc.rodrigor.com/login/oauth2/code/google
```

Em desenvolvimento, `GOOGLE_REDIRECT_URI_DEV` pode substituir o callback local. Em produção, a imagem inicia obrigatoriamente com o perfil `prod` e usa:

```text
GOOGLE_REDIRECT_URI=https://eq13.dsc.rodrigor.com/login/oauth2/code/google
```

Esse contrato também possui teste automatizado para impedir regressão para `localhost` no deploy.

## Integrações externas

### Stripe

O Stripe cuida da assinatura para divulgação de vagas. O sistema só ativa o plano depois de validar um webhook assinado; visitar a página de sucesso não concede acesso.

```text
STRIPE_SECRET_KEY=<stripe-secret-key>
STRIPE_WEBHOOK_SECRET=<stripe-webhook-signing-secret>
STRIPE_MONTHLY_PRICE_ID=<stripe-monthly-price-id>
STRIPE_SUCCESS_URL=https://eq13.dsc.rodrigor.com/divulgar/assinar/sucesso
STRIPE_CANCEL_URL=https://eq13.dsc.rodrigor.com/divulgar/assinar/cancelado
```

Endpoint de webhook:

```text
POST /webhooks/stripe
```

### Assistente de carreira com IA

O perfil do candidato oferece orientações profissionais por meio do proxy LiteLLM da disciplina:

```text
LITELLM_ENABLED=true
LITELLM_BASE_URL=https://llm.rodrigor.com
LITELLM_API_KEY=<litellm-api-key>
LITELLM_MODEL=gpt-4o-mini
```

Quando a integração está indisponível, a falha é tratada e apresentada ao usuário sem expor credenciais nem detalhes internos.

## Auditoria

Eventos relevantes são persistidos na tabela `audit_log`, incluindo:

- sucesso e falha de login, OAuth2 e logout;
- solicitação e conclusão de recuperação de senha;
- cadastro e alterações do perfil;
- foto, capa, enquadramento das imagens, currículo, experiências e preferência de tema;
- uso do assistente de carreira;
- candidaturas;
- envio, criação e moderação de vagas;
- início, ativação e cancelamento de assinatura Stripe.

Cada registro pode guardar ator, ação, entidade, identificador, descrição, IP, navegador e data. Administradores consultam a trilha em `/admin/auditoria`.

## Healthcheck, telemetria e analytics

`GET /ping` é público e retorna HTTP 200 somente quando a aplicação consegue consultar o banco:

```json
{
  "status": "ok",
  "service": "eq13",
  "database": "up",
  "timestamp": "..."
}
```

O `HEALTHCHECK` do container usa esse endpoint; portanto ele verifica aplicação e persistência, não uma resposta estática.

A imagem inclui o OpenTelemetry Java Agent. Traces, métricas e logs usam `service.name=dsc-eq13`:

```text
OTEL_SERVICE_NAME=dsc-eq13
OTEL_EXPORTER_OTLP_ENDPOINT=https://otel.dsc.rodrigor.com
OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
OTEL_EXPORTER_OTLP_HEADERS=Authorization=Bearer <otel-ingest-token>
OTEL_TRACES_EXPORTER=otlp
OTEL_METRICS_EXPORTER=otlp
OTEL_LOGS_EXPORTER=otlp
OTEL_RESOURCE_ATTRIBUTES=deployment.environment=production,service.namespace=dsc
```

No [painel OpenTelemetry/Grafana](https://otel.dsc.rodrigor.com), use o Explore e filtre por `service.name = dsc-eq13`. Credenciais do painel e token de ingestão pertencem ao ambiente de deploy e não são documentados no repositório.

O Umami está carregado no layout compartilhado do Thymeleaf e mede visitas e páginas sem bloquear a navegação.

## Banco de dados

Produção usa PostgreSQL. Alterações estruturais são versionadas em `src/main/resources/db/migration` com Flyway; a aplicação usa `ddl-auto=validate`, evitando alterações destrutivas automáticas.

Principais tabelas:

- `app_user`;
- `candidate_experience`;
- `job_posting`;
- `candidate_application`;
- `subscription`;
- `audit_log`.

Nenhuma migration apaga dados existentes.

## Executar localmente

Pré-requisitos:

- Java 21;
- Maven 3.9 ou superior.

Primeiro, inicie o PostgreSQL local:

```bash
docker compose -f docker/docker-compose.dev.yml up -d
```

Em seguida, execute a aplicação:

```bash
mvn spring-boot:run
```

O perfil de desenvolvimento usa PostgreSQL; o H2 fica isolado no perfil automatizado de testes. Depois, acesse:

- aplicação: `http://localhost:8080`;
- healthcheck: `http://localhost:8080/ping`.

Para acompanhar ou encerrar somente o banco local:

```bash
docker compose -f docker/docker-compose.dev.yml logs -f postgres
docker compose -f docker/docker-compose.dev.yml down
```

## Configuração segura

O repositório não versiona `.env`, variantes de `.env`, senhas, tokens nem chaves reais. Configure os valores no sistema operacional, no portal do servidor ou em GitHub Actions Secrets.

Principais variáveis de produção:

```text
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/eq13
SPRING_DATASOURCE_USERNAME=eq13
SPRING_DATASOURCE_PASSWORD=<senha-real>
ADMIN_USERNAME=admin
ADMIN_PASSWORD=<senha-admin>
GOOGLE_CLIENT_ID=<google-client-id>
GOOGLE_CLIENT_SECRET=<google-client-secret>
GOOGLE_REDIRECT_URI=https://eq13.dsc.rodrigor.com/login/oauth2/code/google
PASSWORD_RESET_EMAIL_ENABLED=true
APP_PUBLIC_URL=https://eq13.dsc.rodrigor.com
MAIL_HOST=<servidor-smtp>
MAIL_PORT=587
MAIL_USERNAME=<usuario-smtp>
MAIL_PASSWORD=<senha-smtp>
MAIL_FROM=<remetente-verificado>
STRIPE_SECRET_KEY=<stripe-secret-key>
STRIPE_WEBHOOK_SECRET=<stripe-webhook-signing-secret>
STRIPE_MONTHLY_PRICE_ID=<stripe-monthly-price-id>
LITELLM_API_KEY=<litellm-api-key>
OTEL_EXPORTER_OTLP_HEADERS=Authorization=Bearer <otel-ingest-token>
AWS_S3_ACCESS_KEY=<s3-access-key>
AWS_S3_SECRET_KEY=<s3-secret-key>
```

## Testes e qualidade

A suíte valida comportamento real em diferentes níveis:

- **JUnit 5:** regras de domínio e serviços;
- **Mockito:** falhas e respostas de Google, Stripe, LiteLLM e dependências isoladas;
- **MockMvc:** páginas públicas, formulários, segurança, papéis e área administrativa;
- **Spring Boot Test:** integração com persistência, migrations e contexto;
- **JaCoCo:** cobertura de linhas e falha automática do build abaixo de 100%.

Comando completo:

```bash
mvn clean verify
```

Relatório:

```text
target/site/jacoco/index.html
```

Uma cópia do último relatório validado também fica em [`cobertura/index.html`](cobertura/index.html) para inspeção direta no repositório.

O pipeline exige 100% de cobertura de linhas. A suíte alcança essa meta com testes de comportamento — sem testes vazios ou exclusões artificiais. Cobertura de linhas não substitui análise de riscos, testes de integração ou revisão de código, mas impede que novos caminhos sejam adicionados sem verificação automatizada.

O GitHub Actions executa `mvn verify` antes de construir e publicar a imagem. Se teste, cobertura ou compilação falhar, não há deploy.

### Evidências dos requisitos de qualidade

| Requisito | Implementação verificável |
|---|---|
| Auditoria | Entidade e migration `audit_log`, `AuditLogService`, eventos de autenticação, perfil, candidatura, vagas, Stripe e IA, consulta em `/admin/auditoria` |
| Integração externa | Google OAuth2, Stripe Checkout/webhooks assinados e LiteLLM |
| Cobertura ≥85% | JaCoCo executado no `mvn verify`, com gate atual de 100% das linhas |
| IA | Assistente de carreira via LiteLLM, com tratamento de indisponibilidade |
| Healthcheck com banco | `/ping` executa consulta real pelo `DatabaseHealthService` e informa `database: up` |
| Telemetria e analytics | OpenTelemetry Java Agent com `service.name=dsc-eq13` e Umami no layout compartilhado |

Essas evidências são exercitadas pela suíte automatizada e pelo workflow de deploy; não dependem apenas da documentação.

## Rotas úteis

| Rota | Acesso | Finalidade |
|---|---|---|
| `/` | público | página inicial |
| `/vagas` | público | pesquisa e filtros |
| `/vagas/{id}` | público | detalhes e candidatura |
| `/login` | público | login tradicional ou Google |
| `/cadastro` | público | criação de conta |
| `/esqueci-senha` | público | solicitação segura de recuperação |
| `/redefinir-senha` | público + token | definição de nova senha |
| `/minha-conta` | autenticado | perfil profissional e IA |
| `/divulgar` | autenticado + assinatura | envio de vaga |
| `/admin` | `ROLE_ADMIN` | dashboard |
| `/admin/vagas` | `ROLE_ADMIN` | moderação |
| `/admin/candidaturas` | `ROLE_ADMIN` | candidaturas |
| `/admin/auditoria` | `ROLE_ADMIN` | trilha de auditoria |
| `/admin/usuarios` | `ROLE_ADMIN` | usuários |
| `/ping` | público | saúde da aplicação e banco |

## Deploy

Pushes na branch `main` disparam:

1. compilação, testes e cobertura;
2. criação da imagem Docker;
3. publicação no GitHub Container Registry;
4. atualização do container no servidor;
5. verificação contínua pelo `/ping`.

A aplicação escuta internamente em `8080` e é publicada no servidor da disciplina em `127.0.0.1:8113`.
