package main

import (
	"context"
	"database/sql"
	"fmt"
	"strings"
	"time"

	_ "github.com/microsoft/go-mssqldb"
)

// ConnectDB inicia a conexão com o SQL Server do cliente
func ConnectDB(connString string) (*sql.DB, error) {
	db, err := sql.Open("sqlserver", connString)
	if err != nil {
		return nil, err
	}

	// Testa a conexão com timeout de 5 segundos
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	err = db.PingContext(ctx)
	if err != nil {
		return nil, err
	}

	return db, nil
}

// loadPrimaryKeyColumns retorna, por tabela dbo, as colunas da PK na ordem do índice
func loadPrimaryKeyColumns(db *sql.DB) (map[string][]string, error) {
	q := `
		SELECT kcu.TABLE_NAME, kcu.COLUMN_NAME
		FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
		INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu
		  ON tc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
		 AND tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
		WHERE tc.TABLE_SCHEMA = 'dbo'
		  AND tc.CONSTRAINT_TYPE = 'PRIMARY KEY'
		ORDER BY kcu.TABLE_NAME, kcu.ORDINAL_POSITION;
	`
	rows, err := db.Query(q)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	out := make(map[string][]string)
	for rows.Next() {
		var tableName, colName string
		if err := rows.Scan(&tableName, &colName); err != nil {
			continue
		}
		out[tableName] = append(out[tableName], colName)
	}
	return out, rows.Err()
}

func appendPrimaryKeyClause(ddl *strings.Builder, tableName string, pkByTable map[string][]string) {
	cols := pkByTable[tableName]
	if len(cols) == 0 {
		return
	}
	ddl.WriteString(", PRIMARY KEY (")
	ddl.WriteString(strings.Join(cols, ", "))
	ddl.WriteString(")")
}

// ExtractSchema lê o INFORMATION_SCHEMA e monta um rascunho de DDL (inclui PK quando existir)
func ExtractSchema(db *sql.DB) (string, error) {
	pkByTable, err := loadPrimaryKeyColumns(db)
	if err != nil {
		return "", err
	}

	query := `
		SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE
		FROM INFORMATION_SCHEMA.COLUMNS
		WHERE TABLE_SCHEMA = 'dbo'
		ORDER BY TABLE_NAME, ORDINAL_POSITION;
	`
	rows, err := db.Query(query)
	if err != nil {
		return "", err
	}
	defer rows.Close()

	var ddl strings.Builder
	currentTable := ""

	for rows.Next() {
		var tableName, colName, dataType string
		if err := rows.Scan(&tableName, &colName, &dataType); err != nil {
			continue
		}

		if tableName != currentTable {
			if currentTable != "" {
				appendPrimaryKeyClause(&ddl, currentTable, pkByTable)
				ddl.WriteString("); \n")
			}
			ddl.WriteString(fmt.Sprintf("CREATE TABLE %s (", tableName))
			currentTable = tableName
		} else {
			ddl.WriteString(", ")
		}
		ddl.WriteString(fmt.Sprintf("%s %s", colName, dataType))
	}
	if currentTable != "" {
		appendPrimaryKeyClause(&ddl, currentTable, pkByTable)
		ddl.WriteString(");")
	}

	return ddl.String(), nil
}

// ExtractDMV lê estatísticas dinâmicas (índices ausentes no SQL Server)
func ExtractDMV(db *sql.DB) (string, error) {
	query := `
		SELECT TOP 5
			mid.statement AS TableName,
			migs.avg_user_impact AS ImprovementMeasure
		FROM sys.dm_db_missing_index_groups mig
		INNER JOIN sys.dm_db_missing_index_group_stats migs ON migs.group_handle = mig.index_group_handle
		INNER JOIN sys.dm_db_missing_index_details mid ON mig.index_handle = mid.index_handle
		ORDER BY migs.avg_user_impact DESC;
	`
	rows, err := db.Query(query)
	if err != nil {
		return "", err
	}
	defer rows.Close()

	var dmv strings.Builder
	dmv.WriteString("Top 5 Missing Indexes Impact (DMV):\n")
	hasData := false
	for rows.Next() {
		hasData = true
		var tableName string
		var impact float64
		if err := rows.Scan(&tableName, &impact); err == nil {
			dmv.WriteString(fmt.Sprintf("Tabela: %s | Impacto Estimado: %.2f%%\n", tableName, impact))
		}
	}
	
	if !hasData {
		return "Nenhum índice ausente crítico detectado nas DMVs.", nil
	}

	return dmv.String(), nil
}

