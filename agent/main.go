package main

import (
	"fmt"
	"log"
	"os"
	"time"
)

func main() {
	fmt.Println("=======================================================")
	fmt.Println("🚀 DBA Agent Worker iniciado com sucesso (Golang).")
	fmt.Println("🛡️  Modo Operacional: M2M (Machine-to-Machine)")
	fmt.Println("=======================================================")

	apiURL := os.Getenv("API_URL")
	if apiURL == "" {
		apiURL = "http://localhost:8080/api/v1"
	}

	agentToken := os.Getenv("X_AGENT_TOKEN")
	if agentToken == "" {
		log.Println("⚠️ AVISO: Variável X_AGENT_TOKEN não encontrada.")
	}

	dbConnString := os.Getenv("DB_CONN_STRING")
	if dbConnString == "" {
		log.Println("⚠️ AVISO: DB_CONN_STRING não configurada. O agente enviará dados de MOCK.")
	}

	intervalo := 10 * time.Second
	ticker := time.NewTicker(intervalo)
	defer ticker.Stop()

	log.Printf("⏳ Agendador configurado. Ciclo a cada %v...\n", intervalo)

	executarCiclo(apiURL, agentToken, dbConnString)

	for range ticker.C {
		executarCiclo(apiURL, agentToken, dbConnString)
	}
}

func executarCiclo(apiURL string, agentToken string, dbConnString string) {
	log.Println("🔄 Iniciando ciclo de trabalho...")

	var ddl, dmv string
	
	if dbConnString != "" {
		db, err := ConnectDB(dbConnString)
		if err != nil {
			log.Printf("❌ Erro ao conectar no SQL Server: %v\n", err)
			return
		}
		defer db.Close()

		ddl, _ = ExtractSchema(db)
		dmv, _ = ExtractDMV(db)
		log.Println("📦 Dados reais extraídos do SQL Server via DDL e DMVs.")
	} else {
		ddl = "CREATE TABLE faturamento_vendas (id INT PRIMARY KEY, valor DECIMAL, cliente_id INT, data_venda TIMESTAMP, status char(2));"
		dmv = "Filtros por 'data_venda' e JOIN com a tabela de clientes estão custando 5 segundos na query."
	}

	telemetry := TelemetryRequest{
		DbEngine:  "SQL Server",
		SchemaDdl: ddl,
		DmvStats:  dmv,
	}

	SendTelemetry(apiURL, agentToken, telemetry)

	log.Println("🔍 Buscando tarefas pendentes (aprovadas)...")
	tasks := FetchPendingTasks(apiURL, agentToken)

	if len(tasks) > 0 {
		for _, task := range tasks {
			log.Printf("⚙️  Processando Tarefa #%d...\n", task.ID)
			
			if dbConnString != "" {
				db, _ := ConnectDB(dbConnString)
				err := ExecuteScript(db, task.UpScript)
				if err != nil {
					log.Printf("❌ Erro ao executar UpScript da Tarefa #%d: %v\n", task.ID, err)
					db.Close()
					continue
				}
				db.Close()
				log.Println("📜 Script de Deploy executado com sucesso no banco local!")
			} else {
				log.Println("⚠️  MOCK MODE: Simulação de execução do script (banco não conectado).")
			}

			MarkTaskCompleted(apiURL, agentToken, task.ID)
		}
	} else {
		log.Println("📭 Nenhuma tarefa aprovada para executar no momento.")
	}

	log.Println("✅ Ciclo finalizado. Voltando a dormir.")
	fmt.Println("-------------------------------------------------------")
}