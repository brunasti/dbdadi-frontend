package it.brunasti.dbdadi.frontend.client;

import it.brunasti.dbdadi.frontend.dto.ColumnDefinitionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ColumnDefinitionClient {

    private static final String BASE_PATH = "/api/v1/columns";
    private final RestClient restClient;

    public List<ColumnDefinitionDto> findAll() {
        return restClient.get()
                .uri(BASE_PATH)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<ColumnDefinitionDto> findByTable(Long tableId) {
        return restClient.get()
                .uri(BASE_PATH + "?tableId={id}", tableId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<ColumnDefinitionDto> findBySchema(Long schemaId) {
        return restClient.get()
                .uri(BASE_PATH + "?schemaId={id}", schemaId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<ColumnDefinitionDto> findByDatabaseModel(Long databaseModelId) {
        return restClient.get()
                .uri(BASE_PATH + "?databaseModelId={id}", databaseModelId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<ColumnDefinitionDto> findByAttribute(Long attributeId) {
        return restClient.get()
                .uri(BASE_PATH + "?attributeId={id}", attributeId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public ColumnDefinitionDto findById(Long id) {
        return restClient.get()
                .uri(BASE_PATH + "/{id}", id)
                .retrieve()
                .body(ColumnDefinitionDto.class);
    }

    public ColumnDefinitionDto create(ColumnDefinitionDto dto) {
        return restClient.post()
                .uri(BASE_PATH)
                .body(dto)
                .retrieve()
                .body(ColumnDefinitionDto.class);
    }

    public ColumnDefinitionDto update(Long id, ColumnDefinitionDto dto) {
        return restClient.put()
                .uri(BASE_PATH + "/{id}", id)
                .body(dto)
                .retrieve()
                .body(ColumnDefinitionDto.class);
    }

    public void delete(Long id) {
        restClient.delete()
                .uri(BASE_PATH + "/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }
}
