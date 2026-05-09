Documento de Fundação - Projeto DBA Agent
1. Visão Geral do Sistema
O "Projeto DBA Agent" é um SaaS B2B projetado para atuar como um Administrador de Banco de Dados (DBA) automatizado. O sistema analisa a estrutura, o uso e o desempenho de bancos de dados relacionais (foco primário em SQL Server, mas com arquitetura agnóstica para suportar PostgreSQL e outros) e utiliza Inteligência Artificial (Google Gemini e Anthropic Claude) para sugerir melhorias de performance (ex: criação de índices, reestruturação de tabelas, otimização de queries). O usuário aprova as melhorias e o sistema fornece scripts de Deploy e Rollback.

2. Arquitetura Geral (Baseada em Agente)
Por questões rigorosas de segurança, o sistema nunca se conecta diretamente ao banco do cliente via IP externo. A arquitetura é dividida em três componentes principais:

Agente (Worker): Executável leve que roda na infraestrutura local do cliente. Ele se conecta ao banco do cliente, extrai metadados (DDL via INFORMATION_SCHEMA) e estatísticas dinâmicas de uso (DMVs no SQL Server, como queries lentas e índices ausentes). Envia esses dados via HTTPS para a API Central.

API Central (Backend): O núcleo da regra de negócio. Recebe os dados dos Agentes, verifica o Cache Semântico, faz a interface com as APIs das IAs (Gemini e Claude) e persiste as informações no banco de dados interno da aplicação.

Frontend Unificado (Dashboard): Aplicação web única que atende tanto os clientes finais (para aprovação de melhorias nos bancos deles) quanto os administradores da plataforma (para gestão de usuários, análise de consumo de tokens e métricas). O controle visual é feito via RBAC (Role-Based Access Control).

3. Stack Tecnológica

Backend Central: Java com Spring Boot.

Banco de Dados Interno (da aplicação): PostgreSQL (armazena usuários, chaves de API, logs de execução, cache semântico de melhorias sugeridas por IAs).

Frontend: Next.js (React), utilizando as melhores práticas para interfaces SaaS.

Agente: Java (preferencialmente compilado nativamente via GraalVM para facilitar a distribuição sem necessidade de JRE na máquina do cliente).

4. Estrutura do Repositório (Monorepo)
O projeto deve seguir uma estrutura de Monorepo, separando os contextos claramente na raiz:

/agent: Código-fonte do Agente coletor.

/api: Código-fonte do backend em Spring Boot.

/web: Código-fonte do frontend em Next.js.

5. Padrões de Projeto e Arquitetura do Backend (Spring Boot)
O backend deve obedecer estritamente à Arquitetura em Camadas (Layered Architecture):

Controllers: Apenas endpoints HTTP. Sem lógica de negócio.

Services: Regras de negócio, lógica de cache, chamadas para IAs externas.

Repositories: Interfaces do Spring Data JPA comunicando-se com o PostgreSQL.

Entities: Mapeamento das tabelas do banco interno.

DTOs: Objetos de transferência de dados para requisições e respostas.

Config: Configurações globais (Segurança, CORS, RestTemplates/WebClients).

Exceptions: Tratamento global de erros (ControllerAdvice).

6. Fluxo de Otimização e Regras de Negócio (Obrigatório)

Passo 1 (Linter Base): A API sempre roda validações estruturais gratuitas baseadas em regras pré-definidas (ex: foreign keys sem index) ANTES de acionar a IA.

Passo 2 (Cache Semântico): A API verifica no PostgreSQL se uma estrutura idêntica já foi analisada anteriormente. Se sim, devolve a melhoria do cache, identificando qual IA gerou (Gemini ou Claude), economizando tokens.

Passo 3 (Análise de IA): A IA só é acionada para análises complexas, recebendo não apenas a estrutura, mas os dados de uso (DMVs).

Passo 4 (Deploy e Rollback): Para toda melhoria aprovada e gerada pela IA, o sistema OBRIGATORIAMENTE deve gerar e salvar o script de aplicação (Deploy) e o script de reversão (Rollback).

7. Metodologia de Trabalho (Vibe Coding)
O desenvolvimento será guiado pelo Tech Lead (o usuário). O assistente (IA) atua como desenvolvedor sênior executor. Código gerado deve ser modular, limpo, seguir os princípios SOLID e ser entregue na íntegra (classes/arquivos completos) para substituição imediata e testes rápidos.