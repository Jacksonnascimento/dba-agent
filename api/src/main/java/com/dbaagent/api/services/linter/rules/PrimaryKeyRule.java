package com.dbaagent.api.services.linter.rules;

import com.dbaagent.api.services.linter.LinterRule;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PrimaryKeyRule implements LinterRule {

    @Override
    public Optional<String> evaluate(String ddl) {
        String upperDdl = ddl.toUpperCase();
        
        // Regra básica: se tem CREATE TABLE, deveria ter PRIMARY KEY
        if (upperDdl.contains("CREATE TABLE") && !upperDdl.contains("PRIMARY KEY")) {
            return Optional.of("[Linter] ALERTA CRÍTICO: A tabela não possui uma Primary Key definida. Isso causará table scans constantes e problemas na replicação.");
        }
        
        return Optional.empty();
    }
}