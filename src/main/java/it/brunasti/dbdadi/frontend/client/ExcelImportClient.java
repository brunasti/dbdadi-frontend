package it.brunasti.dbdadi.frontend.client;

import it.brunasti.dbdadi.frontend.dto.ExcelImportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class ExcelImportClient {

    private final RestClient restClient;

    public ExcelImportResult importExcel(byte[] fileBytes, String filename, boolean clearBeforeImport) {
        ByteArrayResource resource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() { return filename; }
        };
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);
        return restClient.post()
                .uri("/api/v1/admin/import/excel?clearBeforeImport={clear}", clearBeforeImport)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(ExcelImportResult.class);
    }
}
