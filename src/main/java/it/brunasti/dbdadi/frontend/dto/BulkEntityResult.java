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
public class BulkEntityResult {
    private int entitiesCreated;
    private int entitiesReused;
    private int tablesLinked;
    private int tablesSkipped;
    private List<String> createdNames;
    private List<String> warnings;
}
