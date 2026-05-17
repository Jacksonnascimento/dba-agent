package com.dbaagent.api.services;

import com.dbaagent.api.dtos.AiAnalysisResultDTO;
import com.dbaagent.api.entities.AgentWorker;
import com.dbaagent.api.entities.DatabaseConnection;
import com.dbaagent.api.entities.OptimizationSuggestion;
import com.dbaagent.api.entities.SemanticCache;
import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.services.AiPromptService.AiPromptParts;
import com.dbaagent.api.enums.SuggestionStatus;
import com.dbaagent.api.repositories.OptimizationSuggestionRepository;
import com.dbaagent.api.services.linter.LinterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OptimizationAnalysisService {

    private static final String DEFAULT_AI_MODEL = "gemini-2.5-flash";
    private static final Pattern FIRST_TABLE = Pattern.compile("(?i)CREATE\\s+TABLE\\s+(?:\\[?)(\\w+)(?:\\]?)", Pattern.CASE_INSENSITIVE);

    private final LinterService linterService;
    private final SemanticCacheService semanticCacheService;
    private final GeminiIntegrationService geminiIntegrationService;
    private final ClaudeIntegrationService claudeIntegrationService;
    private final OptimizationSuggestionRepository suggestionRepository;
    private final ObjectMapper objectMapper;
    private final DatabaseTelemetrySnapshotService snapshotService;
    private final AiPromptService aiPromptService;

    public OptimizationAnalysisService(
            LinterService linterService,
            SemanticCacheService semanticCacheService,
            GeminiIntegrationService geminiIntegrationService,
            ClaudeIntegrationService claudeIntegrationService,
            OptimizationSuggestionRepository suggestionRepository,
            ObjectMapper objectMapper,
            DatabaseTelemetrySnapshotService snapshotService,
            AiPromptService aiPromptService) {
        this.linterService = linterService;
        this.semanticCacheService = semanticCacheService;
        this.geminiIntegrationService = geminiIntegrationService;
        this.claudeIntegrationService = claudeIntegrationService;
        this.suggestionRepository = suggestionRepository;
        this.objectMapper = objectMapper;
        this.snapshotService = snapshotService;
        this.aiPromptService = aiPromptService;
    }

    @Transactional
    public OptimizationSuggestion analyzeAndPersist(
            Tenant tenant,
            DatabaseConnection databaseConnection,
            AgentWorker agentWorker,
            String schemaDdl,
            String dmvStats,
            String dbEngine,
            String aiModel) {
        
        String ddl = schemaDdl != null ? schemaDdl.trim() : "";
        String hash = sha256Hex(ddl);
        String dbName = databaseConnection.getName();
        String tableName = extractFirstTableName(ddl);

        Optional<OptimizationSuggestion> pendingDup =
                suggestionRepository.findByTenantAndDatabaseConnectionAndSchemaHashAndStatus(
                        tenant,
                        databaseConnection,
                        hash,
                        SuggestionStatus.PENDING);
        if (pendingDup.isPresent()) {
            return pendingDup.get();
        }

        List<String> linterFindings = linterService.runLinter(ddl);
        if (!linterFindings.isEmpty()) {
            String diagnosis = String.join("\n", linterFindings);
            String up = "-- [Linter] Nenhum script automático. Revise o diagnóstico e elabore o ALTER manualmente se aplicável.\n";
            String down = "-- [Linter] Rollback depende das alterações manuais aplicadas.\n";
            return saveSuggestion(
                    tenant,
                    databaseConnection,
                    hash,
                    dbName,
                    tableName,
                    diagnosis,
                    up,
                    down,
                    SuggestionStatus.PENDING);
        }

        Optional<SemanticCache> cached = semanticCacheService.checkCacheByContextHash(hash, tenant, databaseConnection);
        if (cached.isPresent()) {
            try {
                AiAnalysisResultDTO fromCache = objectMapper.readValue(
                        cached.get().getSuggestedImprovement(), AiAnalysisResultDTO.class);
                OptimizationSuggestion saved = saveSuggestion(
                        tenant,
                        databaseConnection,
                        hash,
                        dbName,
                        tableName,
                        fromCache.getDiagnostico(),
                        nullToComment(fromCache.getUpScript()),
                        nullToComment(fromCache.getDownScript()),
                        SuggestionStatus.PENDING);
                saved.setSchemaHash(hash);
                saved.setContextHash(hash);
                return suggestionRepository.save(saved);
            } catch (Exception e) {
                // Cache corrompido ou formato antigo: segue para IA
            }
        }

        AiAnalysisResultDTO ai = callAi(
                tenant, databaseConnection, agentWorker, ddl, dmvStats, null, null, null, null, dbEngine, aiModel);

        if (!StringUtils.hasText(ai.getUpScript()) && StringUtils.hasText(ai.getDiagnostico())
                && ai.getDiagnostico().startsWith("Erro ao processar integração:")) {
            OptimizationSuggestion saved = saveSuggestion(tenant, databaseConnection, hash, dbName, tableName, ai.getDiagnostico(),
                    "-- Falha na integração com a IA.\n",
                    "-- N/A\n",
                    SuggestionStatus.PENDING);
            saved.setSchemaHash(hash);
            saved.setContextHash(hash);
            return suggestionRepository.save(saved);
        }

        try {
            String cachePayload = objectMapper.writeValueAsString(ai);
            // Passamos hash nos dois parametros para não violar constraint do BD interno
            semanticCacheService.saveToCache(hash, hash, cachePayload, "GEMINI", tenant, databaseConnection);
        } catch (Exception ignored) {
            // cache best-effort
        }

        OptimizationSuggestion saved = saveSuggestion(
                tenant,
                databaseConnection,
                hash,
                dbName,
                tableName,
                ai.getDiagnostico(),
                nullToComment(ai.getUpScript()),
                nullToComment(ai.getDownScript()),
                SuggestionStatus.PENDING);
        saved.setSchemaHash(hash);
        saved.setContextHash(hash);
        return suggestionRepository.save(saved);
    }

    @Transactional
    public OptimizationSuggestion analyzeAndPersistFromSnapshot(
            Tenant tenant,
            DatabaseConnection databaseConnection,
            AgentWorker agentWorker,
            String schemaDdl,
            String dmvStats,
            String waitStats,
            String topQueries,
            String executionPlans,
            String indexStats,
            String dbEngine,
            String aiModel) {
        
        String ddl = schemaDdl != null ? schemaDdl.trim() : "";
        String schemaHash = sha256Hex(ddl);
        String contextHash = sha256Hex(
                ddl +
                        "\n--dmv--\n" + (dmvStats == null ? "" : dmvStats.trim()) +
                        "\n--waits--\n" + (waitStats == null ? "" : waitStats.trim()) +
                        "\n--topq--\n" + (topQueries == null ? "" : topQueries.trim()) +
                        "\n--plans--\n" + (executionPlans == null ? "" : executionPlans.trim()) +
                        "\n--indexes--\n" + (indexStats == null ? "" : indexStats.trim()));

        String dbName = databaseConnection.getName();
        String tableName = extractFirstTableName(ddl);

        Optional<OptimizationSuggestion> pendingDup =
                suggestionRepository.findByTenantAndDatabaseConnectionAndContextHashAndStatus(
                        tenant,
                        databaseConnection,
                        contextHash,
                        SuggestionStatus.PENDING);
        if (pendingDup.isPresent()) {
            return pendingDup.get();
        }

        List<String> linterFindings = linterService.runLinter(ddl);
        if (!linterFindings.isEmpty()) {
            String diagnosis = String.join("\n", linterFindings);
            String up = "-- [Linter] Nenhum script automático. Revise o diagnóstico e elabore o ALTER manualmente se aplicável.\n";
            String down = "-- [Linter] Rollback depende das alterações manuais aplicadas.\n";
            OptimizationSuggestion saved = saveSuggestion(
                    tenant,
                    databaseConnection,
                    contextHash,
                    dbName,
                    tableName,
                    diagnosis,
                    up,
                    down,
                    SuggestionStatus.PENDING);
            saved.setSchemaHash(schemaHash);
            saved.setContextHash(contextHash);
            return suggestionRepository.save(saved);
        }

        Optional<SemanticCache> cached = semanticCacheService.checkCacheByContextHash(contextHash, tenant, databaseConnection);
        if (cached.isPresent()) {
            try {
                AiAnalysisResultDTO fromCache = objectMapper.readValue(
                        cached.get().getSuggestedImprovement(), AiAnalysisResultDTO.class);
                OptimizationSuggestion saved = saveSuggestion(
                        tenant,
                        databaseConnection,
                        contextHash,
                        dbName,
                        tableName,
                        fromCache.getDiagnostico(),
                        nullToComment(fromCache.getUpScript()),
                        nullToComment(fromCache.getDownScript()),
                        SuggestionStatus.PENDING);
                saved.setSchemaHash(schemaHash);
                saved.setContextHash(contextHash);
                return suggestionRepository.save(saved);
            } catch (Exception e) {
                // segue para IA
            }
        }

        AiAnalysisResultDTO ai = callAi(
                tenant, databaseConnection, agentWorker, ddl, dmvStats, waitStats, topQueries, executionPlans, indexStats,
                dbEngine, aiModel);

        try {
            String cachePayload = objectMapper.writeValueAsString(ai);
            // Passamos contextHash nos dois parametros para contornar a constraint uk_schema_hash momentaneamente e evitar transação envenenada
            semanticCacheService.saveToCache(contextHash, contextHash, cachePayload, "GEMINI", tenant, databaseConnection);
        } catch (Exception ignored) {
        }

        OptimizationSuggestion saved = saveSuggestion(
                tenant,
                databaseConnection,
                contextHash,
                dbName,
                tableName,
                ai.getDiagnostico(),
                nullToComment(ai.getUpScript()),
                nullToComment(ai.getDownScript()),
                SuggestionStatus.PENDING);
        saved.setSchemaHash(schemaHash);
        saved.setContextHash(contextHash);
        return suggestionRepository.save(saved);
    }

    private OptimizationSuggestion saveSuggestion(
            Tenant tenant,
            DatabaseConnection databaseConnection,
            String schemaHash,
            String databaseName,
            String tableName,
            String suggestionText,
            String upScript,
            String downScript,
            SuggestionStatus status) {

        OptimizationSuggestion s = OptimizationSuggestion.builder()
                .tenant(tenant)
                .databaseConnection(databaseConnection)
                .schemaHash(schemaHash)
                .contextHash(schemaHash)
                .databaseName(databaseName)
                .tableName(tableName)
                .suggestionText(suggestionText)
                .diagnosis(suggestionText) // Preenche a coluna obrigatória
                .upScript(upScript)
                .downScript(downScript)
                .status(status)
                .build();
        return suggestionRepository.save(s);
    }

    private AiAnalysisResultDTO callAi(
            Tenant tenant,
            DatabaseConnection databaseConnection,
            AgentWorker agentWorker,
            String ddl,
            String dmvStats,
            String waitStats,
            String topQueries,
            String executionPlans,
            String indexStats,
            String dbEngine,
            String aiModel) {

        String provider = tenant.getAiProvider() != null ? tenant.getAiProvider() : "GEMINI";
        String model = StringUtils.hasText(tenant.getAiModel()) ? tenant.getAiModel() :
                (StringUtils.hasText(aiModel) ? aiModel : ("CLAUDE".equalsIgnoreCase(provider)
                        ? "claude-3-5-sonnet-20241022" : DEFAULT_AI_MODEL));
        String engine = StringUtils.hasText(dbEngine) ? dbEngine : databaseConnection.getDbEngine();

        AiPromptParts parts = aiPromptService.buildForAnalysis(
                engine, agentWorker, databaseConnection, ddl, dmvStats, waitStats, topQueries, executionPlans, indexStats);

        if ("CLAUDE".equalsIgnoreCase(provider)) {
            if (!StringUtils.hasText(tenant.getClaudeApiKey())) {
                return missingKeyAiResult("Configure a chave Claude (BYOK) no tenant para análise por IA.");
            }
            return claudeIntegrationService.analyzeWithClaudeRich(parts, tenant.getClaudeApiKey(), model);
        }

        if (!StringUtils.hasText(tenant.getGeminiApiKey())) {
            return missingKeyAiResult("Configure a chave Gemini (BYOK) no tenant para análise por IA.");
        }
        return geminiIntegrationService.analyzeWithGeminiRich(parts, tenant.getGeminiApiKey(), model);
    }

    private static AiAnalysisResultDTO missingKeyAiResult(String message) {
        AiAnalysisResultDTO dto = new AiAnalysisResultDTO();
        dto.setDiagnostico(message);
        dto.setUpScript("-- Aguardando configuração de API key no tenant.\n");
        dto.setDownScript("-- N/A\n");
        return dto;
    }

    private static String nullToComment(String sql) {
        if (!StringUtils.hasText(sql)) {
            return "-- (vazio)\n";
        }
        return sql.endsWith("\n") ? sql : sql + "\n";
    }

    private static String extractFirstTableName(String ddl) {
        Matcher m = FIRST_TABLE.matcher(ddl);
        return m.find() ? m.group(1) : "unknown";
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}