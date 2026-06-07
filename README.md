# RadarTech PB - Sistema de Vagas de TI

Projeto desenvolvido para a disciplina de Desenvolvimento de Sistemas Corporativos da UFPB. O RadarTech PB é um portal para curadoria, busca e gerenciamento de vagas de tecnologia, com foco em estudantes, estagiários e profissionais júnior.

## Visão geral

O sistema permite que visitantes consultem vagas reais de TI, filtrem oportunidades por modelo de trabalho e abram o link público da vaga original. Também existe uma área administrativa para moderação das vagas, acompanhamento das candidaturas internas e visualização de indicadores do portal.

As vagas remotas podem ser de qualquer lugar do Brasil. Vagas híbridas e presenciais são focadas apenas na Paraíba.

## Tecnologias

| Camada | Tecnologia |
| --- | --- |
| Backend | Java 21 + Spring Boot 3.5 |
| Web | Spring MVC + Thymeleaf |
| Segurança | Spring Security |
| Banco de dados | PostgreSQL |
| Migrações | Flyway |
| Build | Maven |
| Infraestrutura | Docker + Docker Compose |
| CI/CD | GitHub Actions + GHCR |

## Funcionalidades

- Página inicial com destaques do portal.
- Listagem pública de vagas.
- Filtro por termo de busca e modelo de trabalho.
- Página de detalhes da vaga.
- Link externo para candidatura em plataformas como Gupy, LinkedIn e Indeed.
- Formulário público de candidatura.
- Página pública para divulgar novas vagas.
- Login administrativo.
- Painel administrativo com indicadores.
- Moderação e gerenciamento de vagas.
- Visualização de candidaturas recebidas.
- Health check público em `GET /ping`.

## Requisitos

- Java 21
- Maven
- Docker Desktop ou Docker Engine
- PostgreSQL via Docker Compose

## Como rodar localmente

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

## Acesso administrativo local

```text
Usuário: admin
Senha: admin123
```

## Banco de dados local

O ambiente de desenvolvimento usa PostgreSQL no Docker.

| Configuracao | Valor |
| --- | --- |
| Host | `localhost` |
| Porta | `5432` |
| Banco | `jobhub_dev` |
| Usuário | `jobhub` |
| Senha | `jobhub123` |

As tabelas são criadas automaticamente pelas migrations Flyway em `src/main/resources/db/migration`.

## Testes

Para executar os testes:

```bash
mvn test
```

## Estrutura do projeto

```text
.
|-- .github/workflows/        # Workflow de deploy
|-- docker/                   # Dockerfile e arquivos Docker Compose
|-- src/main/java/br/ufpb/dsc/jobhub/
|   |-- config/               # Configurações da aplicação
|   |-- controller/           # Controllers HTTP
|   |-- domain/               # Entidades JPA
|   |-- dto/                  # Objetos de transferencia/formularios
|   |-- repository/           # Repositórios Spring Data JPA
|   `-- service/              # Regras de negocio
|-- src/main/resources/
|   |-- db/migration/         # Migrations Flyway
|   |-- static/               # Arquivos estaticos
|   `-- templates/            # Páginas Thymeleaf
|-- src/test/                 # Testes automatizados
`-- pom.xml                   # Configuracao Maven
```

## Variáveis de ambiente

O projeto inclui um arquivo `.env.example` com valores de exemplo. Não faça commit de arquivos `.env` reais com senhas ou tokens.

## Deploy

O workflow em `.github/workflows/deploy.yml` executa o pipeline de produção:

1. roda os testes com Java 21 e Maven;
2. constrói a imagem Docker com `docker/Dockerfile`;
3. publica a imagem no GitHub Container Registry (GHCR);
4. aciona o deploy no servidor `dsc.rodrigor.com` usando o secret `SSH_DEPLOY_KEY`.

O deploy usa o usuário SSH da equipe `eq13` e publica a aplicação na porta `8113`.

### Secrets no GitHub

Configure em `Settings -> Secrets and variables -> Actions`:

| Secret | Valor |
| --- | --- |
| `SSH_USERNAME` | `eq13` |
| `SSH_DEPLOY_KEY` | chave privada SSH fornecida pela disciplina |

### Variáveis no servidor

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
AWS_S3_ACCESS_KEY=eq13
AWS_S3_SECRET_KEY=<secret-real-do-minio>

ADMIN_USERNAME=admin
ADMIN_PASSWORD=<senha-admin>
```

Depois do primeiro push, confirme se o pacote no GHCR está público para que o servidor consiga baixar a imagem.

O portal da disciplina verifica `GET /ping`. Essa rota é pública e retorna JSON com `status: "ok"` e `service: "eq13"`.
