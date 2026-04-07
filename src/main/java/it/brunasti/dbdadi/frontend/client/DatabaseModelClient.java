package it.brunasti.dbdadi.frontend.client;

import it.brunasti.dbdadi.frontend.dto.DatabaseModelDto;
import it.brunasti.dbdadi.frontend.dto.DbType;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DatabaseModelClient {

    private static final String BASE_PATH = "/api/v1/database-models";
    private final RestClient restClient;

    public List<DatabaseModelDto> findAll() {
        return restClient.get()
                .uri(BASE_PATH)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<DatabaseModelDto> findByDbType(DbType dbType) {
        return restClient.get()
                .uri(BASE_PATH + "?dbType={type}", dbType)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public DatabaseModelDto findById(Long id) {
        return restClient.get()
                .uri(BASE_PATH + "/{id}", id)
                .retrieve()
                .body(DatabaseModelDto.class);
    }

    public DatabaseModelDto create(DatabaseModelDto dto) {
        return restClient.post()
                .uri(BASE_PATH)
                .body(dto)
                .retrieve()
                .body(DatabaseModelDto.class);
    }

    public DatabaseModelDto update(Long id, DatabaseModelDto dto) {
        return restClient.put()
                .uri(BASE_PATH + "/{id}", id)
                .body(dto)
                .retrieve()
                .body(DatabaseModelDto.class);
    }

    public void delete(Long id) {
        restClient.delete()
                .uri(BASE_PATH + "/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }
}
