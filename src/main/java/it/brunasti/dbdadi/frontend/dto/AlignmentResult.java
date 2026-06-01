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
public class AlignmentResult {
    private Long databaseModelId;
    private String databaseModelName;
    private String jdbcUrl;
    private boolean aligned;
    private int schemasChecked;
    private int tablesChecked;
    private int columnsChecked;
    private List<AlignmentItem> differences;
    private List<String> warnings;
}
