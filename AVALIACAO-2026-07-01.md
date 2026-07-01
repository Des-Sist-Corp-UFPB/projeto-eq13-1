# Avaliação — EQ13 (DSC)

**Data:** 2026-07-01  
**Avaliador:** Prof. Rodrigo  
**Método:** verificação automática cruzando o que o `README.md` declara com evidências no código-fonte (leitura de `origin/main`).

> Esta é uma avaliação automática preliminar. O que não estiver documentado no README e commitado no repositório é considerado não atendido.

---

## 1. Log de Auditoria

✅ **Atendido** — documentado no README e com 106 evidência(s) no código.

---

## 2. Integração com Serviço Externo

- ✅ **Stripe** — declarado no README e comprovado no código (11 ocorrência(s)).
  - Evidência: `docker-compose.yml:21:      STRIPE_SECRET_KEY: ${STRIPE_SECRET_KEY:-}`
- ❌ **AWS S3** — **declarado no README, mas SEM evidência no código.**
- ✅ **MinIO** — declarado no README e comprovado no código (3 ocorrência(s)).
  - Evidência: `docker-compose.yml:12:      AWS_S3_ENDPOINT: ${AWS_S3_ENDPOINT:-http://minio:9000}`

---

## 3. Cobertura de Testes (≥ 85%)

❌ **Não comprovado** — o README menciona cobertura, mas **nenhum relatório foi commitado** na pasta `cobertura/`.

> Observação: a cobertura é lida do relatório commitado pela equipe; não é recalculada nesta avaliação.

---

*Avaliação gerada automaticamente em 2026-07-01. Consulte `ORIENTACOES-AVALIACAO-2026-06-29.md` para os critérios.*