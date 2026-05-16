package com.dbaagent.api.dtos;

import java.time.LocalDateTime;

public class TenantResponseDTO {

    private Long id;
    private String name;
    private Boolean active;
    private LocalDateTime createdAt;

    public TenantResponseDTO() {}

    public TenantResponseDTO(Long id, String name, Boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.active = active;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
