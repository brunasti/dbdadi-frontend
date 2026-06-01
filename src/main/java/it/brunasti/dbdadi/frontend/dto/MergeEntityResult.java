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
public class MergeEntityResult {
    private Long survivingEntityId;
    private String survivingEntityName;
    private int attributesMigrated;
    private int tablesMigrated;
    private int domainsMigrated;
    private List<String> warnings;
}
