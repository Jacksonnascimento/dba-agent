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
	fmt.Println("🛡️  Modo Operacional: BYOK / Nuvem Gerenciada (Multi-Token)")
	fmt.Println("=======================================================")

	apiURL := os.Getenv("API_URL")
	if apiURL == "" {
		apiURL = "http://localhost:8080/api/v1"
	}

	// Agora aceita uma lista separada por vírgulas de tokens no .env
	agentTokensRaw := os.Getenv("X_AGENT_TOKENS")
	
	// Fallback para manter compatibilidade com a versão anterior caso o usuário não tenha atualizado o .env
	if agentTokensRaw == "" {
		agentTokensRaw = os.Getenv("X_AGENT_TOKEN")
	}

	if strings.TrimSpace(agentTokensRaw) == "" {
		log.Fatalln("❌ ERRO FATAL: X_AGENT_TOKENS ou X_AGENT_TOKEN não encontrado no .env ou nas variáveis de ambiente.")
	}

	tokens := strings.Split(agentTokensRaw, ",")
	var wg sync.WaitGroup

	log.Printf("🔍 Encontrados %d token(s) para processamento.\n", len(tokens))

	for _, token := range tokens {
		t := strings.TrimSpace(token)
		if t == "" {
			continue
		}

		wg.Add(1)
		go func(agentToken string) {
			defer wg.Done()
			iniciarCicloAgenteParaToken(apiURL, agentToken)
		}(t)
	}

	wg.Wait()
}

// Inicia o ciclo de vida completo do agente para um token específico
func iniciarCicloAgenteParaToken(apiURL string, agentToken string) {
	var apiConfig *AgentConfigAPI
	var err error

	// LOOP DE RESILIÊNCIA: Tenta conectar na API até obter sucesso
	for {
		log.Printf("🔐 [Token: %s...] Buscando credenciais e configuração na API Central...", agentToken[:8])
		apiConfig, err = FetchConfigFromAPI(apiURL, agentToken)

		if err == nil {
			log.Printf("✅ [Token: %s...] Configuração carregada com sucesso!", agentToken[:8])
			break
		}

		log.Printf("⚠️ [Token: %s...] API indisponível ou erro na busca. Tentando novamente em 15s... (Erro: %v)\n", agentToken[:8], err)
		time.Sleep(15 * time.Second)
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
	log.Printf("⚙️  Alvo configurado: '%s' (%s) - Intervalo: %v\n", apiConfig.Name, apiConfig.DbEngine, intervaloSnapshot)

	var tokenWg sync.WaitGroup

	// Fluxo 1: Extração Massiva (Snapshots) com tempo dinâmico ditado pela API
	tokenWg.Add(1)
	go func() {
		defer tokenWg.Done()
		log.Printf("⏳ [TELEMETRIA - %s] Agendador dinâmico configurado para rodar a cada %v.\n", apiConfig.Name, intervaloSnapshot)

		executarExtraçãoTelemetria(apiURL, targets) // Executa a primeira vez

		ticker := time.NewTicker(intervaloSnapshot)
		defer ticker.Stop()
		for range ticker.C {
			executarExtraçãoTelemetria(apiURL, targets)
		}
	}()

	// Fluxo 2: Polling de Tarefas Aprovadas - Fixo a cada 5 segundos
	tokenWg.Add(1)
	go func() {
		defer tokenWg.Done()
		tasksInterval := 5 * time.Second
		log.Printf("⏳ [TAREFAS - %s] Agendador de tarefas configurado para rodar a cada %v.\n", apiConfig.Name, tasksInterval)

		ticker := time.NewTicker(tasksInterval)
		defer ticker.Stop()
		for range ticker.C {
			verificarEExecutarTarefas(apiURL, targets)
		}
	}()

	tokenWg.Wait()
}

func executarExtraçãoTelemetria(apiURL string, targets []AgentTarget) {
	for _, target := range targets {
		log.Printf("🔄 [TELEMETRIA - %s] Iniciando extração massiva...", target.Name)
		
		if target.DbConnString == "" {
			log.Printf("⚠️ [TELEMETRIA - %s] Alvo ignorado: API não retornou Connection String.\n", target.Name)
			continue
		}

		var ddl, dmv string
		var waits, topq, idxstats, plans string

		if strings.EqualFold(target.DbEngine, "SQL Server") {
			db, err := ConnectDB(target.DbConnString)
			if err != nil {
				log.Printf("❌ [TELEMETRIA - %s] Erro crítico ao conectar no alvo: %v\n", target.Name, err)
				continue
			}

			ddl, _ = ExtractSchema(db)
			dmv, _ = ExtractDMV(db)
			waits, _ = ExtractWaitStats(db)
			topq, _ = ExtractTopQueries(db)
			idxstats, _ = ExtractIndexStats(db)
			plans, _ = ExtractExecutionPlans(db)

			db.Close()
			log.Printf("📦 [TELEMETRIA - %s] Dados extraídos com sucesso.\n", target.Name)
		} else {
			log.Printf("⚠️ [TELEMETRIA - %s] Engine %s não suportada no momento.\n", target.Name, target.DbEngine)
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
		log.Printf("✅ [TELEMETRIA - %s] Ciclo de extração finalizado.", target.Name)
	}
}

func verificarEExecutarTarefas(apiURL string, targets []AgentTarget) {
	for _, target := range targets {
		tasks := FetchPendingTasks(apiURL, target.AgentToken)
		for _, task := range tasks {
			log.Printf("⚙️ [TAREFAS - %s] Baixando Tarefa #%d aprovada pelo Dashboard...\n", target.Name, task.ID)

			if target.DbConnString == "" {
				continue
			}

			if strings.EqualFold(target.DbEngine, "SQL Server") {
				db, err := ConnectDB(target.DbConnString)
				if err != nil {
					log.Printf("❌ [TAREFAS - %s] Falha de conexão na Tarefa %d: %v\n", target.Name, task.ID, err)
					continue
				}

				log.Printf("⚡ [TAREFAS - %s] Aplicando UpScript no banco de dados...", target.Name)
				err = ExecuteScript(db, task.UpScript)
				db.Close()

				if err != nil {
					log.Printf("❌ [TAREFAS - %s] Erro ao executar UpScript (Tarefa #%d): %v\n", target.Name, task.ID, err)
					continue
				}
				log.Printf("✅ [TAREFAS - %s] Script de Deploy executado com sucesso!\n", target.Name)
			}
			MarkTaskCompleted(apiURL, target.AgentToken, task.ID)
		}
	}
}