package com.dbaagent.api.services;

import com.dbaagent.api.dtos.AiAnalysisResultDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AiAnalysisResponseParserTest {

    private final AiAnalysisResponseParser parser = new AiAnalysisResponseParser(new ObjectMapper());

    @Test
    void parsePlainJson() throws Exception {
        String raw = "{\"diagnostico\":\"ok\",\"up_script\":\"-- u\",\"down_script\":\"-- d\"}";
        AiAnalysisResultDTO dto = parser.parse(raw);
        assertEquals("ok", dto.getDiagnostico());
    }

    @Test
    void parseJsonWithLeadingProse() throws Exception {
        String raw = "Olá! Aqui está o resultado:\n{\"diagnostico\":\"Olá Jackson\",\"up_script\":\"-- u\",\"down_script\":\"-- d\"}";
        AiAnalysisResultDTO dto = parser.parse(raw);
        assertEquals("Olá Jackson", dto.getDiagnostico());
    }

    @Test
    void extractJsonObject_findsNestedBracesInStrings() {
        String json = AiAnalysisResponseParser.extractJsonObject(
                "prefix {\"diagnostico\":\"texto com } char\",\"up_script\":\"x\",\"down_script\":\"y\"} suffix");
        assertNotNull(json);
        assertEquals(
                "{\"diagnostico\":\"texto com } char\",\"up_script\":\"x\",\"down_script\":\"y\"}",
                json);
    }
}
