package main

import (
	"context"
	"database/sql"
	"fmt"
	"net/url"
	"strings"
	"time"

	_ "github.com/lib/pq"
	_ "github.com/microsoft/go-mssqldb"
)

// normalizePostgresURI converte a URI interna (postgres://user:pass@host:port?database=db&encrypt=disable)
// para o formato aceito pelo driver lib/pq (postgres://user:pass@host:port/db?sslmode=disable)
func normalizePostgresURI(rawURI string) (string, error) {
	u, err := url.Parse(rawURI)
	if err != nil {
		return "", fmt.Errorf("URI Postgres inválida: %w", err)
	}
	q := u.Query()

	// Move o parâmetro "database" para o path
	dbName := q.Get("database")
	if dbName != "" {
		u.Path = "/" + dbName
		q.Del("database")
	}

	// Remove parâmetros não suportados pelo lib/pq
	q.Del("encrypt")

	// Garante sslmode=disable para ambientes sem TLS configurado
	if q.Get("sslmode") == "" {
		q.Set("sslmode", "disable")
	}

	u.RawQuery = q.Encode()
	return u.String(), nil
}

// ConnectDB abre a conexão com o banco correto baseado na engine
func ConnectDB(dbEngine string, connString string) (*sql.DB, error) {
	var driverName, dsn string
	var err error

	if strings.EqualFold(dbEngine, "PostgreSQL") {
		driverName = "postgres"
		dsn, err = normalizePostgresURI(connString)
		if err != nil {
			return nil, err
		}
	} else {
		driverName = "sqlserver"
		dsn = connString
	}

	db, err := sql.Open(driverName, dsn)
	if err != nil {
		return nil, err
	}

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	if err = db.PingContext(ctx); err != nil {
		db.Close()
		return nil, err
	}

	return db, nil
}

// ──────────────────────────────────────────────────────────────────────────────
// SQL SERVER
// ──────────────────────────────────────────────────────────────────────────────

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

// ExtractSchema lê o INFORMATION_SCHEMA e monta um DDL (SQL Server, schema dbo)
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

// ExtractDMV lê estatísticas de índices ausentes (SQL Server)
func ExtractDMV(db *sql.DB) (string, error) {
	query := `
		SELECT TOP 5
			mid.statement AS TableName,
			migs.avg_user_impact AS ImprovementMeasure
		FROM sys.dm_db_missing_index_groups mig
		INNER JOIN sys.dm_db_missing_index_group_stats migs ON migs.group_handle = mig.index_group_handle
		INNER JOIN sys.dm_db_missing_index_details mid ON mig.index_handle = mid.index_handle
		WHERE mid.database_id = DB_ID()
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
		return "Nenhum índice ausente crítico detectado nas DMVs para este banco.", nil
	}
	return dmv.String(), nil
}

// ExtractWaitStats traz wait stats (SQL Server)
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

// ExtractTopQueries traz queries mais caras (SQL Server)
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
		WHERE st.dbid = DB_ID()
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
		return "Sem dados de query stats para este banco.", nil
	}
	return b.String(), nil
}

// ExtractIndexStats traz fragmentação de índices (SQL Server)
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

// ExtractExecutionPlans retorna planos XML das queries mais caras (SQL Server)
func ExtractExecutionPlans(db *sql.DB) (string, error) {
	query := `
		SELECT TOP 5
			qp.query_plan
		FROM sys.dm_exec_query_stats qs
		CROSS APPLY sys.dm_exec_query_plan(qs.plan_handle) qp
		CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) st
		WHERE st.dbid = DB_ID()
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
		return "Sem planos disponíveis via DMV para este banco.", nil
	}
	return b.String(), nil
}

// ExecuteScript roda um script SQL no banco do cliente (genérico)
func ExecuteScript(db *sql.DB, script string) error {
	_, err := db.Exec(script)
	return err
}

// ──────────────────────────────────────────────────────────────────────────────
// POSTGRESQL
// ──────────────────────────────────────────────────────────────────────────────

