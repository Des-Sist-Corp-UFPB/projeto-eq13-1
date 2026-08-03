# RadarTech PB - Sistema de Vagas de TI

Projeto desenvolvido para a disciplina de Desenvolvimento de Sistemas Corporativos da UFPB. O RadarTech PB é um portal para curadoria, busca e gerenciamento de vagas de tecnologia, com foco em estudantes, estagiários e profissionais júnior.

## Atendimento da Avaliação 2

Esta matriz aponta a implementação executável e a evidência verificável de cada requisito. O RadarTech PB vai além do CRUD de vagas.

| Requisito | Situação | Implementação no código | Como verificar |
| --- | --- | --- | --- |
| **Aud — Auditoria** | ✅ Atendido | Entidade/tabela `audit_log`, migration Flyway, `AuditLogService`, handlers de autenticação e auditoria das ações de vagas, usuários, perfil, IA e Stripe. Consulta protegida em `/admin/auditoria`. | Executar os fluxos e consultar `/admin/auditoria`; testes em `ServiceBehaviorIntegrationTest`, `CandidateProfileIntegrationTest` e controllers. |
| **Int — Integração externa** | ✅ Atendido | Google OAuth2 Client para login/cadastro e Stripe Checkout com webhook assinado. A assinatura só é ativada após `checkout.session.completed`. | Código em `RadarOAuth2UserService`, `BillingService`, `StripeWebhookService`; executar `mvn verify`. |
| **Cob — Cobertura ≥ 85%** | ✅ Atendido | JaCoCo está ligado ao `mvn verify` com regra `COVEREDRATIO >= 0.85`; o build e o deploy falham abaixo desse valor. | `mvn verify` e `target/site/jacoco/index.html`; GitHub Actions executa a verificação antes da imagem Docker. |
| **IA — uso de LLM** | ✅ Atendido | Assistente de carreira em `/minha-conta`, usando a API OpenAI-compatible do LiteLLM e o modelo configurável `gpt-4o-mini`. A utilização é auditada. | `AiCareerService`, formulário “Assistente de carreira” e `AiCareerServiceTest`. |
| **HC — healthcheck no banco** | ✅ Atendido | `GET /ping` chama `DatabaseHealthService`, que executa `SELECT 1` no banco. Só retorna HTTP 200 com `database: up`; falhas retornam HTTP 503. | `curl http://localhost:8080/ping`, `DatabaseHealthServiceTest` e `PingControllerTest`. |
| **Tel — Telemetria** | ✅ Atendido | A imagem Docker inclui o Java Agent do OpenTelemetry e exporta traces, métricas e logs via OTLP com `service.name=dsc-eq13`. | `docker/Dockerfile`, `docker-compose.yml` e Grafana Explore filtrando `service.name = dsc-eq13`. |
| **Uma — Umami** | ✅ Atendido | Script oficial do Umami presente no fragmento comum do `<head>`, portanto carregado em todas as páginas. | `templates/fragments/layout.html` e painel Umami da equipe. |

## Visão Geral

O sistema permite que visitantes consultem vagas reais de TI, filtrem oportunidades por modelo de trabalho e abram o link público da vaga original. Também existe uma área administrativa para moderação das vagas, acompanhamento das candidaturas internas, consulta de auditoria e visualização de indicadores.

As vagas remotas podem ser de qualquer lugar do Brasil. Vagas híbridas e presenciais são focadas apenas na Paraíba.

## Tecnologias

| Camada | Tecnologia |
| --- | --- |
| Backend | Java 21 + Spring Boot 3.5 |
| Web | Spring MVC + Thymeleaf |
| Segurança | Spring Security + OAuth2 Client |
| Banco de dados | PostgreSQL |
| Migrações | Flyway |
| Build | Maven |
| Testes | JUnit 5, MockMvc, Mockito, JaCoCo |
| IA | LiteLLM, API OpenAI-compatible |
| Observabilidade | OpenTelemetry (traces, métricas e logs) + Umami |
| Infraestrutura | Docker + Docker Compose |
| CI/CD | GitHub Actions + GHCR |

## Funcionalidades

- Página inicial com busca e atalhos para vagas.
- Listagem pública de vagas.
- Filtro por termo de busca e modelo de trabalho.
- Página de detalhes da vaga.
- Link externo para candidatura em plataformas como Gupy, LinkedIn e Indeed.
- Formulário público de candidatura interna.
- Associação da candidatura ao usuário logado, quando houver.
- Página pública para divulgar novas vagas.
- Login tradicional com e-mail/usuário e senha.
- Cadastro de usuário comum.
- Login com Google OAuth2.
- Página "Minha conta".
- Login administrativo.
- Checkout de assinatura com Stripe para recursos administrativos de cobranca.
- Assistente de carreira com IA, contextualizado pelo perfil do candidato.
- Telemetria com OpenTelemetry e análise de acesso com Umami.
- Painel administrativo com indicadores.
- Gestão de vagas com busca e filtros.
- Gestão de usuários.
- Consulta de logs de auditoria.
- Health check público em `GET /ping`.

