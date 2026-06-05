package it.brunasti.dbdadi.frontend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisAssociationSuggestion {
    private String suggestedName;
    private RelationshipType type;
    private Long fromEntityId;
    private String fromEntityName;
    private Long toEntityId;
    private String toEntityName;
    private Long existingAssociationId;
    private List<String> relationshipLabels;
}
