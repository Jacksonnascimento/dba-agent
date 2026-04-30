package com.dbaagent.api.services.linter;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LinterService {

    private final List<LinterRule> rules;

    // O Spring injeta a PrimaryKeyRule (e qualquer outra regra futura) aqui automaticamente
    public LinterService(List<LinterRule> rules) {
        this.rules = rules;
    }

    public List<String> runLinter(String ddl) {
        List<String> findings = new ArrayList<>();
        
        for (LinterRule rule : rules) {
            rule.evaluate(ddl).ifPresent(findings::add);
        }
        
        return findings;
    }
}