package com.dbaagent.api.services.schema;

import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Índice de tabelas/colunas extraído do DDL sintético gerado pelo agente (CREATE TABLE ...).
 */
public class DdlSchemaIndex {

    private static final Pattern CREATE_TABLE = Pattern.compile(
            "(?i)CREATE\\s+TABLE\\s+(?:\\[?dbo\\]?\\.)?\\[?(\\w+)\\]?\\s*\\(",
            Pattern.MULTILINE);

    private static final Pattern COLUMN_DEF = Pattern.compile(
            "(?i)\\b([A-Za-z_][A-Za-z0-9_]*)\\s+(?:varchar|nvarchar|char|nchar|int|bigint|decimal|numeric|datetime2?|date|time|bit|uniqueidentifier|float|real|money|text|ntext|image|binary|varbinary|smallint|tinyint|smalldatetime)\\b");

    private final Map<String, Set<String>> tableColumns;

    public DdlSchemaIndex(Map<String, Set<String>> tableColumns) {
        this.tableColumns = tableColumns;
    }

    public static DdlSchemaIndex parse(String ddl) {
        if (!StringUtils.hasText(ddl)) {
            return new DdlSchemaIndex(Collections.emptyMap());
        }

        Map<String, Set<String>> map = new HashMap<>();
        Matcher m = CREATE_TABLE.matcher(ddl);
        while (m.find()) {
            String table = normalizeIdent(m.group(1));
            int openParen = m.end() - 1;
            int closeParen = findClosingParen(ddl, openParen);
            if (closeParen < 0) {
                continue;
            }
            String body = ddl.substring(m.end(), closeParen);
            map.put(table, parseColumnNames(body));
        }
        return new DdlSchemaIndex(map);
    }

    public boolean hasTable(String table) {
        return tableColumns.containsKey(normalizeIdent(table));
    }

    public boolean hasColumn(String table, String column) {
        Set<String> cols = tableColumns.get(normalizeIdent(table));
        return cols != null && cols.contains(normalizeIdent(column));
    }

    public Set<String> columnsOf(String table) {
        Set<String> cols = tableColumns.get(normalizeIdent(table));
        return cols == null ? Set.of() : Collections.unmodifiableSet(cols);
    }

    public Set<String> tables() {
        return Collections.unmodifiableSet(tableColumns.keySet());
    }

    private static Set<String> parseColumnNames(String tableBody) {
        Set<String> cols = new HashSet<>();
        Matcher col = COLUMN_DEF.matcher(tableBody);
        while (col.find()) {
            String name = normalizeIdent(col.group(1));
            if (!isReservedWord(name)) {
                cols.add(name);
            }
        }
        Matcher pk = Pattern.compile("(?i)PRIMARY\\s+KEY\\s*\\(([^)]+)\\)").matcher(tableBody);
        if (pk.find()) {
            for (String part : pk.group(1).split(",")) {
                String c = normalizeIdent(part.replaceAll("[\\[\\]]", "").trim());
                if (!c.isEmpty()) {
                    cols.add(c);
                }
            }
        }
        return cols;
    }

    private static boolean isReservedWord(String name) {
        return switch (name) {
            case "CONSTRAINT", "PRIMARY", "KEY", "FOREIGN", "REFERENCES", "UNIQUE", "CLUSTERED", "NONCLUSTERED" -> true;
            default -> false;
        };
    }

    private static int findClosingParen(String text, int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    static String normalizeIdent(String ident) {
        if (ident == null) {
            return "";
        }
        return ident.replaceAll("[\\[\\]]", "").trim().toUpperCase(Locale.ROOT);
    }
}
