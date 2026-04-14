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
public class AttributeDefinitionDto {
    private Long id;
    private String name;
    private String description;
    private Long entityId;
    private String entityName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
