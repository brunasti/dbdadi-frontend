package it.brunasti.dbdadi.frontend.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class ExcelExportClient {

    private final RestClient restClient;

    public byte[] exportExcel() {
        return restClient.get()
                .uri("/api/v1/admin/export/excel")
                .retrieve()
                .body(byte[].class);
    }
}