// ExtractSchemaPostgres monta DDL do schema 'public' via information_schema
func ExtractSchemaPostgres(db *sql.DB) (string, error) {
	pkQuery := `
		SELECT kcu.table_name, kcu.column_name
		FROM information_schema.table_constraints tc
		INNER JOIN information_schema.key_column_usage kcu
		       ON tc.constraint_schema = kcu.constraint_schema
		      AND tc.constraint_name   = kcu.constraint_name
		WHERE tc.table_schema = 'public'
		  AND tc.constraint_type = 'PRIMARY KEY'
		ORDER BY kcu.table_name, kcu.ordinal_position;
	`
	pkRows, err := db.Query(pkQuery)
	if err != nil {
		return "", err
	}
	defer pkRows.Close()
	pkByTable := make(map[string][]string)
	for pkRows.Next() {
		var tbl, col string
		if err := pkRows.Scan(&tbl, &col); err == nil {
			pkByTable[tbl] = append(pkByTable[tbl], col)
		}
	}

	colQuery := `
		SELECT table_name, column_name, data_type
		FROM information_schema.columns
		WHERE table_schema = 'public'
		ORDER BY table_name, ordinal_position;
	`
	rows, err := db.Query(colQuery)
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
				if cols := pkByTable[currentTable]; len(cols) > 0 {
					ddl.WriteString(fmt.Sprintf(", PRIMARY KEY (%s)", strings.Join(cols, ", ")))
				}
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
		if cols := pkByTable[currentTable]; len(cols) > 0 {
			ddl.WriteString(fmt.Sprintf(", PRIMARY KEY (%s)", strings.Join(cols, ", ")))
		}
		ddl.WriteString(");")
	}
	return ddl.String(), nil
}

// ExtractMissingIndexesPostgres detecta tabelas com muitos sequential scans
func ExtractMissingIndexesPostgres(db *sql.DB) (string, error) {
	query := `
		SELECT relname AS table_name,
		       seq_scan,
		       seq_tup_read,
		       COALESCE(idx_scan, 0) AS idx_scan,
		       CASE WHEN COALESCE(idx_scan, 0) = 0 THEN seq_scan::float
		            ELSE seq_scan::float / idx_scan END AS seq_to_idx_ratio
		FROM pg_stat_user_tables
		WHERE seq_scan > 100
		ORDER BY seq_to_idx_ratio DESC
		LIMIT 10;
	`
	rows, err := db.Query(query)
	if err != nil {
		return "Dados de missing indexes indisponíveis (pg_stat_user_tables).", nil
	}
	defer rows.Close()

	var b strings.Builder
	b.WriteString("Top 10 Tabelas com Alto Sequential Scan (possível índice ausente):\n")
	has := false
	for rows.Next() {
		has = true
		var table string
		var seqScan, seqTupRead, idxScan int64
		var ratio float64
		if err := rows.Scan(&table, &seqScan, &seqTupRead, &idxScan, &ratio); err == nil {
			b.WriteString(fmt.Sprintf("Tabela: %s | seq_scan=%d | idx_scan=%d | ratio=%.2f\n", table, seqScan, idxScan, ratio))
		}
	}
	if !has {
		return "Nenhum índice ausente crítico detectado.", nil
	}
	return b.String(), nil
}

// ExtractWaitStatsPostgres retorna wait events via pg_stat_activity
func ExtractWaitStatsPostgres(db *sql.DB) (string, error) {
	query := `
		SELECT COALESCE(wait_event_type, 'None') AS wait_event_type,
		       COALESCE(wait_event, 'None')      AS wait_event,
		       COUNT(*) AS sessions
		FROM pg_stat_activity
		WHERE wait_event IS NOT NULL
		  AND state != 'idle'
		  AND datname = current_database()
		GROUP BY wait_event_type, wait_event
		ORDER BY sessions DESC
		LIMIT 10;
	`
	rows, err := db.Query(query)
	if err != nil {
		return "Dados de wait stats indisponíveis.", nil
	}
	defer rows.Close()

	var b strings.Builder
	b.WriteString("Top 10 Wait Events (pg_stat_activity):\n")
	has := false
	for rows.Next() {
		has = true
		var eventType, event string
		var sessions int
		if err := rows.Scan(&eventType, &event, &sessions); err == nil {
			b.WriteString(fmt.Sprintf("%s / %s | sessions=%d\n", eventType, event, sessions))
		}
	}
	if !has {
		return "Sem eventos de espera ativos no momento.", nil
	}
	return b.String(), nil
}

// ExtractTopQueriesPostgres usa pg_stat_statements (com fallback para pg_stat_activity)
func ExtractTopQueriesPostgres(db *sql.DB) (string, error) {
	query := `
		SELECT mean_exec_time, total_exec_time, calls, query
		FROM pg_stat_statements
		WHERE dbid = (SELECT oid FROM pg_database WHERE datname = current_database())
		ORDER BY total_exec_time DESC
		LIMIT 10;
	`
	rows, err := db.Query(query)
	if err != nil {
		// Fallback: queries longas em pg_stat_activity
		return extractLongQueriesFromActivity(db)
	}
	defer rows.Close()

	var b strings.Builder
	b.WriteString("Top 10 Queries por Tempo Total (pg_stat_statements):\n")
	has := false
	for rows.Next() {
		has = true
		var mean, total float64
		var calls int64
		var q string
		if err := rows.Scan(&mean, &total, &calls, &q); err == nil {
			clean := strings.ReplaceAll(q, "\n", " ")
			if len(clean) > 400 {
				clean = clean[:400] + "..."
			}
			b.WriteString(fmt.Sprintf("total_ms=%.2f mean_ms=%.2f calls=%d | %s\n", total, mean, calls, clean))
		}
	}
	if !has {
		return "Sem dados em pg_stat_statements.", nil
	}
	return b.String(), nil
}

