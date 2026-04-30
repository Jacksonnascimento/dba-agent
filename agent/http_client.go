package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"time"
)

// DTO de Envio de Telemetria
type TelemetryRequest struct {
	DbEngine  string `json:"dbEngine"`
	SchemaDdl string `json:"schemaDdl"`
	DmvStats  string `json:"dmvStats"`
}

// DTO de Recebimento de Tarefa (Aprovada)
type TaskResponse struct {
	ID         int    `json:"id"`
	SchemaHash string `json:"schemaHash"`
	Diagnosis  string `json:"diagnosis"`
	UpScript   string `json:"upScript"`
	DownScript string `json:"downScript"`
	Status     string `json:"status"`
}

// Cliente HTTP configurado com timeout
var client = &http.Client{
	Timeout: 10 * time.Second,
}

// Envia os dados do banco para a API Central analisar
func SendTelemetry(apiURL string, token string, reqBody TelemetryRequest) {
	url := fmt.Sprintf("%s/agent/telemetry", apiURL)

	jsonData, err := json.Marshal(reqBody)
	if err != nil {
		log.Printf("❌ Erro ao serializar telemetria: %v\n", err)
		return
	}

	req, err := http.NewRequest("POST", url, bytes.NewBuffer(jsonData))
	if err != nil {
		log.Printf("❌ Erro ao criar requisição HTTP: %v\n", err)
		return
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Agent-Token", token)

	resp, err := client.Do(req)
	if err != nil {
		log.Printf("❌ Erro de conexão com a API Central: %v\n", err)
		return
	}
	defer resp.Body.Close()

	if resp.StatusCode == 201 {
		log.Println("📡 Telemetria enviada com sucesso! Aguardando análise/aprovação.")
	} else {
		bodyBytes, _ := io.ReadAll(resp.Body)
		log.Printf("⚠️ Falha ao enviar telemetria. Status: %d | Resposta: %s\n", resp.StatusCode, string(bodyBytes))
	}
}

// Busca tarefas que o DBA humano aprovou no painel
func FetchPendingTasks(apiURL string, token string) []TaskResponse {
	url := fmt.Sprintf("%s/agent/tasks", apiURL)

	req, err := http.NewRequest("GET", url, nil)
	if err != nil {
		log.Printf("❌ Erro ao criar requisição de tarefas: %v\n", err)
		return nil
	}

	req.Header.Set("X-Agent-Token", token)

	resp, err := client.Do(req)
	if err != nil {
		log.Printf("❌ Erro ao buscar tarefas na API: %v\n", err)
		return nil
	}
	defer resp.Body.Close()

	if resp.StatusCode == 200 {
		var tasks []TaskResponse
		err := json.NewDecoder(resp.Body).Decode(&tasks)
		if err != nil {
			log.Printf("❌ Erro ao ler JSON das tarefas: %v\n", err)
			return nil
		}
		return tasks
	}

	if resp.StatusCode != 204 { // 204 No Content é esperado quando não tem tarefa
		log.Printf("⚠️ Retorno inesperado ao buscar tarefas. Status: %d\n", resp.StatusCode)
	}
	return nil
}

// Avisa a API que o script foi executado com sucesso no banco do cliente
func MarkTaskCompleted(apiURL string, token string, taskID int) {
	url := fmt.Sprintf("%s/agent/tasks/%d/complete", apiURL, taskID)

	req, err := http.NewRequest("POST", url, nil)
	if err != nil {
		log.Printf("❌ Erro ao criar requisição de conclusão: %v\n", err)
		return
	}

	req.Header.Set("X-Agent-Token", token)

	resp, err := client.Do(req)
	if err != nil {
		log.Printf("❌ Erro ao avisar API sobre conclusão: %v\n", err)
		return
	}
	defer resp.Body.Close()

	if resp.StatusCode == 200 {
		log.Printf("✅ Tarefa #%d marcada como EXECUTADA na API Central.\n", taskID)
	} else {
		log.Printf("⚠️ Falha ao concluir tarefa #%d. Status: %d\n", taskID, resp.StatusCode)
	}
}