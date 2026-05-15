package com.dbaagent.api.enums;

public enum SuggestionStatus {
    PENDING,          // Aguardando ação do usuário
    APPROVED,         // Aprovado, pronto para o Agente executar
    REJECTED,         // Usuário descartou a sugestão
    EXECUTED,         // Agente rodou com sucesso no banco do cliente
    FAILED,           // Agente falhou ao executar o UpScript
    ROLLBACK_PENDING, // Usuário solicitou Rollback, aguardando Agente
    ROLLED_BACK,      // Agente executou o DownScript com sucesso
    ROLLBACK_FAILED   // Agente falhou ao executar o DownScript
}