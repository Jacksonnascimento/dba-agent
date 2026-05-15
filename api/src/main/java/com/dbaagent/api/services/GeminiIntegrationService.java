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
public class GeminiIntegrationService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiIntegrationService(ObjectMapper objectMapper) {
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
    }

    public AiAnalysisResultDTO analyzeWithGeminiRich(
            String ddl,
            String dmvStats,
            String waitStats,
            String topQueries,
            String executionPlans,
            String indexStats,
            String apiKey,
            String aiModel,
            String dbEngine) {

        String cleanModel = aiModel.startsWith("models/") ? aiModel.replace("models/", "") : aiModel;
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + cleanModel + ":generateContent?key=" + apiKey;

        // PROMPT BLINDADO E RESTRITO A TUNING SEGURO
        String prompt = String.format(
            "Você é um DBA Sênior Especialista em Performance e Confiabilidade de %s.\n\n" +
            "OBJETIVO: Diagnosticar gargalos e gerar scripts de otimização ESTRITAMENTE SEGUROS, " +
            "focados em performance, baseados no contexto abaixo.\n\n" +
            "## Estrutura (DDL)\n%s\n\n" +
            "## DMVs / Estatísticas dinâmicas\n%s\n\n" +
            "## Wait Stats\n%s\n\n" +
            "## Top Queries (consultas mais custosas)\n%s\n\n" +
            "## Index Stats / Fragmentação / Missing indexes\n%s\n\n" +
            "## Execution Plans (planos de execução)\n%s\n\n" +
            "DIRETRIZES DE SEGURANÇA E REGRAS OBRIGATÓRIAS (CRÍTICO):\n" +
            "1. ESCOPO PERMITIDO: Seus scripts de 'up_script' e 'down_script' devem se limitar a operações " +
            "de otimização de performance: criação/remoção de índices (CREATE/DROP INDEX), atualização de " +
            "estatísticas (UPDATE STATISTICS) e desfragmentação (ALTER INDEX REBUILD).\n" +
            "2. PROIBIÇÃO DE DDL DESTRUTIVO: NUNCA, SOB NENHUMA HIPÓTESE, gere comandos CREATE TABLE, DROP TABLE, " +
            "ALTER TABLE (para colunas), DROP TRIGGER ou DROP FUNCTION. Assuma que a estrutura do cliente é intocável.\n" +
            "3. LIDANDO COM DDL TRUNCADO: Se as DMVs indicarem tabelas ou colunas ausentes no DDL fornecido, " +
            "ASSUMA QUE ELAS JÁ EXISTEM NO BANCO DO CLIENTE. Infira os nomes de colunas lógicas APENAS para criar " +
            "os índices recomendados, não tente recriar a tabela.\n" +
            "4. CÓDIGO SEGURO E IDEMPOTENTE: Todo comando de criação ou remoção no 'up_script' e 'down_script' " +
            "deve ser obrigatoriamente envelopado em blocos condicionais (IF EXISTS / IF NOT EXISTS).\n" +
            "5. INTERVENÇÃO MANUAL DE CÓDIGO: Se o maior gargalo for código interno (ex: uma Função Escalar iterativa " +
            "ou Trigger pesada), NÃO tente reescrever o objeto inteiro. Ao invés disso, coloque um alerta estruturado " +
            "como comentário (/* ALERTA DBA: ... */) no 'up_script' orientando o refactoring manual, e prossiga " +
            "criando os índices possíveis para as outras falhas.\n" +
            "6. GARANTIA DE SAÍDA: NUNCA retorne 'up_script' ou 'down_script' vazios. Se não houver o que automatizar, " +
            "retorne um script com PRINTs e comentários.\n" +
            "7. SINTAXE T-SQL RIGOROSA: Ao gerar scripts para SQL Server, não utilize ponto-e-vírgula ';' isolado como terminador " +
            "obrigatório se isso quebrar blocos IF/BEGIN/END. NUNCA utilize 'GO' nos scripts, pois o driver Go MSSQL não suporta. " +
            "Mantenha a sintaxe impecável para não causar 'Incorrect syntax near'.\n\n" +
            "Responda EXCLUSIVAMENTE em JSON puro com as chaves exatas: \"diagnostico\", \"up_script\", \"down_script\". " +
            "Não inclua blocos markdown (```json), devolva apenas o objeto JSON.",
            dbEngine,
            ddl,
            (dmvStats != null ? dmvStats : "N/A"),
            (waitStats != null ? waitStats : "N/A"),
            (topQueries != null ? topQueries : "N/A"),
            (indexStats != null ? indexStats : "N/A"),
            (executionPlans != null ? executionPlans : "N/A")
        );

        try {
            ObjectNode textPart = objectMapper.createObjectNode();
            textPart.put("text", prompt);

            ArrayNode partsArray = objectMapper.createArrayNode();
            partsArray.add(textPart);

            ObjectNode contentNode = objectMapper.createObjectNode();
            contentNode.set("parts", partsArray);

            ArrayNode contentsArray = objectMapper.createArrayNode();
            contentsArray.add(contentNode);

            ObjectNode requestBodyNode = objectMapper.createObjectNode();
            requestBodyNode.set("contents", contentsArray);

            String requestBody = objectMapper.writeValueAsString(requestBodyNode);

            String response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode rootNode = objectMapper.readTree(response);
            String rawText = rootNode.path("candidates").get(0)
                                     .path("content").path("parts").get(0)
                                     .path("text").asText();

            String cleanedJson = rawText.replaceAll("(?i)^```json\\s*", "")
                                        .replaceAll("(?i)^```\\s*", "")
                                        .replaceAll("\\s*```$", "")
                                        .trim();

            return objectMapper.readValue(cleanedJson, AiAnalysisResultDTO.class);

        } catch (Exception e) {
            AiAnalysisResultDTO errorDto = new AiAnalysisResultDTO();
            errorDto.setDiagnostico("Erro ao processar integração com Gemini: " + e.getMessage());
            errorDto.setUpScript("-- Erro de integracao\n");
            errorDto.setDownScript("-- Erro de integracao\n");
            return errorDto;
        }
    }

    // Compatibilidade com chamadas antigas
    public AiAnalysisResultDTO analyzeWithGemini(String ddl, String dmvStats, String apiKey, String aiModel, String dbEngine) {
        return analyzeWithGeminiRich(ddl, dmvStats, null, null, null, null, apiKey, aiModel, dbEngine);
    }

    public void testConnection(String apiKey, String aiModel) {
        String cleanModel = aiModel.startsWith("models/") ? aiModel.replace("models/", "") : aiModel;
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + cleanModel + ":generateContent?key=" + apiKey;
        try {
            ObjectNode textPart = objectMapper.createObjectNode();
            textPart.put("text", "Responda apenas 'Conectado'");

            ArrayNode partsArray = objectMapper.createArrayNode();
            partsArray.add(textPart);

            ObjectNode contentNode = objectMapper.createObjectNode();
            contentNode.set("parts", partsArray);

            ArrayNode contentsArray = objectMapper.createArrayNode();
            contentsArray.add(contentNode);

            ObjectNode requestBodyNode = objectMapper.createObjectNode();
            requestBodyNode.set("contents", contentsArray);

            String requestBody = objectMapper.writeValueAsString(requestBodyNode);

            restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new IllegalArgumentException("Erro ao conectar com Gemini: " + e.getMessage(), e);
        }
    }
}