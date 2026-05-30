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
public class AnalysisAttributeSuggestion {
    private String suggestedName;
    private String entityName;
    private Long existingAttributeId;
    private List<Long> columnIds;
    private List<String> columnLabels;
}
