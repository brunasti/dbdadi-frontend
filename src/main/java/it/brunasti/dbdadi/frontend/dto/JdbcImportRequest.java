package it.brunasti.dbdadi.frontend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JdbcImportRequest {
    private String modelName;
    private String jdbcUrl;
    private String username;
    private String password;
    private String schemaPattern;
    private String tablePattern;
    private boolean includeViews;
    private boolean overwrite;
}
