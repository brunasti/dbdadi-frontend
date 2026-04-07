package it.brunasti.dbdadi.frontend.client;

import it.brunasti.dbdadi.frontend.dto.JdbcImportRequest;
import it.brunasti.dbdadi.frontend.dto.JdbcImportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class JdbcImportClient {

    private final RestClient restClient;

    public JdbcImportResult importFromJdbc(JdbcImportRequest request) {
        return restClient.post()
                .uri("/api/v1/import/jdbc")
                .body(request)
                .retrieve()
                .body(JdbcImportResult.class);
    }
}
