# RadarTech PB - Sistema de Vagas de TI

Projeto desenvolvido para a disciplina de Desenvolvimento de Sistemas Corporativos da UFPB. O RadarTech PB é um portal para curadoria, busca e gerenciamento de vagas de tecnologia, com foco em estudantes, estagiários e profissionais júnior.

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

## Google OAuth2

Foi escolhido Google OAuth2 porque é a alternativa mais simples e compatível com Spring Security OAuth2 Client.

Configure as credenciais por variáveis de ambiente:

```env
GOOGLE_CLIENT_ID=<google-client-id>
GOOGLE_CLIENT_SECRET=<google-client-secret>
GOOGLE_REDIRECT_URI=https://eq13.dsc.rodrigor.com/login/oauth2/code/google
```

No console do Google Cloud, configure o redirect URI autorizado:

```text
https://eq13.dsc.rodrigor.com/login/oauth2/code/google
```

Para testes locais, use também:

```text
http://localhost:8080/login/oauth2/code/google
```

Nunca versione client id real, client secret real, tokens ou senhas reais.

Ao logar com Google:

- se o e-mail ainda não existir, o sistema cria um usuário com `ROLE_USER`;
- se o e-mail já existir, o sistema reutiliza a conta existente;
- usuários administradores existentes preservam o papel administrativo.

## Cobranca com Stripe

O painel administrativo possui um fluxo de checkout de assinatura usando Stripe Checkout. As credenciais e o identificador do preco mensal devem ser configurados apenas por variaveis de ambiente:

```env
STRIPE_SECRET_KEY=<stripe-secret-key>
STRIPE_MONTHLY_PRICE_ID=<stripe-monthly-price-id>
STRIPE_SUCCESS_URL=https://eq13.dsc.rodrigor.com/admin/billing/sucesso
STRIPE_CANCEL_URL=https://eq13.dsc.rodrigor.com/admin/billing/cancelado
```

Nunca versione chaves reais da Stripe. Sem `STRIPE_SECRET_KEY` e `STRIPE_MONTHLY_PRICE_ID`, o checkout fica indisponivel e o sistema informa a ausencia de configuracao.

## Auditoria

O sistema grava logs persistentes na tabela `audit_log`.

Eventos auditados incluem:

- login tradicional bem-sucedido;
- falha de login;
- logout;
- cadastro de usuário;
- login/cadastro via Google;
- envio público de vaga;
- criação de vaga pelo admin;
- publicação, pendência, arquivamento e remoção de vaga;
- envio de candidatura interna.

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
mvn clean test jacoco:report
```

O projeto usa JaCoCo e exige cobertura mínima de 85%. O build falha caso a cobertura fique abaixo do mínimo configurado.

**Cobertura total de linhas: 93,01%** (639 de 687 linhas cobertas).

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
