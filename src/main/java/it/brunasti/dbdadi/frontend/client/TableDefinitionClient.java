package it.brunasti.dbdadi.frontend.client;

import it.brunasti.dbdadi.frontend.dto.TableDefinitionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TableDefinitionClient {

    private static final String BASE_PATH = "/api/v1/tables";
    private final RestClient restClient;

    public List<TableDefinitionDto> findAll() {
        return restClient.get()
                .uri(BASE_PATH)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<TableDefinitionDto> findByDatabaseModel(Long databaseModelId) {
        return restClient.get()
                .uri(BASE_PATH + "?databaseModelId={id}", databaseModelId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public TableDefinitionDto findById(Long id) {
        return restClient.get()
                .uri(BASE_PATH + "/{id}", id)
                .retrieve()
                .body(TableDefinitionDto.class);
    }

    public TableDefinitionDto create(TableDefinitionDto dto) {
        return restClient.post()
                .uri(BASE_PATH)
                .body(dto)
                .retrieve()
                .body(TableDefinitionDto.class);
    }

    public TableDefinitionDto update(Long id, TableDefinitionDto dto) {
        return restClient.put()
                .uri(BASE_PATH + "/{id}", id)
                .body(dto)
                .retrieve()
                .body(TableDefinitionDto.class);
    }

    public void delete(Long id) {
        restClient.delete()
                .uri(BASE_PATH + "/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }
}
