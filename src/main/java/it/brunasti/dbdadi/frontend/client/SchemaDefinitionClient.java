package it.brunasti.dbdadi.frontend.client;

import it.brunasti.dbdadi.frontend.dto.SchemaDefinitionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SchemaDefinitionClient {

    private static final String BASE_PATH = "/api/v1/schemas";
    private final RestClient restClient;

    public List<SchemaDefinitionDto> findAll() {
        return restClient.get()
                .uri(BASE_PATH)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<SchemaDefinitionDto> findByDatabaseModel(Long databaseModelId) {
        return restClient.get()
                .uri(BASE_PATH + "?databaseModelId={id}", databaseModelId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public SchemaDefinitionDto findById(Long id) {
        return restClient.get()
                .uri(BASE_PATH + "/{id}", id)
                .retrieve()
                .body(SchemaDefinitionDto.class);
    }

    public SchemaDefinitionDto create(SchemaDefinitionDto dto) {
        return restClient.post()
                .uri(BASE_PATH)
                .body(dto)
                .retrieve()
                .body(SchemaDefinitionDto.class);
    }

    public SchemaDefinitionDto update(Long id, SchemaDefinitionDto dto) {
        return restClient.put()
                .uri(BASE_PATH + "/{id}", id)
                .body(dto)
                .retrieve()
                .body(SchemaDefinitionDto.class);
    }

    public void delete(Long id) {
        restClient.delete()
                .uri(BASE_PATH + "/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }
}