## Login e Perfis

O sistema usa dois papéis:

- `ROLE_USER`: usuário comum.
- `ROLE_ADMIN`: administrador.

Páginas públicas continuam acessíveis sem login. A área `/admin/**` exige `ROLE_ADMIN`.

O login tradicional usa BCrypt para armazenar senhas. O usuário administrador inicial é criado automaticamente a partir das variáveis:

```env
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin123
```

Em produção, troque `ADMIN_PASSWORD` por uma senha real fora do código-fonte.

### Perfil profissional do candidato

Em `/minha-conta`, usuários autenticados podem personalizar:

- nome e biografia profissional;
- foto em JPEG, PNG ou WebP, limitada a 3 MB;
- currículo em PDF, limitado a 5 MB;
- múltiplas experiências profissionais com período e descrição;
- tema claro com fundo branco por padrão, escuro ou automático.

Foto e currículo são privados e só podem ser acessados pela própria conta autenticada. Atualização do perfil, arquivos, experiências e tema geram eventos persistentes na auditoria administrativa.

### Identidade visual e temas

A interface usa a logomarca oficial recortada, sem fundo, nas variantes `radartech-logo-transparent.png` (tema claro) e `radartech-logo-dark.png` (branco/turquesa no tema escuro), além da paleta azul-marinho, azul e turquesa da marca. O layout foi reorganizado com navegação e cartões inspirados em redes profissionais, mantendo componentes próprios do RadarTech PB.

Visitantes começam no tema claro com fundo branco e podem trocar o tema localmente. Para usuários autenticados, a preferência `LIGHT`, `DARK` ou `SYSTEM` também é persistida no perfil.

### Curadoria recente de vagas

A migration `V9__seed_recent_verified_jobs.sql` adiciona oportunidades verificadas entre 10 e 17 de julho de 2026, com links para as publicações originais. A inclusão também registra a ação `JOB_CURATED` na auditoria.

## Google OAuth2

Foi escolhido Google OAuth2 porque é a alternativa mais simples e compatível com Spring Security OAuth2 Client.

Configure as credenciais por variáveis de ambiente:

```env
GOOGLE_CLIENT_ID=<google-client-id>
GOOGLE_CLIENT_SECRET=<google-client-secret>
GOOGLE_REDIRECT_URI=https://eq13.dsc.rodrigor.com/login/oauth2/code/google
GOOGLE_REDIRECT_URI_DEV=http://localhost:8080/login/oauth2/code/google
```

No console do Google Cloud, configure o redirect URI autorizado:

```text
https://eq13.dsc.rodrigor.com/login/oauth2/code/google
```

Para testes locais, use também:

```text
http://localhost:8080/login/oauth2/code/google
```

Em desenvolvimento (`spring.profiles.active=dev`), o projeto usa `GOOGLE_REDIRECT_URI_DEV` por padrão.
Isso evita que uma variável `GOOGLE_REDIRECT_URI` de produção redirecione o login local para `eq13.dsc.rodrigor.com`.

Nunca versione client id real, client secret real, tokens ou senhas reais.

Ao logar com Google:

- se o e-mail ainda não existir, o sistema cria um usuário com `ROLE_USER`;
- se o e-mail já existir, o sistema reutiliza a conta existente;
- usuários administradores existentes preservam o papel administrativo.

## Cobranca com Stripe

O sistema possui um fluxo de assinatura usando Stripe Checkout. A criação do checkout deixa a assinatura como `PENDING`; ela só passa para `ACTIVE` quando o Stripe envia o evento assinado `checkout.session.completed`. Cancelamentos recebidos do provedor também atualizam o sistema e ambos os eventos são auditados.

Configure no Stripe o webhook público:

```text
POST https://eq13.dsc.rodrigor.com/webhooks/stripe
```

Assine pelo menos os eventos `checkout.session.completed` e `customer.subscription.deleted`. As credenciais e o identificador do preço mensal devem ser configurados apenas por variáveis de ambiente:

```env
STRIPE_SECRET_KEY=<stripe-secret-key>
STRIPE_WEBHOOK_SECRET=<stripe-webhook-signing-secret>
STRIPE_MONTHLY_PRICE_ID=<stripe-monthly-price-id>
STRIPE_SUCCESS_URL=https://eq13.dsc.rodrigor.com/divulgar/assinar/sucesso
STRIPE_CANCEL_URL=https://eq13.dsc.rodrigor.com/divulgar/assinar/cancelado
```

