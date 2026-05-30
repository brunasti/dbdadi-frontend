package it.brunasti.dbdadi.frontend.client;

import it.brunasti.dbdadi.frontend.dto.AnalysisApplyRequest;
import it.brunasti.dbdadi.frontend.dto.AnalysisApplyResult;
import it.brunasti.dbdadi.frontend.dto.AnalysisResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class AnalysisClient {

    private static final String BASE_PATH = "/api/v1/analysis";
    private final RestClient restClient;

    public AnalysisResult run() {
        return restClient.post()
                .uri(BASE_PATH + "/run")
                .retrieve()
                .body(AnalysisResult.class);
    }

    public AnalysisApplyResult apply(AnalysisApplyRequest request) {
        return restClient.post()
                .uri(BASE_PATH + "/apply")
                .body(request)
                .retrieve()
                .body(AnalysisApplyResult.class);
    }
}
