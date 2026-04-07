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
public class JdbcImportResult {
    private Long databaseModelId;
    private String databaseModelName;
    private int schemasImported;
    private int tablesImported;
    private int columnsImported;
    private int relationshipsImported;
    private List<String> warnings;
}
