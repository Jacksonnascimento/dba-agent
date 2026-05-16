package com.dbaagent.api.dtos;

import jakarta.validation.constraints.NotBlank;

public class TenantCreateRequestDTO {

    @NotBlank(message = "O nome da empresa é obrigatório.")
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
