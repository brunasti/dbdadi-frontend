package it.brunasti.dbdadi.frontend.client;

import it.brunasti.dbdadi.frontend.dto.SystemInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class SystemInfoClient {

    private final RestClient restClient;

    public SystemInfoDto get() {
        return restClient.get()
                .uri("/api/v1/system-info")
                .retrieve()
                .body(SystemInfoDto.class);
    }
}
