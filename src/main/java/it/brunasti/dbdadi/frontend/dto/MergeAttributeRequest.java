package it.brunasti.dbdadi.frontend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeAttributeRequest {
    private Long sourceAttributeId;
    private Long targetAttributeId;
}
