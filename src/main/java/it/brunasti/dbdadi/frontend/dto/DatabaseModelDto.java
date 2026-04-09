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
public class DatabaseModelDto {
    private Long id;
    private String name;
    private String description;
    private DbType dbType;
    private String version;
    private String jdbcUrl;
    private String username;
    private String schemaPattern;
    private String tablePattern;
    private String importFlags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