// ExtractWaitStats traz um resumo de waits do SQL Server.
func ExtractWaitStats(db *sql.DB) (string, error) {
	query := `
		SELECT TOP 10
			wait_type,
			wait_time_ms,
			signal_wait_time_ms
		FROM sys.dm_os_wait_stats
		WHERE wait_type NOT LIKE '%SLEEP%'
		ORDER BY wait_time_ms DESC;
	`
	rows, err := db.Query(query)
	if err != nil {
		return "", err
	}
	defer rows.Close()

	var b strings.Builder
	b.WriteString("Top 10 Wait Stats:\n")
	has := false
	for rows.Next() {
		has = true
		var waitType string
		var waitMs, signalMs int64
		if err := rows.Scan(&waitType, &waitMs, &signalMs); err == nil {
			b.WriteString(fmt.Sprintf("%s | wait_ms=%d | signal_ms=%d\n", waitType, waitMs, signalMs))
		}
	}
	if !has {
		return "Sem dados de wait stats.", nil
	}
	return b.String(), nil
}

// ExtractTopQueries traz um resumo das queries mais caras (CPU / Duration).
func ExtractTopQueries(db *sql.DB) (string, error) {
	query := `
		SELECT TOP 10
			qs.total_worker_time,
			qs.total_elapsed_time,
			qs.execution_count,
			SUBSTRING(st.text, (qs.statement_start_offset/2)+1,
				(CASE WHEN qs.statement_end_offset = -1
					THEN LEN(CONVERT(nvarchar(max), st.text)) * 2
					ELSE qs.statement_end_offset END - qs.statement_start_offset)/2 + 1) AS statement_text
		FROM sys.dm_exec_query_stats qs
		CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) st
		ORDER BY qs.total_elapsed_time DESC;
	`
	rows, err := db.Query(query)
	if err != nil {
		return "", err
	}
	defer rows.Close()

	var b strings.Builder
	b.WriteString("Top 10 Queries (elapsed):\n")
	has := false
	for rows.Next() {
		has = true
		var cpu, elapsed, execCount int64
		var text string
		if err := rows.Scan(&cpu, &elapsed, &execCount, &text); err == nil {
			clean := strings.ReplaceAll(text, "\n", " ")
			if len(clean) > 400 {
				clean = clean[:400] + "..."
			}
			b.WriteString(fmt.Sprintf("elapsed=%d cpu=%d exec=%d | %s\n", elapsed, cpu, execCount, clean))
		}
	}
	if !has {
		return "Sem dados de query stats.", nil
	}
	return b.String(), nil
}

// ExtractIndexStats traz um resumo de fragmentação e tamanho.
func ExtractIndexStats(db *sql.DB) (string, error) {
	query := `
		SELECT TOP 10
			OBJECT_NAME(s.object_id) AS table_name,
			i.name AS index_name,
			s.avg_fragmentation_in_percent,
			s.page_count
		FROM sys.dm_db_index_physical_stats(DB_ID(), NULL, NULL, NULL, 'SAMPLED') s
		INNER JOIN sys.indexes i
			ON i.object_id = s.object_id AND i.index_id = s.index_id
		WHERE s.index_id > 0
		ORDER BY s.avg_fragmentation_in_percent DESC;
	`
	rows, err := db.Query(query)
	if err != nil {
		return "", err
	}
	defer rows.Close()

	var b strings.Builder
	b.WriteString("Top 10 Index Fragmentation:\n")
	has := false
	for rows.Next() {
		has = true
		var table, idx string
		var frag float64
		var pages int64
		if err := rows.Scan(&table, &idx, &frag, &pages); err == nil {
			b.WriteString(fmt.Sprintf("%s.%s | frag=%.2f%% pages=%d\n", table, idx, frag, pages))
		}
	}
	if !has {
		return "Sem dados de index stats.", nil
	}
	return b.String(), nil
}

// ExtractExecutionPlans retorna os planos XML das queries mais caras.
func ExtractExecutionPlans(db *sql.DB) (string, error) {
	query := `
		SELECT TOP 5
			qp.query_plan
		FROM sys.dm_exec_query_stats qs
		CROSS APPLY sys.dm_exec_query_plan(qs.plan_handle) qp
		ORDER BY qs.total_elapsed_time DESC;
	`
	rows, err := db.Query(query)
	if err != nil {
		return "", err
	}
	defer rows.Close()

	var b strings.Builder
	b.WriteString("Top 5 Execution Plans (XML):\n")
	has := false
	i := 0
	for rows.Next() {
		has = true
		var plan string
		if err := rows.Scan(&plan); err == nil {
			i++
			if len(plan) > 4000 {
				plan = plan[:4000] + "...(truncated)"
			}
			b.WriteString(fmt.Sprintf("Plan #%d:\n%s\n\n", i, plan))
		}
	}
	if !has {
		return "Sem planos disponíveis via DMV.", nil
	}
	return b.String(), nil
}

// ExecuteScript roda o UpScript aprovado no banco do cliente
func ExecuteScript(db *sql.DB, script string) error {
	_, err := db.Exec(script)
	return err
}