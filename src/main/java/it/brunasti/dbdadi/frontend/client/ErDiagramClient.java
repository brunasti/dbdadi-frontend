package it.brunasti.dbdadi.frontend.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class ErDiagramClient {

    private static final String BASE_PATH = "/api/v1/er-diagram";
    private final RestClient restClient;

    public String generate(Long domainId) {
        return restClient.get()
                .uri(b -> {
                    var u = b.path(BASE_PATH);
                    if (domainId != null) u = u.queryParam("domainId", domainId);
                    return u.build();
                })
                .accept(MediaType.TEXT_PLAIN)
                .retrieve()
                .body(String.class);
    }

    public String generateSvg(Long domainId) {
        return restClient.get()
                .uri(b -> {
                    var u = b.path(BASE_PATH + "/svg");
                    if (domainId != null) u = u.queryParam("domainId", domainId);
                    return u.build();
                })
                .accept(MediaType.parseMediaType("image/svg+xml"))
                .retrieve()
                .body(String.class);
    }

    public String generateForSchema(Long schemaId) {
        return restClient.get()
                .uri(BASE_PATH + "/schema/{id}", schemaId)
                .accept(MediaType.TEXT_PLAIN)
                .retrieve()
                .body(String.class);
    }

    public String generateSvgForSchema(Long schemaId) {
        return restClient.get()
                .uri(BASE_PATH + "/schema/{id}/svg", schemaId)
                .accept(MediaType.parseMediaType("image/svg+xml"))
                .retrieve()
                .body(String.class);
    }
}