func extractLongQueriesFromActivity(db *sql.DB) (string, error) {
	query := `
		SELECT state,
		       EXTRACT(EPOCH FROM (now() - query_start)) AS duration_seconds,
		       LEFT(query, 400) AS query_text
		FROM pg_stat_activity
		WHERE state != 'idle'
		  AND query_start IS NOT NULL
		  AND datname = current_database()
		ORDER BY duration_seconds DESC NULLS LAST
		LIMIT 10;
	`
	rows, err := db.Query(query)
	if err != nil {
		return "Dados de query stats indisponíveis (pg_stat_statements não habilitado).", nil
	}
	defer rows.Close()

	var b strings.Builder
	b.WriteString("Top 10 Queries Ativas (pg_stat_activity - fallback):\n")
	has := false
	for rows.Next() {
		has = true
		var state, text string
		var duration float64
		if err := rows.Scan(&state, &duration, &text); err == nil {
			b.WriteString(fmt.Sprintf("state=%s duration=%.2fs | %s\n", state, duration, text))
		}
	}
	if !has {
		return "Sem queries ativas no momento.", nil
	}
	return b.String(), nil
}

// ExtractIndexStatsPostgres retorna estatísticas de uso de índices
func ExtractIndexStatsPostgres(db *sql.DB) (string, error) {
	query := `
		SELECT s.relname        AS table_name,
		       s.indexrelname   AS index_name,
		       s.idx_scan,
		       s.idx_tup_read,
		       pg_size_pretty(pg_relation_size(s.indexrelid)) AS index_size
		FROM pg_stat_user_indexes s
		ORDER BY s.idx_scan ASC, pg_relation_size(s.indexrelid) DESC
		LIMIT 10;
	`
	rows, err := db.Query(query)
	if err != nil {
		return "Dados de index stats indisponíveis.", nil
	}
	defer rows.Close()

	var b strings.Builder
	b.WriteString("Top 10 Índices (menor uso primeiro - candidatos a remoção):\n")
	has := false
	for rows.Next() {
		has = true
		var table, index, size string
		var idxScan, tupRead int64
		if err := rows.Scan(&table, &index, &idxScan, &tupRead, &size); err == nil {
			b.WriteString(fmt.Sprintf("%s.%s | scans=%d reads=%d size=%s\n", table, index, idxScan, tupRead, size))
		}
	}
	if !has {
		return "Sem dados de index stats.", nil
	}
	return b.String(), nil
}

// ExtractExecutionPlansPostgres tenta coletar planos via pg_stat_statements
func ExtractExecutionPlansPostgres(db *sql.DB) (string, error) {
	query := `
		SELECT LEFT(query, 300)
		FROM pg_stat_statements
		ORDER BY total_exec_time DESC
		LIMIT 3;
	`
	rows, err := db.Query(query)
	if err != nil {
		return "Planos de execução indisponíveis (pg_stat_statements não habilitado).", nil
	}
	defer rows.Close()

	var queries []string
	for rows.Next() {
		var q string
		if err := rows.Scan(&q); err == nil {
			trimmed := strings.TrimSpace(q)
			// Só tenta EXPLAIN em SELECTs por segurança
			if strings.HasPrefix(strings.ToUpper(trimmed), "SELECT") {
				queries = append(queries, trimmed)
			}
		}
	}

	if len(queries) == 0 {
		return "Nenhuma query SELECT disponível para análise de plano.", nil
	}

	var b strings.Builder
	b.WriteString("Planos de Execução das Top Queries (EXPLAIN):\n\n")
	for i, q := range queries {
		planRows, err := db.Query("EXPLAIN " + q)
		if err != nil {
			continue
		}
		b.WriteString(fmt.Sprintf("Plan #%d:\n", i+1))
		for planRows.Next() {
			var line string
			if err := planRows.Scan(&line); err == nil {
				b.WriteString(line + "\n")
			}
		}
		planRows.Close()
		b.WriteString("\n")
	}
	return b.String(), nil
}