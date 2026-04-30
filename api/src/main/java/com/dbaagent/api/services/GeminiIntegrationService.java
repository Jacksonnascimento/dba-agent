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

    // Método atualizado para receber o dbEngine
    public AiAnalysisResultDTO analyzeWithGemini(String ddl, String dmvStats, String apiKey, String aiModel, String dbEngine) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + aiModel + ":generateContent?key=" + apiKey;

        // PROMPT DINÂMICO: Agora a IA sabe exatamente qual é o banco!
        String prompt = String.format(
            "Você é um DBA Sênior focado em performance de %s. " +
            "Analise a seguinte estrutura: \n%s\n\n" +
            "Estatísticas dinâmicas (se houver): %s\n\n" +
            "Responda EXCLUSIVAMENTE em formato JSON com as chaves exatas: " +
            "'diagnostico' (explicação do problema focado na arquitetura do banco especificado), " +
            "'up_script' (código SQL compatível com o banco para aplicar a melhoria) e " +
            "'down_script' (código SQL compatível com o banco para reverter a melhoria). Não adicione textos fora do JSON.",
            dbEngine, ddl, (dmvStats != null ? dmvStats : "N/A")
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
}