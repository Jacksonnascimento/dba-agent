package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"strings"
	"sync"
	"time"

	"github.com/joho/godotenv"
	"github.com/kardianos/service"
)

type AgentConfigAPI struct {
	Name                    string `json:"name"`
	DbEngine                string `json:"dbEngine"`
	ConnectionUri           string `json:"connectionUri"`
	SnapshotIntervalMinutes int    `json:"snapshotIntervalMinutes"`
	AgentToken              string `json:"agentToken"`
}

type AgentTarget struct {
	Name         string
	DbEngine     string
	DbConnString string
	AgentToken   string
}

// program representa a estrutura do nosso serviço
type program struct {
	quit chan struct{}
}

type SafeWorkerConfig struct {
	mu       sync.RWMutex
	Targets  []AgentTarget
	Interval time.Duration
}

var globalConfig SafeWorkerConfig

func limitPayload(s string, max int) string {
	if len(s) <= max {
		return s
	}
	return s[:max] + "\n-- truncated by agent --"
}

// FetchConfigsFromAPI faz o request seguro para o backend buscando todas as configs dos bancos deste worker.
func FetchConfigsFromAPI(apiURL string, workerToken string) ([]AgentConfigAPI, error) {
	req, err := http.NewRequest("GET", fmt.Sprintf("%s/agent-workers/me/config", apiURL), nil)
	if err != nil {
		return nil, err
	}
	req.Header.Set("X-Worker-Token", workerToken)
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

	var configs []AgentConfigAPI
	if err := json.NewDecoder(resp.Body).Decode(&configs); err != nil {
		return nil, err
	}

	return configs, nil
}

// Start é chamado quando o serviço é iniciado pelo SO
func (p *program) Start(s service.Service) error {
	p.quit = make(chan struct{})
	go p.run()
	return nil
}

// Stop é chamado quando o serviço é parado pelo SO
func (p *program) Stop(s service.Service) error {
	log.Println("Sinal de parada recebido. Encerrando DBA Agent Worker...")
	close(p.quit)
	return nil
}

// run contém a lógica principal que antes ficava na func main()
func (p *program) run() {
	// Carrega as variáveis do arquivo .env local
	err := godotenv.Load()
	if err != nil {
		log.Println("⚠️ Nenhum arquivo .env encontrado. Tentando ler variáveis nativas do SO.")
	}

	fmt.Println("=======================================================")
	fmt.Println("🚀 DBA Agent Worker iniciado com sucesso (Golang).")
	fmt.Println("🛡️  Modo Operacional: BYOK / Nuvem Gerenciada (Multi-Token)")
	fmt.Println("=======================================================")

	// Lê argumentos via SO (se estiver rodando como serviço) ou fallback para Env var
	var apiURL, workerToken string
	for i, arg := range os.Args {
		if arg == "-api" && i+1 < len(os.Args) {
			apiURL = os.Args[i+1]
		}
		if arg == "-token" && i+1 < len(os.Args) {
			workerToken = os.Args[i+1]
		}
	}

	if apiURL == "" {
		apiURL = os.Getenv("API_URL")
		if apiURL == "" {
			apiURL = "http://localhost:8080/api/v1"
		}
	}

	if workerToken == "" {
		workerToken = os.Getenv("X_WORKER_TOKEN")
	}

	if strings.TrimSpace(workerToken) == "" {
		log.Fatalln("❌ ERRO FATAL: -token ou X_WORKER_TOKEN não encontrado.")
	}

	iniciarCicloWorker(apiURL, workerToken)

	// Bloqueia a execução aguardando o sinal de parada do serviço
	<-p.quit
}

