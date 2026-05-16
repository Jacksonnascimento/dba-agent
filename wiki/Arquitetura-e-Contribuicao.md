# 🏢 Arquitetura do Monorepo e Guia de Contribuição

Esta seção destina-se a engenheiros e programadores que darão manutenção ou estenderão as funcionalidades do ecossistema do **DBA Agent**.

---

## 🛠️ Stack Tecnológica Consolidada

O projeto foi projetado como um monorepo para facilitar o compartilhamento de especificações de DTOs e agilizar testes integrados de ponta a ponta.

### 🌐 Frontend (Dashboard)
* **Estrutura:** `/web`
* **Tecnologias:** Next.js 15, React 19, TypeScript, TailwindCSS.
* **Componentes Chave:**
  * `AuthProvider.tsx`: Armazena tokens JWT locais e gerencia estado de login do usuário.
  * `apiFetch` (`lib/api.ts`): Wrapper do Fetch API com injeção automática de Headers de autorização Bearer.

### ☕ Backend Central (API Central)
* **Estrutura:** `/api`
* **Tecnologias:** Spring Boot 3.4.0 (Java 21), Spring Data JPA, Spring Security, Hibernate.
* **Banco de Dados Central:** PostgreSQL (nome default: `dba_agent_db`).
* **Segurança:** Filtros JWT customizados no escopo de requisições HTTP, protegendo todos os endpoints da API Central.

### 🛰️ Worker local (Agente)
* **Estrutura:** `/agent`
* **Tecnologias:** Go (Golang) 1.26.2.
* **Acessibilidade:** Desenvolvido sem pacotes pesados ou bibliotecas compartilhadas complexas, sendo compilado em um binário executável estático leve de fácil distribuição.

---

## 🔑 O Modelo BYOK (Bring Your Own Key)

Como a análise de IA é computacionalmente onerosa, o SaaS adota o modelo **BYOK**:
1. Os administradores de cada empresa (*Tenant*) cadastram suas próprias chaves de API na tela de configurações (`BYOK / Config`) para o **Google Gemini** ou **Anthropic Claude**.
2. A API Central criptografa essas chaves e as armazena na tabela de `Tenants` no banco PostgreSQL.
3. Nas chamadas para as LLMs, o backend descriptografa a chave em memória e injeta-a dinamicamente como Header ou parâmetro nas requisições às provedoras de IA, garantindo que o custo de tokens seja rateado individualmente e com total isolamento.

---

## 📐 Padrões de Projeto do Backend (Java/Spring Boot)

O backend segue rigidamente a **Arquitetura em Camadas (Layered Architecture)**. Ao codificar novos recursos, certifique-se de respeitar o fluxo e isolamento de cada camada:

```
Requisição HTTP ➔ Controller ➔ Service ➔ Repository ➔ PostgreSQL DB
```

### 1. Controllers (`com.dbaagent.api.controllers`)
* **Responsabilidade:** Mapear rotas REST, validar payloads e formatos básicos de DTOs, e delegar a execução às Services.
* **Regra de Ouro:** **Sem lógica de negócio nos Controllers.** Tratamento de erros HTTP deve ser delegado ao `ControllerAdvice` global.

### 2. Services (`com.dbaagent.api.services`)
* **Responsabilidade:** Centralizar todas as regras de negócio, transações (`@Transactional`), orquestração do funil de otimização e integrações externas (Gemini/Claude).
* **Regra de Ouro:** Camadas de transações de banco de dados devem ser totalmente finalizadas e comitadas no fechamento do método da Service.

### 3. Repositories (`com.dbaagent.api.repositories`)
* **Responsabilidade:** Consultas ao banco PostgreSQL Central via Spring Data JPA.
* **Regra de Ouro:** Para evitar problemas de proxies lazy-load desacoplados (`LazyInitializationException`) ao renderizar ou mapear dados em DTOs fora do escopo da transação, utilize queries com `JOIN FETCH` (ex: `findByIdWithTenant` ou `findAllWithTenant`) sempre que a entidade filha (como `Tenant`) for obrigatória na resposta do payload HTTP.

### 4. Entities (`com.dbaagent.api.entities`)
* **Responsabilidade:** Mapear o esquema das tabelas físicas do banco central usando anotações JPA/Hibernate (`@Entity`, `@Table`, `@Id`).

---

## 🤝 Fluxo de Trabalho e Git

* **Branches:** Mantenha a branch `main` sempre estável. Desenvolva melhorias em branches do tipo `feature/nome-da-feature` ou correções em `bugfix/nome-do-bug`.
* **Commits:** Siga boas práticas de commits semânticos:
  * `feat: ...` (novas funcionalidades)
  * `fix: ...` (correções de bugs)
  * `docs: ...` (documentações e readme)
  * `refactor: ...` (alteração de código sem alterar comportamento)
