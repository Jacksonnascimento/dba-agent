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
public class GeminiIntegrationService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AiAnalysisResponseParser responseParser;

    public GeminiIntegrationService(ObjectMapper objectMapper, AiAnalysisResponseParser responseParser) {
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
        this.responseParser = responseParser;
    }

    public AiAnalysisResultDTO analyzeWithGeminiRich(
            AiPromptParts promptParts,
            String apiKey,
            String aiModel) {

        String cleanModel = aiModel.startsWith("models/") ? aiModel.replace("models/", "") : aiModel;
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + cleanModel + ":generateContent?key=" + apiKey;

        String prompt = promptParts.systemInstructions() + "\n\n" + promptParts.telemetryContext();

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
            requestBodyNode.set("generationConfig", buildJsonGenerationConfig());

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

            return responseParser.parse(rawText);

        } catch (Exception e) {
            AiAnalysisResultDTO errorDto = new AiAnalysisResultDTO();
            errorDto.setDiagnostico("Erro ao processar integração com Gemini: " + e.getMessage());
            errorDto.setUpScript("-- Erro de integracao\n");
            errorDto.setDownScript("-- Erro de integracao\n");
            return errorDto;
        }
    }

    private ObjectNode buildJsonGenerationConfig() {
        ObjectNode config = objectMapper.createObjectNode();
        config.put("responseMimeType", "application/json");

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("diagnostico", stringProperty());
        properties.set("up_script", stringProperty());
        properties.set("down_script", stringProperty());
        schema.set("properties", properties);

        ArrayNode required = objectMapper.createArrayNode();
        required.add("diagnostico");
        required.add("up_script");
        required.add("down_script");
        schema.set("required", required);

        config.set("responseSchema", schema);
        return config;
    }

    private ObjectNode stringProperty() {
        ObjectNode prop = objectMapper.createObjectNode();
        prop.put("type", "string");
        return prop;
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
