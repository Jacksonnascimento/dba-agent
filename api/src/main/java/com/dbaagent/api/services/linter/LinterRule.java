package com.dbaagent.api.services.linter;

import java.util.Optional;

public interface LinterRule {
    /**
     * Analisa o DDL e retorna uma sugestão de melhoria, ou Optional.empty() se estiver tudo certo.
     */
    Optional<String> evaluate(String ddl);
}