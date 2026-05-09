# Documento de Visão de Produto e Decisões Arquiteturais (ADR) - DBA Agent

Este documento consolida a visão estratégica, o escopo do produto e as regras fundamentais de arquitetura para o SaaS **DBA Agent**. Ele serve como a "fonte da verdade" para qualquer desenvolvimento futuro.

## 1. Visão Geral do Produto
O DBA Agent é um SaaS B2B focado em análise, diagnóstico e otimização automatizada de bancos de dados. Ele não se limita a análises superficiais de DDL; atua como um DBA autônomo, analisando estruturas complexas, métricas de performance e garantindo a aplicação segura de melhorias com governança completa.

## 2. Decisões Arquiteturais Estratégicas

### 2.1. Funil de Processamento (Eficiência e Custos)
A análise de dados segue uma ordem rigorosa para minimizar custos de IA e garantir baixa latência:
1. **Passo 1: Motor de Linter (Regras Estáticas):** Erros básicos e determinísticos (ex: falta de Primary Key, FK sem índice, tipos de dados obsoletos) são barrados e respondidos localmente pelo sistema, com custo zero.
2. **Passo 2: Cache Semântico:** Verifica no banco relacional interno da aplicação (`dba_agent_db`) se uma estrutura idêntica já foi analisada anteriormente.
3. **Passo 3: Motor de IA (LLMs):** Acionado apenas para problemas de alta complexidade. 

### 2.2. Modelo de Negócios e API: BYOK (Bring Your Own Key)
Para viabilizar a escala do SaaS sem estrangular os custos de infraestrutura ou esbarrar em *rate limits*:
* A plataforma adota o modelo **BYOK**.
* Os clientes finais fornecem e gerenciam as suas próprias chaves de API das provedoras de IA (OpenAI, Google Gemini, Anthropic Claude).
* A chave do cliente será armazenada de forma segura (criptografada) no banco de dados, associada ao seu *Tenant*/Usuário, e injetada dinamicamente pelo backend em cada requisição.

### 2.3. Profundidade de Análise (Contexto Amplo)
A ferramenta rejeita o modelo amador de "análise de tabelas isoladas". O motor de IA receberá um contexto relacional e dinâmico:
* **Estrutura de Dados:** Tabelas correlacionadas (JOINs), Views e Stored Procedures.
* **Comportamento e Estatísticas:** Métricas vitais capturadas pelo Agente, como DMVs, fragmentação de índices, *Wait Stats*, *Execution Plans* (Planos de Execução) e tabelas com alto índice de *lock*.

### 2.4. Governança e Máquina de Estados
A IA não atua diretamente no banco do cliente sem supervisão. O fluxo de aplicação obedece a uma máquina de estados:
* Toda sugestão gerada pela IA entra no status `AGUARDANDO_APROVACAO`.
* O usuário interage com uma interface web para aprovar a mudança.
* Após aprovação, a API notifica o Agente (instalado no servidor do cliente) para executar o script.
* O banco de dados interno da aplicação registra toda a trilha de auditoria (Quem aprovou, quando executou, qual foi o impacto).

### 2.5. Ciclo de Vida Seguro: Up-Scripts e Down-Scripts
Toda solução de banco de dados gerada pela plataforma (seja via IA ou Linter) deve obrigatoriamente possuir dois artefatos:
* **Up-Script:** O código SQL necessário para aplicar a melhoria sugerida.
* **Down-Script (Rollback):** O código SQL exato para desfazer a alteração caso haja regressão de performance.

### 2.6. Feature para MSPs: Repositório de Soluções Exportáveis
O SaaS atua como uma base de conhecimento corporativa.
* Sugestões validadas com sucesso poderão ser convertidas em arquivos textuais `.sql` e baixadas pela interface.
* Essa feature permite que Consultorias de TI e MSPs (Managed Service Providers) repliquem otimizações em outros clientes que utilizam o mesmo software/ERP (ex: TOTVS, SAP), transformando a ferramenta em um centralizador de soluções validadas.