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
public class ColumnDefinitionDto {
    private Long id;
    private String name;
    private String description;
    private String dataType;
    private Integer length;
    private Integer precision;
    private Integer scale;
    private boolean nullable;
    private boolean primaryKey;
    private boolean unique;
    private String defaultValue;
    private Integer ordinalPosition;
    private Long tableId;
    private String tableName;
    private Long schemaId;
    private String schemaName;
    private Long databaseModelId;
    private String databaseModelName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
