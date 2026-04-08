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
public class RelationshipDefinitionDto {
    private Long id;
    private String name;
    private String description;
    private RelationshipType type;
    private Long fromTableId;
    private String fromTableName;
    private String fromColumnName;
    private Long fromColumnId;
    private Long toTableId;
    private String toTableName;
    private String toColumnName;
    private Long toColumnId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
