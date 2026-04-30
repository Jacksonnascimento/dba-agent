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

// ExtractSchema lê o INFORMATION_SCHEMA e monta um rascunho de DDL
func ExtractSchema(db *sql.DB) (string, error) {
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

// ExecuteScript roda o UpScript aprovado no banco do cliente
func ExecuteScript(db *sql.DB, script string) error {
	_, err := db.Exec(script)
	return err
}