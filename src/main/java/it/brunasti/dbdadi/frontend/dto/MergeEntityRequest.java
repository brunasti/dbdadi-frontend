package it.brunasti.dbdadi.frontend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeEntityRequest {
    private Long sourceEntityId;
    private Long targetEntityId;
}
