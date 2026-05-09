package com.dbaagent.api.entities;

import com.dbaagent.api.enums.SuggestionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "optimization_suggestions")
public class OptimizationSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "database_connection_id", nullable = false)
    private DatabaseConnection databaseConnection;

    @Column(name = "schema_hash", length = 255)
    private String schemaHash;

    @Column(name = "context_hash", nullable = false, length = 255)
    private String contextHash;

    @Column(name = "database_name", nullable = false)
    private String databaseName;

    @Column(name = "table_name")
    private String tableName;

    @Column(name = "suggestion_text", columnDefinition = "TEXT", nullable = false)
    private String suggestionText;

    @Column(name = "up_script", columnDefinition = "TEXT", nullable = false)
    private String upScript;

    @Column(name = "down_script", columnDefinition = "TEXT", nullable = false)
    private String downScript;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuggestionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = SuggestionStatus.PENDING;
        }
        if (this.contextHash == null || this.contextHash.isBlank()) {
            this.contextHash = this.schemaHash;
        }
    }
}