package it.brunasti.dbdadi.frontend.client;

import it.brunasti.dbdadi.frontend.dto.AlignmentRequest;
import it.brunasti.dbdadi.frontend.dto.AlignmentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class AlignmentClient {

    private static final String BASE_PATH = "/api/v1/alignment";
    private final RestClient restClient;

    public AlignmentResult check(Long databaseModelId, String password) {
        AlignmentRequest req = AlignmentRequest.builder()
                .databaseModelId(databaseModelId)
                .password(password)
                .build();
        return restClient.post()
                .uri(BASE_PATH)
                .body(req)
                .retrieve()
                .body(AlignmentResult.class);
    }
}
