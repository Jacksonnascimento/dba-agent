package com.dbaagent.api.enums;

public enum SuggestionStatus {
    PENDING,   // Aguardando ação do usuário
    APPROVED,  // Aprovado, pronto para o Agente executar
    REJECTED,  // Usuário descartou a sugestão
    EXECUTED   // Agente rodou com sucesso no banco do cliente
}