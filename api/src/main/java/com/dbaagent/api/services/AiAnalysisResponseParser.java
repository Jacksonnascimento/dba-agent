package com.dbaagent.api.services;

import com.dbaagent.api.dtos.AiAnalysisResultDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Normalizes LLM output into {@link AiAnalysisResultDTO}, tolerating markdown fences and leading prose.
 */
@Component
public class AiAnalysisResponseParser {

    private final ObjectMapper objectMapper;

    public AiAnalysisResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiAnalysisResultDTO parse(String rawText) throws Exception {
        String cleaned = stripMarkdownFences(rawText);
        try {
            return objectMapper.readValue(cleaned, AiAnalysisResultDTO.class);
        } catch (Exception first) {
            String jsonObject = extractJsonObject(cleaned);
            if (jsonObject != null) {
                return objectMapper.readValue(jsonObject, AiAnalysisResultDTO.class);
            }
            throw first;
        }
    }

    static String stripMarkdownFences(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("(?i)^```json\\s*", "")
                .replaceAll("(?i)^```\\s*", "")
                .replaceAll("\\s*```\\s*$", "")
                .trim();
    }

    /**
     * Finds the outermost JSON object in the text (first '{' to matching '}').
     */
    static String extractJsonObject(String text) {
        int start = text.indexOf('{');
        if (start < 0) {
            return null;
        }
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                continue;
            }
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        return null;
    }
}
