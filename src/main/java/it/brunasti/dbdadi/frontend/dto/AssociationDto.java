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
public class AssociationDto {
    private Long id;
    private String name;
    private String description;
    private RelationshipType type;
    private Long fromEntityId;
    private String fromEntityName;
    private Long toEntityId;
    private String toEntityName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
