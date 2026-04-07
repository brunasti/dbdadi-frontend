package it.brunasti.dbdadi.frontend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableDefinitionDto {
    private Long id;
    private String name;
    private String schemaName;
    private String description;
    private Long databaseModelId;
    private String databaseModelName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