func main() {
	action := flag.String("service", "", "Ações do serviço: install, uninstall, start, stop, restart")
	apiArg := flag.String("api", "", "URL da API")
	tokenArg := flag.String("token", "", "Token do Worker")
	nameArg := flag.String("name", "", "Nome do agente (como cadastrado no painel)")
	flag.Parse()

	internalName, displayName := serviceNamesFromAgent(*nameArg)

	svcArgs := []string{}
	if *apiArg != "" && *tokenArg != "" {
		svcArgs = []string{"-api", *apiArg, "-token", *tokenArg}
	}

	svcConfig := &service.Config{
		Name:        internalName,
		DisplayName: displayName,
		Description: "DBA Agent — coleta e envio de métricas para o SaaS DBA Agent.",
		Arguments:   svcArgs,
	}

	prg := &program{}
	s, err := service.New(prg, svcConfig)
	if err != nil {
		log.Fatal(err)
	}

	if *action != "" {
		err = service.Control(s, *action)
		if err != nil {
			log.Fatalf("Falha ao executar a ação '%s': %v", *action, err)
		}
		fmt.Printf("Ação '%s' executada com sucesso no serviço.\n", *action)
		return
	}

	// Inicia o programa (como serviço ou no terminal se executado diretamente)
	err = s.Run()
	if err != nil {
		log.Fatal(err)
	}
}

// Inicia o ciclo de vida completo do Worker
func iniciarCicloWorker(apiURL string, workerToken string) {
	// Primeira carga obrigatória para iniciar o ciclo
	for {
		log.Printf("🔐 [Worker] Buscando credenciais e configuração inicial...")
		apiConfigs, err := FetchConfigsFromAPI(apiURL, workerToken)

		if err == nil {
			updateGlobalConfig(apiConfigs)
			globalConfig.mu.RLock()
			interval := globalConfig.Interval
			globalConfig.mu.RUnlock()
			log.Printf("✅ [Worker] Configurações carregadas com sucesso! Intervalo Mestre: %v", interval)
			break
		}

		log.Printf("⚠️ [Worker] API indisponível ou erro na busca. Tentando novamente em 15s... (Erro: %v)\n", err)
		time.Sleep(15 * time.Second)
	}

	var wg sync.WaitGroup

	// Fluxo 1: Hot Reload (Polling de Configuração a cada minuto)
	wg.Add(1)
	go func() {
		defer wg.Done()
		ticker := time.NewTicker(60 * time.Second)
		defer ticker.Stop()
		for range ticker.C {
			apiConfigs, err := FetchConfigsFromAPI(apiURL, workerToken)
			if err == nil {
				updateGlobalConfig(apiConfigs)
			} else {
				log.Printf("⚠️ [Hot Reload] Falha ao atualizar configurações do agente: %v", err)
			}
		}
	}()

	// Fluxo 2: Extração Massiva (Baseada no Intervalo Mestre dinâmico)
	wg.Add(1)
	go func() {
		defer wg.Done()
		var lastRun time.Time

		// Executa a primeira vez imediatamente
		globalConfig.mu.RLock()
		targets := globalConfig.Targets
		globalConfig.mu.RUnlock()
		executarExtraçãoTelemetria(apiURL, targets)
		lastRun = time.Now()

		// Acorda frequentemente (30s) para verificar se é hora de rodar, permitindo mudanças dinâmicas
		ticker := time.NewTicker(30 * time.Second)
		defer ticker.Stop()

		for range ticker.C {
			globalConfig.mu.RLock()
			interval := globalConfig.Interval
			targets := globalConfig.Targets
			globalConfig.mu.RUnlock()

			if len(targets) > 0 && time.Since(lastRun) >= interval {
				executarExtraçãoTelemetria(apiURL, targets)
				lastRun = time.Now()
			}
		}
	}()

	// Fluxo 3: Polling de Tarefas Aprovadas (Para Coleta Manual e Scripts)
	wg.Add(1)
	go func() {
		defer wg.Done()
		tasksInterval := 5 * time.Second
		log.Printf("⏳ [TAREFAS] Agendador de tarefas configurado para rodar a cada %v.\n", tasksInterval)

		ticker := time.NewTicker(tasksInterval)
		defer ticker.Stop()
		for range ticker.C {
			globalConfig.mu.RLock()
			targets := globalConfig.Targets
			globalConfig.mu.RUnlock()
			verificarEExecutarTarefas(apiURL, targets)
		}
	}()

	wg.Wait()
}

