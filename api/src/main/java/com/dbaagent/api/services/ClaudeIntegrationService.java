package com.dbaagent.api.services;

import com.dbaagent.api.dtos.AiAnalysisResultDTO;
import com.dbaagent.api.services.AiPromptService.AiPromptParts;
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
    private final AiAnalysisResponseParser responseParser;

    public ClaudeIntegrationService(ObjectMapper objectMapper, AiAnalysisResponseParser responseParser) {
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
        this.responseParser = responseParser;
    }

    public AiAnalysisResultDTO analyzeWithClaudeRich(
            AiPromptParts promptParts,
            String apiKey,
            String aiModel) {

        String url = "https://api.anthropic.com/v1/messages";

        try {
            ObjectNode requestBodyNode = objectMapper.createObjectNode();
            requestBodyNode.put("model", aiModel);
            requestBodyNode.put("max_tokens", 4096);
            requestBodyNode.put("system", promptParts.systemInstructions());

            ArrayNode messagesArray = objectMapper.createArrayNode();
            ObjectNode messageNode = objectMapper.createObjectNode();
            messageNode.put("role", "user");
            messageNode.put("content", promptParts.telemetryContext());
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

            return responseParser.parse(rawText);

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
