package com.dbaagent.api.services;

import com.dbaagent.api.dtos.AiAnalysisResultDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + aiModel + ":generateContent?key=" + apiKey;

        String prompt = String.format(
            "Você é um DBA Sênior focado em performance de %s.\n\n" +
            "OBJETIVO: sugerir otimizações complexas e seguras (com deploy e rollback), baseadas no contexto completo.\n\n" +
            "## Estrutura (DDL)\n%s\n\n" +
            "## DMVs / Estatísticas dinâmicas\n%s\n\n" +
            "## Wait Stats\n%s\n\n" +
            "## Top Queries (consultas mais custosas)\n%s\n\n" +
            "## Index Stats / Fragmentação / Missing indexes\n%s\n\n" +
            "## Execution Plans (planos de execução)\n%s\n\n" +
            "REGRAS:\n" +
            "- Gere no máximo 3 sugestões priorizadas.\n" +
            "- Cada sugestão deve ter up_script e down_script aplicáveis e compatíveis com o banco.\n" +
            "- Evite mudanças destrutivas; se houver risco, descreva no diagnostico.\n\n" +
            "Responda EXCLUSIVAMENTE em JSON com as chaves exatas: " +
            "'diagnostico', 'up_script', 'down_script'. Não adicione textos fora do JSON.",
            dbEngine,
            ddl,
            (dmvStats != null ? dmvStats : "N/A"),
            (waitStats != null ? waitStats : "N/A"),
            (topQueries != null ? topQueries : "N/A"),
            (indexStats != null ? indexStats : "N/A"),
            (executionPlans != null ? executionPlans : "N/A")
        );

        String requestBody = String.format(
            "{\"contents\": [{\"parts\": [{\"text\": \"%s\"}]}]}",
            prompt.replace("\"", "\\\"").replace("\n", "\\n")
        );

        try {
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

            String cleanedJson = rawText.replaceAll("^```json\\s*", "").replaceAll("\\s*```$", "").trim();

            return objectMapper.readValue(cleanedJson, AiAnalysisResultDTO.class);

        } catch (Exception e) {
            AiAnalysisResultDTO errorDto = new AiAnalysisResultDTO();
            errorDto.setDiagnostico("Erro ao processar integração: " + e.getMessage());
            return errorDto;
        }
    }

    // Compatibilidade com chamadas antigas
    public AiAnalysisResultDTO analyzeWithGemini(String ddl, String dmvStats, String apiKey, String aiModel, String dbEngine) {
        return analyzeWithGeminiRich(ddl, dmvStats, null, null, null, null, apiKey, aiModel, dbEngine);
    }
}