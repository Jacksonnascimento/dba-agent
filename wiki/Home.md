# 📖 Central de Conhecimento - DBA Agent Wiki

Bem-vindo à Wiki oficial do **DBA Agent**! Este espaço reúne toda a documentação arquitetural, guias de desenvolvimento, manual do usuário e políticas de segurança para apoiar desenvolvedores, engenheiros de DevOps e administradores de banco de dados no uso e manutenção da plataforma.

---

## 🗂️ Índice da Documentação

Para facilitar a navegação, a documentação foi dividida nos seguintes tópicos fundamentais:

### 🚀 [1. Guia do Agente Worker (Go)](Guia-do-Agente.md)
* Guia passo a passo para compilar, configurar e rodar o coletor local em Go.
* Como registrar, iniciar e desinstalar o Agente como um **Serviço do Windows** (`Windows Service`) ou executá-lo no Linux.
* Padrões de conexão de banco de dados local (Microsoft SQL Server e PostgreSQL) sem expor IPs públicos.

### 🛡️ [2. Segurança, Governança e Fluxo de IA](Seguranca-e-Otimizacao.md)
* Detalhamento sobre por que a nossa arquitetura baseada em Agent-Pull é blindada contra ataques de invasão externos.
* Máquina de estados para governança de scripts SQL sugeridos (Aguardando Aprovação ➔ Aprovado ➔ Executado).
* Estratégia de Redução de Custo de Tokens: Funil com **Linter Estático** e **Cache Semântico**.
* O Ciclo de Vida Seguro: Garantia obrigatória de **Up-Scripts (Deploy)** e **Down-Scripts (Rollback)**.

### 🏢 [3. Arquitetura do Monorepo e Guia de Contribuição](Arquitetura-e-Contribuicao.md)
* Visão geral da stack tecnológica: Java 21 (Spring Boot), Next.js (React/TypeScript) e Go.
* Regras do modelo **BYOK (Bring Your Own Key)** para injeção de chaves dos clientes.
* Padrões de desenvolvimento para as camadas do backend (Entity ➔ Repository ➔ Service ➔ Controller) e boas práticas.

---

## 💡 Sobre o DBA Agent

O **DBA Agent** é uma solução corporativa SaaS B2B inovadora. Ele se propõe a resolver um dos maiores gargalos de TI das empresas: **a manutenção e otimização contínua de bancos de dados relacionais**. 

Ao aliar a inteligência coletiva de motores de IA à governança humana e ao isolamento rígido de rede, o DBA Agent atua como um DBA sênior virtual dedicado, aumentando a performance do ERP, diminuindo custos com infraestrutura de nuvem e eliminando riscos de vazamento de dados ou invasões de rede.
