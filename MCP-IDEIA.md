# Ideia de Servidor MCP — EQ13

**Domínio:** RadarTech PB — portal de vagas de TI (JobPosting, CandidateApplication, OAuth2, auditoria)  
**Data:** 2026-07-01

## O que é

Um **servidor MCP (Model Context Protocol)** expõe as operações do seu sistema como *tools* e *resources* que qualquer assistente de IA (Claude Desktop, Cursor, etc.) pode chamar com segurança. Na prática, é uma camada fina sobre a **API que vocês já têm** — cada tool chama um endpoint/service existente. Assim o RadarTech deixa de ser só uma tela e passa a ser operável por um agente de IA.

## Servidor proposto: `radartech-mcp`

### Tools sugeridas

- `buscar_vagas(area, tipo_contrato, modalidade)` — filtra vagas de TI
- `detalhes_vaga(id)` — descrição, requisitos e empresa
- `candidatar(vagaId, userId)` — registra candidatura
- `minhas_candidaturas(userId)` — acompanha o status das candidaturas
- `publicar_vaga(dados)` — cria vaga (perfil recrutador/admin)

### Resources (somente leitura)

- lista de vagas abertas como resource
- trilha de auditoria (AuditLog) — útil para o requisito da disciplina

### Exemplos de uso com um LLM

- "Ache vagas de back-end Java remotas em João Pessoa e me candidate às 3 mais aderentes ao meu perfil."
- "Quais candidaturas minhas ainda estão em análise?"
- "Publique uma vaga de estágio em front-end React, híbrida, CLT."

## Esqueleto para começar (Java / Spring AI)

```java
// pom.xml: org.springframework.ai:spring-ai-starter-mcp-server-webmvc
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class RadarTechTools {

    private final JobPostingService jobService;   // reaproveite seus services

    public RadarTechTools(JobPostingService jobService) { this.jobService = jobService; }

    @Tool(description = "Filtra vagas de TI por área, tipo de contrato e modalidade")
    public Object buscar_vagas(String area, String tipoContrato, String modalidade) {
        return jobService.buscar(area, tipoContrato, modalidade);   // sua lógica atual
    }
}
```
> Registre as tools com um `MethodToolCallbackProvider` (bean) apontando para esta classe.

## Boas práticas

- **Segurança:** tools que alteram dados (candidatar, publicar_vaga) devem exigir autenticação (vocês já têm OAuth2) e registrar no **log de auditoria**.
- **Escopo mínimo:** separe tools de leitura (buscar/detalhar) das de escrita (candidatar/publicar).
- **Reaproveite:** as tools devem chamar seus *services* existentes, não reimplementar regra de negócio.

## Referências
- Documentação MCP: https://modelcontextprotocol.io
- SDKs: Python (`mcp`), TypeScript (`@modelcontextprotocol/sdk`), Java (Spring AI MCP Server).

*Sugestão gerada em 2026-07-01 para orientar a integração de LLMs ao projeto.*
