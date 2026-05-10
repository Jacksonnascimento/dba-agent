package main

import (
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"strings"
	"sync"
	"time"

	"github.com/joho/godotenv"
)

type AgentConfigAPI struct {
	Name                    string `json:"name"`
	DbEngine                string `json:"dbEngine"`
	ConnectionUri           string `json:"connectionUri"`
	SnapshotIntervalMinutes int    `json:"snapshotIntervalMinutes"`
}

type AgentTarget struct {
	Name         string
	DbEngine     string
	DbConnString string
	AgentToken   string
}

func limitPayload(s string, max int) string {
	if len(s) <= max {
		return s
	}
	return s[:max] + "\n-- truncated by agent --"
}

// FetchConfigFromAPI faz o request seguro para o backend buscando os dados sensíveis.
func FetchConfigFromAPI(apiURL string, token string) (*AgentConfigAPI, error) {
	req, err := http.NewRequest("GET", fmt.Sprintf("%s/agent/config", apiURL), nil)
	if err != nil {
		return nil, err
	}
	req.Header.Set("X-Agent-Token", token)
	req.Header.Set("Content-Type", "application/json")

	client := &http.Client{Timeout: 10 * time.Second}
	resp, err := client.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		bodyBytes, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("Erro API (Status %d): %s", resp.StatusCode, string(bodyBytes))
	}

	var config AgentConfigAPI
	if err := json.NewDecoder(resp.Body).Decode(&config); err != nil {
		return nil, err
	}

	return &config, nil
}

func main() {
	// Carrega as variáveis do arquivo .env local
	err := godotenv.Load()
	if err != nil {
		log.Println("⚠️ Nenhum arquivo .env encontrado. Tentando ler variáveis nativas do SO.")
	}

	fmt.Println("=======================================================")
	fmt.Println("🚀 DBA Agent Worker iniciado com sucesso (Golang).")
	fmt.Println("🛡️  Modo Operacional: BYOK / Nuvem Gerenciada")
	fmt.Println("=======================================================")

	apiURL := os.Getenv("API_URL")
	if apiURL == "" {
		apiURL = "http://localhost:8080/api/v1"
	}

	agentToken := strings.TrimSpace(os.Getenv("X_AGENT_TOKEN"))
	if agentToken == "" {
		log.Fatalln("❌ ERRO FATAL: X_AGENT_TOKEN não encontrado no .env ou nas variáveis de ambiente.")
	}

	log.Println("🔐 Buscando credenciais e configuração de forma segura na API Central...")
	apiConfig, err := FetchConfigFromAPI(apiURL, agentToken)
	if err != nil {
		log.Fatalf("❌ ERRO CRÍTICO ao buscar configurações: %v\n", err)
	}

	// Montando o Alvo dinâmico a partir do banco
	targets := []AgentTarget{
		{
			Name:         apiConfig.Name,
			DbEngine:     apiConfig.DbEngine,
			DbConnString: apiConfig.ConnectionUri,
			AgentToken:   agentToken,
		},
	}

	intervaloSnapshot := time.Duration(apiConfig.SnapshotIntervalMinutes) * time.Minute
	log.Printf("⚙️  Alvo configurado: '%s' (%s)\n", apiConfig.Name, apiConfig.DbEngine)

	var wg sync.WaitGroup

	// Fluxo 1: Extração Massiva (Snapshots) com tempo dinâmico ditado pela API
	wg.Add(1)
	go func() {
		defer wg.Done()
		log.Printf("⏳ [TELEMETRIA] Agendador dinâmico configurado para rodar a cada %v.\n", intervaloSnapshot)

		executarExtraçãoTelemetria(apiURL, targets) // Executa a primeira vez

		ticker := time.NewTicker(intervaloSnapshot)
		defer ticker.Stop()
		for range ticker.C {
			executarExtraçãoTelemetria(apiURL, targets)
		}
	}()

	// Fluxo 2: Polling de Tarefas Aprovadas - Fixo a cada 5 segundos
	wg.Add(1)
	go func() {
		defer wg.Done()
		tasksInterval := 5 * time.Second
		log.Printf("⏳ [TAREFAS] Agendador de tarefas configurado para rodar a cada %v.\n", tasksInterval)

		ticker := time.NewTicker(tasksInterval)
		defer ticker.Stop()
		for range ticker.C {
			verificarEExecutarTarefas(apiURL, targets)
		}
	}()

	wg.Wait()
}

func executarExtraçãoTelemetria(apiURL string, targets []AgentTarget) {
	log.Println("🔄 [TELEMETRIA] Iniciando extração massiva...")

	for _, target := range targets {
		if target.DbConnString == "" {
			log.Printf("⚠️ [TELEMETRIA] Alvo '%s' ignorado: API não retornou Connection String.\n", target.Name)
			continue
		}

		var ddl, dmv string
		var waits, topq, idxstats, plans string

		if strings.EqualFold(target.DbEngine, "SQL Server") {
			db, err := ConnectDB(target.DbConnString)
			if err != nil {
				log.Printf("❌ [TELEMETRIA] Erro crítico ao conectar no alvo '%s': %v\n", target.Name, err)
				continue
			}

			ddl, _ = ExtractSchema(db)
			dmv, _ = ExtractDMV(db)
			waits, _ = ExtractWaitStats(db)
			topq, _ = ExtractTopQueries(db)
			idxstats, _ = ExtractIndexStats(db)
			plans, _ = ExtractExecutionPlans(db)

			db.Close()
			log.Printf("📦 [TELEMETRIA] Dados extraídos do banco '%s' com sucesso.\n", target.Name)
		} else {
			log.Printf("⚠️ [TELEMETRIA] Engine %s não suportada no momento.\n", target.DbEngine)
			continue
		}

		telemetry := TelemetryRequest{
			DbEngine:       target.DbEngine,
			SchemaDdl:      limitPayload(ddl, 150000),
			DmvStats:       limitPayload(dmv, 12000),
			WaitStats:      limitPayload(waits, 12000),
			TopQueries:     limitPayload(topq, 20000),
			IndexStats:     limitPayload(idxstats, 12000),
			ExecutionPlans: limitPayload(plans, 60000),
		}
		SendTelemetry(apiURL, target.AgentToken, telemetry)
	}
	log.Println("✅ [TELEMETRIA] Ciclo de extração finalizado.")
}

func verificarEExecutarTarefas(apiURL string, targets []AgentTarget) {
	for _, target := range targets {
		tasks := FetchPendingTasks(apiURL, target.AgentToken)
		for _, task := range tasks {
			log.Printf("⚙️ [TAREFAS] Baixando Tarefa #%d aprovada pelo Dashboard...\n", task.ID)

			if target.DbConnString == "" {
				continue
			}

			if strings.EqualFold(target.DbEngine, "SQL Server") {
				db, err := ConnectDB(target.DbConnString)
				if err != nil {
					log.Printf("❌ [TAREFAS] Falha de conexão na Tarefa %d: %v\n", task.ID, err)
					continue
				}

				log.Printf("⚡ [TAREFAS] Aplicando UpScript no banco de dados...")
				err = ExecuteScript(db, task.UpScript)
				db.Close()

				if err != nil {
					log.Printf("❌ [TAREFAS] Erro ao executar UpScript (Tarefa #%d): %v\n", task.ID, err)
					continue
				}
				log.Println("✅ [TAREFAS] Script de Deploy executado com sucesso!")
			}
			MarkTaskCompleted(apiURL, target.AgentToken, task.ID)
		}
	}
}