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
public class AnalysisEntitySuggestion {
    private String suggestedName;
    private Long existingEntityId;
    private List<Long> tableIds;
    private List<String> tableLabels;
}
