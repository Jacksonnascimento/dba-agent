package com.dbaagent.api.services.schema;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Formata scripts SQL para exibição e execução legível (quebras de linha entre comandos).
 */
@Component
public class AiScriptFormatter {

    public String format(String sql) {
        if (!StringUtils.hasText(sql)) {
            return sql;
        }

        String s = sql.trim();

        // Comentário de bloco seguido de comando
        s = s.replaceAll("(?i)(\\*/)\\s*(?=IF\\s|CREATE\\s|ALTER\\s|UPDATE\\s|PRINT\\s|DROP\\s)", "$1\n\n");

        // Ponto-e-vírgula seguido de novo comando
        s = s.replaceAll(";\\s*(?=IF\\s|CREATE\\s|ALTER\\s|UPDATE\\s|PRINT\\s|DROP\\s)", ";\n\n");

        // Comandos colados sem ';' (comum na saída da IA)
        s = s.replaceAll("(?i)(\\))\\s*(?=IF\\s+NOT\\s+EXISTS)", "$1\n\n");
        s = s.replaceAll("(?i)(;?)\\s*(CREATE\\s+(?:NONCLUSTERED\\s+)?INDEX)", "$1\n\n$2");
        s = s.replaceAll("(?i)(;?)\\s*(ALTER\\s+INDEX)", "$1\n\n$2");
        s = s.replaceAll("(?i)(;?)\\s*(UPDATE\\s+STATISTICS)", "$1\n\n$2");

        // BEGIN / END
        s = s.replaceAll("(?i)\\s*\\bBEGIN\\b", "\nBEGIN");
        s = s.replaceAll("(?i)\\bEND\\s*;", "\nEND;\n");

        // Normaliza múltiplas linhas em branco
        s = s.replaceAll("\n{3,}", "\n\n");

        if (!s.endsWith("\n")) {
            s = s + "\n";
        }
        return s;
    }
}