func updateGlobalConfig(apiConfigs []AgentConfigAPI) {
	var newTargets []AgentTarget
	for _, conf := range apiConfigs {
		newTargets = append(newTargets, AgentTarget{
			Name:         conf.Name,
			DbEngine:     conf.DbEngine,
			DbConnString: conf.ConnectionUri,
			AgentToken:   conf.AgentToken,
		})
	}

	interval := 24 * time.Hour // Default 24h fallback
	if len(apiConfigs) > 0 {
		interval = time.Duration(apiConfigs[0].SnapshotIntervalMinutes) * time.Minute
	}

	globalConfig.mu.Lock()
	defer globalConfig.mu.Unlock()
	
	// Apenas para logar caso ocorra uma mudança
	if len(globalConfig.Targets) != len(newTargets) || globalConfig.Interval != interval {
		if len(globalConfig.Targets) > 0 { // Ignorar no primeiro boot
			log.Printf("🔄 [Hot Reload] Configurações alteradas na API! Atualizando memória local (Bancos: %d, Intervalo: %v).", len(newTargets), interval)
		}
	}
	
	globalConfig.Targets = newTargets
	globalConfig.Interval = interval
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
			db, err := ConnectDB(target.DbEngine, target.DbConnString)
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
		} else if strings.EqualFold(target.DbEngine, "PostgreSQL") {
			db, err := ConnectDB(target.DbEngine, target.DbConnString)
			if err != nil {
				log.Printf("❌ [TELEMETRIA - %s] Erro crítico ao conectar no alvo PostgreSQL: %v\n", target.Name, err)
				continue
			}

			ddl, _ = ExtractSchemaPostgres(db)
			dmv, _ = ExtractMissingIndexesPostgres(db)
			waits, _ = ExtractWaitStatsPostgres(db)
			topq, _ = ExtractTopQueriesPostgres(db)
			idxstats, _ = ExtractIndexStatsPostgres(db)
			plans, _ = ExtractExecutionPlansPostgres(db)

			db.Close()
			log.Printf("📦 [TELEMETRIA - %s] Dados PostgreSQL extraídos com sucesso.\n", target.Name)
		} else {
			log.Printf("⚠️ [TELEMETRIA - %s] Engine '%s' não suportada no momento.\n", target.Name, target.DbEngine)
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
			if task.TaskType == "FORCE_TELEMETRY" {
				log.Printf("⚡ [TAREFAS - %s] Comando de Extração Manual recebido (Tarefa #%d)!", target.Name, task.ID)
				executarExtraçãoTelemetria(apiURL, []AgentTarget{target})
				MarkTaskCompleted(apiURL, target.AgentToken, task.ID)
				continue
			}

			tipoStr := "Deploy (UpScript)"
			scriptParaRodar := task.UpScript
			if task.TaskType == "ROLLBACK" {
				tipoStr = "Rollback (DownScript)"
				scriptParaRodar = task.DownScript
			}

			log.Printf("⚙️ [TAREFAS - %s] Baixando Tarefa #%d aprovada pelo Dashboard (%s)...\n", target.Name, task.ID, tipoStr)

			if target.DbConnString == "" {
				continue
			}

			// Roteia a execução do script baseado na engine
			if strings.EqualFold(target.DbEngine, "SQL Server") || strings.EqualFold(target.DbEngine, "PostgreSQL") {
				db, err := ConnectDB(target.DbEngine, target.DbConnString)
				if err != nil {
					log.Printf("❌ [TAREFAS - %s] Falha de conexão na Tarefa %d: %v\n", target.Name, task.ID, err)
					continue
				}

				log.Printf("⚡ [TAREFAS - %s] Aplicando %s no banco de dados (%s)...", target.Name, tipoStr, target.DbEngine)
				err = ExecuteScript(db, scriptParaRodar)
				db.Close()

				if err != nil {
					log.Printf("❌ [TAREFAS - %s] Erro ao executar %s (Tarefa #%d): %v\n", target.Name, tipoStr, task.ID, err)
					MarkTaskFailed(apiURL, target.AgentToken, task.ID, err.Error())
					continue
				}
				log.Printf("✅ [TAREFAS - %s] %s executado com sucesso!\n", target.Name, tipoStr)
			} else {
				log.Printf("⚠️ [TAREFAS - %s] Engine '%s' não suportada para execução de scripts.\n", target.Name, target.DbEngine)
			}
			MarkTaskCompleted(apiURL, target.AgentToken, task.ID)
		}
	}
}