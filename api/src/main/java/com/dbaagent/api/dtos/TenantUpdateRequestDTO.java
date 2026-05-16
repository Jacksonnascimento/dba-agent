package com.dbaagent.api.dtos;

import jakarta.validation.constraints.NotBlank;

public class TenantUpdateRequestDTO {

    @NotBlank(message = "O nome da empresa é obrigatório.")
    private String name;

    private Boolean active;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
