# JobHub PB

Portal full stack para vagas de tecnologia remotas e vagas presenciais na Paraiba.

## Stack

- Java 21
- Spring Boot 3.5
- Spring MVC + Thymeleaf
- Spring Security
- PostgreSQL
- Flyway
- Docker + GitHub Actions

## Funcionalidades

- Listagem publica de vagas
- Filtros por termo e modelo de trabalho
- Detalhe da vaga e candidatura
- Pagina publica para divulgar vagas
- Moderacao de vagas pelo admin
- Dashboard com indicadores de vagas, candidaturas, visualizacoes e atividade recente
- Health check publico em `GET /ping`

## Rodar local

```bash
docker compose -f docker/docker-compose.dev.yml up -d
mvn spring-boot:run
```

Acesse `http://localhost:8080`.

Admin local:

- usuario: `admin`
- senha: `admin123`

## Deploy DSC

O workflow em `.github/workflows/deploy.yml` publica a imagem no GHCR e chama o deploy no servidor da disciplina via `SSH_DEPLOY_KEY`.

Depois do primeiro push, o package do GHCR precisa estar publico para o servidor conseguir baixar a imagem.
