package com.dbaagent.api.services.schema;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AiScriptSchemaValidator {

    private static final Pattern CREATE_INDEX = Pattern.compile(
            "(?is)CREATE\\s+(?:NONCLUSTERED\\s+)?INDEX\\s+(\\w+)\\s+ON\\s+" +
                    "(?:\\[?dbo\\]?\\.)?\\[?(\\w+)\\]?\\s*\\(([^)]+)\\)" +
                    "(?:\\s+INCLUDE\\s*\\(([^)]+)\\))?");

    // CORREÇÃO: Adicionado [^;]*;? para engolir o resto do comando (ex: REBUILD WITH...)
    private static final Pattern ALTER_INDEX_ON = Pattern.compile(
            "(?is)ALTER\\s+INDEX\\s+[^\\s]+\\s+ON\\s+(?:\\[?dbo\\]?\\.)?\\[?(\\w+)\\]?[^;]*;?");

    // CORREÇÃO: Adicionado [^;]*;? para engolir o resto do comando (ex: WITH FULLSCAN;)
    private static final Pattern UPDATE_STATS = Pattern.compile(
            "(?is)UPDATE\\s+STATISTICS\\s+(?:\\[?dbo\\]?\\.)?\\[?(\\w+)\\]?[^;]*;?");

    public record ValidationResult(String script, List<String> warnings) {}

    public ValidationResult sanitize(String script, DdlSchemaIndex schema) {
        if (!StringUtils.hasText(script) || schema.tables().isEmpty()) {
            return new ValidationResult(script, List.of());
        }

        List<String> warnings = new ArrayList<>();
        Set<String> reported = new LinkedHashSet<>();
        String working = script;

        Matcher idx = CREATE_INDEX.matcher(working);
        List<int[]> removeRanges = new ArrayList<>();

        while (idx.find()) {
            String table = DdlSchemaIndex.normalizeIdent(idx.group(2));
            String reason = validateIndexBlock(schema, table, idx.group(3), idx.group(4));
            if (reason != null) {
                if (reported.add(reason)) {
                    warnings.add(reason);
                }
                int start = findBlockStart(working, idx.start());
                int end = idx.end();

                // CORREÇÃO: Avança o 'end' para capturar o "END" do bloco IF ou o ";" do comando
                String searchStr = working.toUpperCase(Locale.ROOT);
                int endOfBlock = searchStr.indexOf("END", end);
                if (endOfBlock > 0 && endOfBlock < end + 500) { 
                    // Limite de 500 caracteres para evitar pegar um END de outro bloco distante
                    end = endOfBlock + 3; // +3 engloba a palavra "END"
                } else {
                    int semiColon = searchStr.indexOf(";", end);
                    if (semiColon > 0 && semiColon < end + 200) {
                        end = semiColon + 1; // Engloba o ";"
                    }
                }

                removeRanges.add(new int[] { start, end });
            }
        }

        working = applyRemovals(working, removeRanges, reported, warnings);
        working = removeInvalidTableRefs(working, ALTER_INDEX_ON, schema, "ALTER INDEX", warnings, reported);
        working = removeInvalidTableRefs(working, UPDATE_STATS, schema, "UPDATE STATISTICS", warnings, reported);

        return new ValidationResult(working, warnings);
    }

    private static String validateIndexBlock(
            DdlSchemaIndex schema,
            String table,
            String keyCols,
            String includeCols) {
        if (!schema.hasTable(table)) {
            return "Tabela '" + table + "' não existe no DDL coletado.";
        }
        for (String col : splitColumns(keyCols)) {
            if (!schema.hasColumn(table, col)) {
                return "Coluna '" + col + "' não existe na tabela '" + table + "' no DDL coletado.";
            }
        }
        if (includeCols != null) {
            for (String col : splitColumns(includeCols)) {
                if (!schema.hasColumn(table, col)) {
                    return "Coluna INCLUDE '" + col + "' não existe na tabela '" + table + "' no DDL coletado.";
                }
            }
        }
        return null;
    }

    private static int findBlockStart(String script, int createIndexStart) {
        int searchFrom = Math.max(0, createIndexStart - 500);
        String prefix = script.substring(searchFrom, createIndexStart);
        int ifIdx = prefix.toUpperCase(Locale.ROOT).lastIndexOf("IF NOT EXISTS");
        if (ifIdx >= 0) {
            return searchFrom + ifIdx;
        }
        return createIndexStart;
    }

    private static String applyRemovals(
            String script,
            List<int[]> removeRanges,
            Set<String> reported,
            List<String> warnings) {
        if (removeRanges.isEmpty()) {
            return script;
        }
        removeRanges.sort((a, b) -> Integer.compare(b[0], a[0]));
        StringBuilder sb = new StringBuilder(script);
        for (int[] range : removeRanges) {
            String comment = "\n-- [Removido pela validação] Trecho omitido (tabela/coluna ausente no DDL).\n";
            sb.replace(range[0], range[1], comment);
        }
        return sb.toString();
    }

    private static String removeInvalidTableRefs(
            String script,
            Pattern pattern,
            DdlSchemaIndex schema,
            String commandLabel,
            List<String> warnings,
            Set<String> reported) {
        Matcher m = pattern.matcher(script);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String table = DdlSchemaIndex.normalizeIdent(m.group(1));
            if (schema.hasTable(table)) {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
            } else {
                String reason = commandLabel + " em tabela '" + table + "' omitido: tabela ausente no DDL.";
                if (reported.add(reason)) {
                    warnings.add(reason);
                }
                // CORREÇÃO: A mensagem de substituição agora é sempre um comentário SQL válido
                m.appendReplacement(sb, "\n-- [Removido pela validação] " + reason + "\n");
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static List<String> splitColumns(String list) {
        List<String> out = new ArrayList<>();
        if (!StringUtils.hasText(list)) {
            return out;
        }
        for (String part : list.split(",")) {
            String col = DdlSchemaIndex.normalizeIdent(part);
            if (!col.isEmpty()) {
                out.add(col);
            }
        }
        return out;
    }
}