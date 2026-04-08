package it.brunasti.dbdadi.frontend.client;

import it.brunasti.dbdadi.frontend.dto.RelationshipDefinitionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RelationshipDefinitionClient {

    private static final String BASE_PATH = "/api/v1/relationships";
    private final RestClient restClient;

    public List<RelationshipDefinitionDto> findAll() {
        return restClient.get()
                .uri(BASE_PATH)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<RelationshipDefinitionDto> findByFromTable(Long fromTableId) {
        return restClient.get()
                .uri(BASE_PATH + "?fromTableId={id}", fromTableId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<RelationshipDefinitionDto> findByToTable(Long toTableId) {
        return restClient.get()
                .uri(BASE_PATH + "?toTableId={id}", toTableId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<RelationshipDefinitionDto> findByDatabaseModel(Long databaseModelId) {
        return restClient.get()
                .uri(BASE_PATH + "?databaseModelId={id}", databaseModelId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public RelationshipDefinitionDto findById(Long id) {
        return restClient.get()
                .uri(BASE_PATH + "/{id}", id)
                .retrieve()
                .body(RelationshipDefinitionDto.class);
    }

    public RelationshipDefinitionDto create(RelationshipDefinitionDto dto) {
        return restClient.post()
                .uri(BASE_PATH)
                .body(dto)
                .retrieve()
                .body(RelationshipDefinitionDto.class);
    }

    public RelationshipDefinitionDto update(Long id, RelationshipDefinitionDto dto) {
        return restClient.put()
                .uri(BASE_PATH + "/{id}", id)
                .body(dto)
                .retrieve()
                .body(RelationshipDefinitionDto.class);
    }

    public void delete(Long id) {
        restClient.delete()
                .uri(BASE_PATH + "/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }
}
