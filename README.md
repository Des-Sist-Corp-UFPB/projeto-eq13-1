# JobHub PB - Sistema de Vagas

Projeto desenvolvido para a disciplina de Desenvolvimento de Sistemas Corporativos da UFPB. O JobHub PB e um portal para divulgacao, busca e gerenciamento de vagas de tecnologia, com foco em oportunidades remotas e presenciais na Paraiba.

## Visao geral

O sistema permite que visitantes consultem vagas, filtrem oportunidades e enviem candidaturas. Tambem existe uma area administrativa para moderacao das vagas, acompanhamento das candidaturas e visualizacao de indicadores do portal.

## Tecnologias

| Camada | Tecnologia |
| --- | --- |
| Backend | Java 21 + Spring Boot 3.5 |
| Web | Spring MVC + Thymeleaf |
| Seguranca | Spring Security |
| Banco de dados | PostgreSQL |
| Migracoes | Flyway |
| Build | Maven |
| Infraestrutura | Docker + Docker Compose |
| CI/CD | GitHub Actions + GHCR |

## Funcionalidades

- Pagina inicial com destaques do portal.
- Listagem publica de vagas.
- Filtro por termo de busca e modelo de trabalho.
- Pagina de detalhes da vaga.
- Formulario publico de candidatura.
- Pagina publica para divulgar novas vagas.
- Login administrativo.
- Painel administrativo com indicadores.
- Moderacao e gerenciamento de vagas.
- Visualizacao de candidaturas recebidas.
- Health check publico em `GET /ping`.

## Requisitos

- Java 21
- Maven
- Docker Desktop ou Docker Engine
- PostgreSQL via Docker Compose

## Como rodar localmente

Antes de iniciar a aplicacao, suba o PostgreSQL:

```bash
docker compose -f docker/docker-compose.dev.yml up -d
```

Depois rode a aplicacao Spring Boot:

```bash
mvn spring-boot:run
```

A aplicacao ficara disponivel em:

```text
http://localhost:8080
```

## Acesso administrativo local

```text
Usuario: admin
Senha: admin123
```

## Banco de dados local

O ambiente de desenvolvimento usa PostgreSQL no Docker.

| Configuracao | Valor |
| --- | --- |
| Host | `localhost` |
| Porta | `5432` |
| Banco | `jobhub_dev` |
| Usuario | `jobhub` |
| Senha | `jobhub123` |

As tabelas sao criadas automaticamente pelas migrations Flyway em `src/main/resources/db/migration`.

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
|   |-- config/               # Configuracoes da aplicacao
|   |-- controller/           # Controllers HTTP
|   |-- domain/               # Entidades JPA
|   |-- dto/                  # Objetos de transferencia/formularios
|   |-- repository/           # Repositorios Spring Data JPA
|   `-- service/              # Regras de negocio
|-- src/main/resources/
|   |-- db/migration/         # Migrations Flyway
|   |-- static/               # Arquivos estaticos
|   `-- templates/            # Paginas Thymeleaf
|-- src/test/                 # Testes automatizados
`-- pom.xml                   # Configuracao Maven
```

## Variaveis de ambiente

O projeto inclui um arquivo `.env.example` com valores de exemplo. Nao faca commit de arquivos `.env` reais com senhas ou tokens.

## Deploy

O workflow em `.github/workflows/deploy.yml` publica a imagem Docker no GitHub Container Registry e executa o deploy no servidor da disciplina usando o secret `SSH_DEPLOY_KEY`.

Depois do primeiro push, confirme se o pacote no GHCR esta publico para que o servidor consiga baixar a imagem.
