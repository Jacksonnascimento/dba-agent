package com.dbaagent.api.services;

import com.dbaagent.api.dtos.AiAnalysisResultDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ClaudeIntegrationService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ClaudeIntegrationService(ObjectMapper objectMapper) {
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
    }

    public AiAnalysisResultDTO analyzeWithClaudeRich(
            String ddl,
            String dmvStats,
            String waitStats,
            String topQueries,
            String executionPlans,
            String indexStats,
            String apiKey,
            String aiModel,
            String dbEngine) {

        String url = "https://api.anthropic.com/v1/messages";

        String systemPrompt = String.format(
            "Você é um DBA Sênior Especialista em Performance e Confiabilidade de %s.\n\n" +
            "OBJETIVO: Diagnosticar gargalos e gerar scripts de otimização ESTRITAMENTE SEGUROS, " +
            "focados em performance.\n\n" +
            "DIRETRIZES DE SEGURANÇA E REGRAS OBRIGATÓRIAS (CRÍTICO):\n" +
            "1. ESCOPO PERMITIDO: Seus scripts de 'up_script' e 'down_script' devem se limitar a operações " +
            "de otimização de performance: criação/remoção de índices (CREATE/DROP INDEX), atualização de " +
            "estatísticas (UPDATE STATISTICS) e desfragmentação (ALTER INDEX REBUILD).\n" +
            "2. PROIBIÇÃO DE DDL DESTRUTIVO: NUNCA gere comandos CREATE TABLE, DROP TABLE, " +
            "ALTER TABLE (para colunas), DROP TRIGGER ou DROP FUNCTION. Assuma que a estrutura do cliente é intocável.\n" +
            "3. LIDANDO COM DDL TRUNCADO: Se as DMVs indicarem tabelas/colunas ausentes no DDL fornecido, " +
            "ASSUMA QUE ELAS JÁ EXISTEM NO BANCO DO CLIENTE.\n" +
            "4. CÓDIGO SEGURO E IDEMPOTENTE: Todo comando de criação ou remoção no 'up_script' e 'down_script' " +
            "deve ser obrigatoriamente envelopado em blocos condicionais (IF EXISTS / IF NOT EXISTS).\n" +
            "5. INTERVENÇÃO MANUAL: Se o gargalo for código interno, coloque um alerta estruturado " +
            "como comentário (/* ALERTA DBA: ... */) no 'up_script' orientando o refactoring manual.\n" +
            "6. GARANTIA DE SAÍDA: NUNCA retorne 'up_script' ou 'down_script' vazios. Se não houver o que automatizar, " +
            "retorne um script com PRINTs e comentários.\n" +
            "7. SINTAXE T-SQL RIGOROSA: Ao gerar scripts para SQL Server, não utilize ponto-e-vírgula ';' isolado como terminador " +
            "obrigatório se isso quebrar blocos IF/BEGIN/END. NUNCA utilize 'GO' nos scripts, pois o driver Go MSSQL não suporta. " +
            "Mantenha a sintaxe impecável para não causar 'Incorrect syntax near'.\n\n" +
            "Responda EXCLUSIVAMENTE em JSON puro com as chaves exatas: \"diagnostico\", \"up_script\", \"down_script\". " +
            "Não inclua blocos markdown (```json), devolva apenas o objeto JSON formatado perfeitamente.",
            dbEngine
        );

        String userContent = String.format(
            "## Estrutura (DDL)\n%s\n\n" +
            "## DMVs / Estatísticas dinâmicas\n%s\n\n" +
            "## Wait Stats\n%s\n\n" +
            "## Top Queries (consultas mais custosas)\n%s\n\n" +
            "## Index Stats / Fragmentação / Missing indexes\n%s\n\n" +
            "## Execution Plans (planos de execução)\n%s\n\n",
            ddl,
            (dmvStats != null ? dmvStats : "N/A"),
            (waitStats != null ? waitStats : "N/A"),
            (topQueries != null ? topQueries : "N/A"),
            (indexStats != null ? indexStats : "N/A"),
            (executionPlans != null ? executionPlans : "N/A")
        );

        try {
            ObjectNode requestBodyNode = objectMapper.createObjectNode();
            requestBodyNode.put("model", aiModel);
            requestBodyNode.put("max_tokens", 4096);
            requestBodyNode.put("system", systemPrompt);

            ArrayNode messagesArray = objectMapper.createArrayNode();
            ObjectNode messageNode = objectMapper.createObjectNode();
            messageNode.put("role", "user");
            messageNode.put("content", userContent);
            messagesArray.add(messageNode);
            
            requestBodyNode.set("messages", messagesArray);

            String requestBody = objectMapper.writeValueAsString(requestBodyNode);

            String response = restClient.post()
                    .uri(url)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode rootNode = objectMapper.readTree(response);
            String rawText = rootNode.path("content").get(0).path("text").asText();

            String cleanedJson = rawText.replaceAll("(?i)^```json\\s*", "")
                                        .replaceAll("(?i)^```\\s*", "")
                                        .replaceAll("\\s*```$", "")
                                        .trim();

            return objectMapper.readValue(cleanedJson, AiAnalysisResultDTO.class);

        } catch (Exception e) {
            AiAnalysisResultDTO errorDto = new AiAnalysisResultDTO();
            errorDto.setDiagnostico("Erro ao processar integração com Claude: " + e.getMessage());
            errorDto.setUpScript("-- Erro de integracao\n");
            errorDto.setDownScript("-- Erro de integracao\n");
            return errorDto;
        }
    }

    public void testConnection(String apiKey, String aiModel) {
        String url = "https://api.anthropic.com/v1/messages";

        try {
            ObjectNode requestBodyNode = objectMapper.createObjectNode();
            requestBodyNode.put("model", aiModel);
            requestBodyNode.put("max_tokens", 10);
            
            ArrayNode messagesArray = objectMapper.createArrayNode();
            ObjectNode messageNode = objectMapper.createObjectNode();
            messageNode.put("role", "user");
            messageNode.put("content", "Responda apenas 'Conectado'");
            messagesArray.add(messageNode);
            
            requestBodyNode.set("messages", messagesArray);

            String requestBody = objectMapper.writeValueAsString(requestBodyNode);

            restClient.post()
                    .uri(url)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new IllegalArgumentException("Erro ao conectar com Claude: " + e.getMessage(), e);
        }
    }
}
