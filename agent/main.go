package main

import (
	"encoding/json"
	"fmt"
	"log"
	"os"
	"strings"
	"time"
)

type AgentTarget struct {
	Name         string `json:"name"`
	DbEngine     string `json:"dbEngine"`
	DbConnString string `json:"dbConnString"`
	AgentToken   string `json:"agentToken"`
}

func limitPayload(s string, max int) string {
	if len(s) <= max {
		return s
	}
	return s[:max] + "\n-- truncated by agent --"
}

func main() {
	fmt.Println("=======================================================")
	fmt.Println("🚀 DBA Agent Worker iniciado com sucesso (Golang).")
	fmt.Println("🛡️  Modo Operacional: M2M (Machine-to-Machine)")
	fmt.Println("=======================================================")

	apiURL := os.Getenv("API_URL")
	if apiURL == "" {
		apiURL = "http://localhost:8080/api/v1"
	}

	targets := loadTargetsFromEnv()
	if len(targets) == 0 {
		log.Println("❌ Nenhum alvo de banco configurado. Defina DBA_TARGETS_JSON ou X_AGENT_TOKEN.")
		return
	}

	intervalo := 10 * time.Second
	ticker := time.NewTicker(intervalo)
	defer ticker.Stop()

	log.Printf("⏳ Agendador configurado. Ciclo a cada %v com %d alvo(s).\n", intervalo, len(targets))

	executarCiclo(apiURL, targets)

	for range ticker.C {
		executarCiclo(apiURL, targets)
	}
}

func loadTargetsFromEnv() []AgentTarget {
	jsonConfig := strings.TrimSpace(os.Getenv("DBA_TARGETS_JSON"))
	if jsonConfig != "" {
		var targets []AgentTarget
		if err := json.Unmarshal([]byte(jsonConfig), &targets); err != nil {
			log.Printf("❌ DBA_TARGETS_JSON inválido: %v\n", err)
			return nil
		}
		valid := make([]AgentTarget, 0, len(targets))
		for _, t := range targets {
			if strings.TrimSpace(t.AgentToken) == "" {
				log.Printf("⚠️ Alvo '%s' ignorado: agentToken ausente.\n", t.Name)
				continue
			}
			if strings.TrimSpace(t.DbEngine) == "" {
				t.DbEngine = "SQL Server"
			}
			valid = append(valid, t)
		}
		return valid
	}

	legacyToken := strings.TrimSpace(os.Getenv("X_AGENT_TOKEN"))
	if legacyToken == "" {
		return nil
	}
	legacyConn := strings.TrimSpace(os.Getenv("DB_CONN_STRING"))
	legacyEngine := strings.TrimSpace(os.Getenv("DB_ENGINE"))
	if legacyEngine == "" {
		legacyEngine = "SQL Server"
	}
	return []AgentTarget{
		{
			Name:         "default",
			DbEngine:     legacyEngine,
			DbConnString: legacyConn,
			AgentToken:   legacyToken,
		},
	}
}

func executarCiclo(apiURL string, targets []AgentTarget) {
	log.Println("🔄 Iniciando ciclo de trabalho...")

	for _, target := range targets {
		log.Printf("🧭 Alvo: %s (%s)\n", target.Name, target.DbEngine)
		var ddl, dmv string
		var waits, topq, idxstats, plans string
		if target.DbConnString != "" && strings.EqualFold(target.DbEngine, "SQL Server") {
			db, err := ConnectDB(target.DbConnString)
			if err != nil {
				log.Printf("❌ Erro ao conectar no alvo '%s': %v\n", target.Name, err)
				continue
			}
			ddl, _ = ExtractSchema(db)
			dmv, _ = ExtractDMV(db)
			waits, _ = ExtractWaitStats(db)
			topq, _ = ExtractTopQueries(db)
			idxstats, _ = ExtractIndexStats(db)
			plans, _ = ExtractExecutionPlans(db)
			db.Close()
			log.Printf("📦 Telemetria extraída do alvo '%s'.\n", target.Name)
		} else {
			ddl = "CREATE TABLE faturamento_vendas (id INT PRIMARY KEY, valor DECIMAL, cliente_id INT, data_venda TIMESTAMP, status char(2));"
			dmv = "Modo fallback/mock para alvo sem conexão SQL Server configurada."
			waits = ""
			topq = ""
			idxstats = ""
			plans = ""
			if !strings.EqualFold(target.DbEngine, "SQL Server") {
				log.Printf("⚠️ Engine %s ainda sem extractor nativo. Enviando mock para '%s'.\n", target.DbEngine, target.Name)
			}
		}

		telemetry := TelemetryRequest{
			DbEngine:       target.DbEngine,
			SchemaDdl:      limitPayload(ddl, 12000),
			DmvStats:       limitPayload(dmv, 12000),
			WaitStats:      limitPayload(waits, 12000),
			TopQueries:     limitPayload(topq, 20000),
			IndexStats:     limitPayload(idxstats, 12000),
			ExecutionPlans: limitPayload(plans, 60000),
		}
		SendTelemetry(apiURL, target.AgentToken, telemetry)

		tasks := FetchPendingTasks(apiURL, target.AgentToken)
		for _, task := range tasks {
			log.Printf("⚙️ Processando Tarefa #%d para '%s'...\n", task.ID, target.Name)
			if task.DatabaseName != "" && !strings.EqualFold(task.DatabaseName, target.Name) {
				log.Printf("ℹ️ Tarefa vinculada ao banco '%s' (alvo local: '%s').\n", task.DatabaseName, target.Name)
			}
			if target.DbConnString != "" && strings.EqualFold(target.DbEngine, "SQL Server") {
				db, err := ConnectDB(target.DbConnString)
				if err != nil {
					log.Printf("❌ Falha ao conectar para executar tarefa %d: %v\n", task.ID, err)
					continue
				}
				err = ExecuteScript(db, task.UpScript)
				db.Close()
				if err != nil {
					log.Printf("❌ Erro ao executar UpScript da Tarefa #%d: %v\n", task.ID, err)
					continue
				}
				log.Println("📜 Script de Deploy executado com sucesso no banco local!")
			} else {
				log.Println("⚠️ MOCK MODE: Simulação de execução do script.")
			}
			MarkTaskCompleted(apiURL, target.AgentToken, task.ID)
		}
	}

	log.Println("✅ Ciclo finalizado. Voltando a dormir.")
	fmt.Println("-------------------------------------------------------")
}