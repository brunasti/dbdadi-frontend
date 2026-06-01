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
public class AttributeEntitySuggestion {
    private Long entityId;
    private String entityName;
    private String entityDescription;
    private List<String> viaTableNames;
    private int linkedColumnsCount;
}