Nunca versione chaves reais da Stripe. Sem `STRIPE_SECRET_KEY` e `STRIPE_MONTHLY_PRICE_ID`, o checkout fica indisponivel e o sistema informa a ausencia de configuracao.

## IA com LiteLLM

O assistente de carreira aparece em `/minha-conta`. Ele envia ao LiteLLM somente o contexto profissional necessário — nome, biografia e resumo das experiências — e não envia foto nem conteúdo do currículo PDF. A resposta é exibida como orientação, e o uso gera o evento de auditoria `AI_CAREER_ASSISTANT_USED`.

```env
LITELLM_ENABLED=true
LITELLM_BASE_URL=https://llm.rodrigor.com
LITELLM_API_KEY=<litellm-api-key>
LITELLM_MODEL=gpt-4o-mini
```

O cliente usa `POST /v1/chat/completions`, formato compatível com a API da OpenAI. Nenhuma API key é armazenada no repositório.

## Healthcheck com consulta ao banco

`GET /ping` é público e consulta o banco de dados de verdade:

```sql
SELECT 1
```

Resposta saudável:

```json
{"status":"ok","service":"eq13","database":"up","timestamp":"..."}
```

Se a consulta falhar, o endpoint retorna HTTP `503` e `database: down`. O healthcheck do Docker usa exatamente essa rota; por isso o container só é considerado saudável quando aplicação e banco estão disponíveis.

## Telemetria OpenTelemetry

A imagem de produção contém `opentelemetry-javaagent`. O agente instrumenta automaticamente Spring MVC, JDBC, chamadas HTTP e outras bibliotecas, exportando traces, métricas e logs por OTLP.

```env
OTEL_SERVICE_NAME=dsc-eq13
OTEL_EXPORTER_OTLP_ENDPOINT=https://otel.dsc.rodrigor.com
OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
OTEL_EXPORTER_OTLP_HEADERS=Authorization=Bearer <otel-ingest-token>
OTEL_TRACES_EXPORTER=otlp
OTEL_METRICS_EXPORTER=otlp
OTEL_LOGS_EXPORTER=otlp
OTEL_RESOURCE_ATTRIBUTES=deployment.environment=production,service.namespace=dsc
```

No Grafana/OTel Explore, filtre por:

```text
service.name = dsc-eq13
```

O token de ingestão deve ficar apenas nas variáveis do servidor.

## Analytics com Umami

O fragmento global `src/main/resources/templates/fragments/layout.html` contém o script Umami e o identificador do site fornecido para a equipe 13. Como o fragmento compõe o `<head>` de todas as páginas, visitas, páginas e referências são coletadas em toda a aplicação. As credenciais do painel não ficam no código.

## Auditoria

O sistema grava logs persistentes na tabela `audit_log`.

Eventos auditados incluem:

- login tradicional bem-sucedido;
- falha de login;
- logout;
- cadastro de usuário;
- login/cadastro via Google;
- atualização de perfil, foto, currículo e tema;
- inclusão e remoção de experiências profissionais;
- inclusão de vagas verificadas pela curadoria;
- envio público de vaga;
- criação de vaga pelo admin;
- publicação, pendência, arquivamento e remoção de vaga;
- envio de candidatura interna.
- uso do assistente de carreira via LiteLLM;
- início de checkout, confirmação e cancelamento de assinatura via Stripe.

Administradores podem consultar os logs em:

```text
/admin/auditoria
```

A tela permite filtros por ação, usuário/e-mail, tipo de entidade e período.

## Painel Administrativo

O painel admin possui navegação para:

- Dashboard;
- Vagas;
- Candidaturas;
- Auditoria;
- Usuários.

O dashboard exibe:

- total de vagas;
- vagas publicadas;
- vagas pendentes;
- vagas arquivadas;
- total de candidaturas internas;
- total de usuários cadastrados;
- vagas por modalidade;
- visualizações.

## Como Rodar Localmente

Antes de iniciar a aplicação, suba o PostgreSQL:

```bash
docker compose -f docker/docker-compose.dev.yml up -d
```

Depois rode a aplicação Spring Boot:

```bash
mvn spring-boot:run
```

A aplicação ficará disponível em:

```text
http://localhost:8080
```

## Acesso Administrativo Local

```text
Usuário: admin
Senha: admin123
```

## Banco de Dados Local

O ambiente de desenvolvimento usa PostgreSQL no Docker.

| Configuração | Valor |
| --- | --- |
| Host | `localhost` |
| Porta | `5432` |
| Banco | `jobhub_dev` |
| Usuário | `jobhub` |
| Senha | `jobhub123` |

As tabelas são criadas automaticamente pelas migrations Flyway em `src/main/resources/db/migration`.

## Testes e Cobertura

Para executar testes e cobertura:

```bash
mvn verify
```

