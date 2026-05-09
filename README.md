# dba-agent

## Agent Multi-Banco (produção)

O worker em `agent/` suporta múltiplos bancos no mesmo processo usando `DBA_TARGETS_JSON`.
Cada alvo deve ter token próprio (um token por `database_connection` no backend).

Exemplo:

```json
[
  {
    "name": "erp-sqlserver-prod",
    "dbEngine": "SQL Server",
    "dbConnString": "sqlserver://user:pass@host:1433?database=ERP",
    "agentToken": "TOKEN_DO_BANCO_1"
  },
  {
    "name": "finance-postgres",
    "dbEngine": "PostgreSQL",
    "dbConnString": "",
    "agentToken": "TOKEN_DO_BANCO_2"
  }
]
```

> Hoje o extractor nativo está pronto para SQL Server; engines diferentes entram em modo fallback/mock até o extractor específico ser adicionado.

