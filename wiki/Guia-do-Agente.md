# 🛰️ Guia do Agente Worker (Coletor Go)

O **DBA Agent Worker** é um programa leve escrito em Go projetado para ser executado diretamente nos servidores internos do cliente (On-Premises ou Nuvem Privada). Ele é responsável por coletar dados vitais de performance do banco de dados e enviá-los de forma segura via HTTPS para a API Central.

---

## 🛠️ Requisitos de Instalação

* **Compilação:** Go Compiler versão `1.26` ou superior.
* **Acesso à Rede:** 
  * Acesso de leitura ao banco de dados local (SQL Server ou PostgreSQL).
  * Acesso de saída à internet (HTTPS - Porta `443`) apenas para enviar dados ao domínio da API Central.
  * **Zero portas de entrada abertas:** O Agente não precisa de nenhuma porta escutando requisições externas, garantindo que ele seja invulnerável a port-scans.

---

## 📝 Configuração do Agente (`.env`)

Na pasta `/agent`, crie um arquivo `.env` com as seguintes variáveis estruturais:

```env
# URL da API Central do SaaS
API_URL=http://localhost:8080/api/v1

# Token de autenticação fornecido no Dashboard (vinculado ao Tenant do Cliente)
AGENT_TOKEN=seu-token-jwt-gerado-no-front

# Driver do banco analisado (mssql ou postgres)
DB_DRIVER=mssql

# String de conexão local segura
DB_CONNECTION_STRING=server=127.0.0.1;port=1433;database=meu_banco_producao;user id=sa;password=SenhaSuperSegura123;encrypt=disable;
```

---

## ⚙️ Registro como Serviço do Windows (`Windows Service`)

Em servidores de banco de dados rodando em ambiente Windows Server, o recomendável é registrar o Agente para rodar de forma perene no background. A ferramenta possui suporte nativo para isso:

### 1. Compilação do Binário
Abra o PowerShell como Administrador na pasta `/agent` e execute o script de build:
```powershell
.\build.ps1
```
Isso gerará o executável `dba-agent.exe`.

### 2. Instalação e Execução
Execute os seguintes comandos para registrar o executável e inicializar o serviço:

```powershell
# Instala o dba-agent no Registro do Windows Services
.\dba-agent.exe -service install

# Inicia o serviço em background
.\dba-agent.exe -service start
```

### 3. Gerenciamento do Serviço
Se precisar atualizar o binário, pausar a coleta ou remover a instalação:

```powershell
# Parar o serviço ativo
.\dba-agent.exe -service stop

# Remover o serviço do Windows
.\dba-agent.exe -service uninstall
```

---

## 🔍 Coleta de Métricas: O que o Agente lê?

O Agente atua de forma estritamente cirúrgica para **não ler ou exportar dados sensíveis dos clientes** (como CPF, CNPJ, dados de clientes, transações financeiras, etc.). Ele lê apenas dados analíticos:

1. **Metadata DDL (SQL Server & Postgres):** Estrutura física das tabelas, chaves primárias, estrangeiras e índices existentes usando as tabelas do `INFORMATION_SCHEMA`.
2. **Dynamic Management Views (DMVs) - SQL Server:**
   * **Índices Ausentes:** Estatísticas geradas pelo query optimizer sugerindo a ausência de índices (`sys.dm_db_missing_index_details`).
   * **Wait Stats:** Estatísticas acumuladas de gargalos (CPU, Disco, Rede, Memória, Locks) que causam lentidão global no banco (`sys.dm_os_wait_stats`).
   * **Queries Lentas:** Planos de execução e tempos de CPU de queries de alta latência (`sys.dm_exec_query_stats`).
3. **Métricas Equivalentes - PostgreSQL:**
   * Estatísticas de uso de tabelas (`pg_stat_user_tables`).
   * Desempenho e latência de consultas (`pg_stat_statements`).
   * Gargalos de lock em tempo real (`pg_locks`).