O projeto usa JaCoCo e exige cobertura mínima integral de 85%, sem excluir os pacotes de domínio, DTOs ou segurança. O build falha caso a cobertura fique abaixo do mínimo configurado.

**Cobertura atual: 89,96% das linhas** (977 de 1.086), medida pelo JaCoCo em 02/08/2026. O valor aceito nunca pode ser inferior a **85% de linhas**, pois essa regra está no `pom.xml` e bloqueia o build quando não é cumprida.

O relatório de cobertura está commitado na pasta `cobertura/` na raiz do projeto:

```text
cobertura/index.html
```

O relatório também pode ser regenerado localmente em:

```text
target/site/jacoco/index.html
```


## Estrutura do Projeto

```text
.
|-- .github/workflows/        # Workflow de deploy
|-- docker/                   # Dockerfile e arquivos Docker Compose
|-- src/main/java/br/ufpb/dsc/jobhub/
|   |-- config/               # Configurações de segurança e autenticação
|   |-- controller/           # Controllers HTTP
|   |-- domain/               # Entidades JPA
|   |-- dto/                  # Objetos de transferência/formulários
|   |-- repository/           # Repositórios Spring Data JPA
|   `-- service/              # Regras de negócio
|-- src/main/resources/
|   |-- db/migration/         # Migrations Flyway
|   |-- static/               # Arquivos estáticos
|   `-- templates/            # Páginas Thymeleaf
|-- src/test/                 # Testes automatizados
`-- pom.xml                   # Configuração Maven
```

## Variáveis de Ambiente

O projeto inclui um arquivo `.env.example` com valores de exemplo. Não faça commit de arquivos `.env` reais com senhas ou tokens.

```env
GOOGLE_CLIENT_ID=<google-client-id>
GOOGLE_CLIENT_SECRET=<google-client-secret>
GOOGLE_REDIRECT_URI=https://eq13.dsc.rodrigor.com/login/oauth2/code/google
GOOGLE_REDIRECT_URI_DEV=http://localhost:8080/login/oauth2/code/google
STRIPE_SECRET_KEY=<stripe-secret-key>
STRIPE_MONTHLY_PRICE_ID=<stripe-monthly-price-id>
ADMIN_USERNAME=admin
ADMIN_PASSWORD=<senha-admin>
```

## Deploy

O workflow em `.github/workflows/deploy.yml` executa o pipeline de produção:

1. roda `mvn verify -B`;
2. valida testes e cobertura;
3. constrói a imagem Docker com `docker/Dockerfile`;
4. publica a imagem no GitHub Container Registry (GHCR);
5. aciona o deploy no servidor `dsc.rodrigor.com` usando o secret `SSH_DEPLOY_KEY`.

O deploy usa o usuário SSH da equipe `eq13` e publica a aplicação na porta `8113`.

### Secrets no GitHub

Configure em `Settings -> Secrets and variables -> Actions`:

| Secret | Valor |
| --- | --- |
| `SSH_USERNAME` | `eq13` |
| `SSH_DEPLOY_KEY` | chave privada SSH fornecida pela disciplina |

### Variáveis no Servidor

Configure as variáveis reais no `.env` do servidor ou pelo painel da disciplina. Não versione senhas reais no GitHub.

```env
APP_IMAGE=ghcr.io/des-sist-corp-ufpb/projeto-eq13:latest
SERVER_PORT=8113

SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/eq13
SPRING_DATASOURCE_USERNAME=eq13
SPRING_DATASOURCE_PASSWORD=<senha-real-do-banco>
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=5

AWS_S3_ENDPOINT=http://minio:9000
AWS_S3_PUBLIC_ENDPOINT=https://s3.dsc.rodrigor.com
AWS_S3_REGION=us-east-1
AWS_S3_BUCKET=eq13
AWS_S3_ACCESS_KEY=<access-key-minio>
AWS_S3_SECRET_KEY=<secret-real-do-minio>

GOOGLE_CLIENT_ID=<google-client-id>
GOOGLE_CLIENT_SECRET=<google-client-secret>
GOOGLE_REDIRECT_URI=https://eq13.dsc.rodrigor.com/login/oauth2/code/google

STRIPE_SECRET_KEY=<stripe-secret-key>
STRIPE_MONTHLY_PRICE_ID=<stripe-monthly-price-id>
STRIPE_SUCCESS_URL=https://eq13.dsc.rodrigor.com/admin/billing/sucesso
STRIPE_CANCEL_URL=https://eq13.dsc.rodrigor.com/admin/billing/cancelado

ADMIN_USERNAME=admin
ADMIN_PASSWORD=<senha-admin>
```

O portal da disciplina verifica `GET /ping`. Essa rota é pública e retorna JSON com `status: "ok"` e `service: "eq13"`.
