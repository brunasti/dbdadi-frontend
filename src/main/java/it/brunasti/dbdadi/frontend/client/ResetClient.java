package it.brunasti.dbdadi.frontend.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class ResetClient {

    private final RestClient restClient;

    public void resetDatabase() {
        restClient.delete()
                .uri("/api/v1/admin/reset/database")
                .retrieve()
                .toBodilessEntity();
    }

    public void resetModeling() {
        restClient.delete()
                .uri("/api/v1/admin/reset/modeling")
                .retrieve()
                .toBodilessEntity();
    }
}
