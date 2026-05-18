package com.dbaagent.api.services;

import com.dbaagent.api.dtos.AiAnalysisResultDTO;
import com.dbaagent.api.services.schema.AiScriptFormatter;
import com.dbaagent.api.services.schema.AiScriptSchemaValidator;
import com.dbaagent.api.services.schema.DdlSchemaIndex;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AiAnalysisPostProcessor {

    private final AiScriptFormatter scriptFormatter;
    private final AiScriptSchemaValidator schemaValidator;

    public AiAnalysisPostProcessor(AiScriptFormatter scriptFormatter, AiScriptSchemaValidator schemaValidator) {
        this.scriptFormatter = scriptFormatter;
        this.schemaValidator = schemaValidator;
    }

    public AiAnalysisResultDTO process(AiAnalysisResultDTO ai, String ddl) {
        if (ai == null) {
            return null;
        }
        if (isIntegrationError(ai)) {
            return ai;
        }

        DdlSchemaIndex schema = DdlSchemaIndex.parse(ddl);

        AiScriptSchemaValidator.ValidationResult up = schemaValidator.sanitize(ai.getUpScript(), schema);
        AiScriptSchemaValidator.ValidationResult down = schemaValidator.sanitize(ai.getDownScript(), schema);

        ai.setUpScript(scriptFormatter.format(up.script()));
        ai.setDownScript(scriptFormatter.format(down.script()));

        List<String> warnings = new java.util.ArrayList<>();
        warnings.addAll(up.warnings());
        warnings.addAll(down.warnings());

        if (!warnings.isEmpty()) {
            StringBuilder diag = new StringBuilder();
            if (StringUtils.hasText(ai.getDiagnostico())) {
                diag.append(ai.getDiagnostico().trim());
            }
            diag.append("\n\n--- Validação automática (DDL) ---\n");
            for (String w : warnings) {
                diag.append("• ").append(w).append("\n");
            }
            diag.append("Trechos inválidos foram removidos dos scripts para evitar erro em produção.");
            ai.setDiagnostico(diag.toString());
        }

        return ai;
    }

    private static boolean isIntegrationError(AiAnalysisResultDTO ai) {
        return StringUtils.hasText(ai.getDiagnostico())
                && ai.getDiagnostico().startsWith("Erro ao processar integração:");
    }
}
