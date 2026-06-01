package it.brunasti.dbdadi.frontend.client;

import it.brunasti.dbdadi.frontend.dto.BulkEntityRequest;
import it.brunasti.dbdadi.frontend.dto.BulkEntityResult;
import it.brunasti.dbdadi.frontend.dto.DomainDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.EntityDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.TableDefinitionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EntityDefinitionClient {

    private static final String BASE_PATH = "/api/v1/entities";
    private final RestClient restClient;

    public List<EntityDefinitionDto> findAll() {
        return restClient.get()
                .uri(BASE_PATH)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public EntityDefinitionDto findById(Long id) {
        return restClient.get()
                .uri(BASE_PATH + "/{id}", id)
                .retrieve()
                .body(EntityDefinitionDto.class);
    }

    public List<TableDefinitionDto> findTables(Long entityId) {
        return restClient.get()
                .uri("/api/v1/tables?entityId={id}", entityId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public EntityDefinitionDto create(EntityDefinitionDto dto) {
        return restClient.post()
                .uri(BASE_PATH)
                .body(dto)
                .retrieve()
                .body(EntityDefinitionDto.class);
    }

    public EntityDefinitionDto update(Long id, EntityDefinitionDto dto) {
        return restClient.put()
                .uri(BASE_PATH + "/{id}", id)
                .body(dto)
                .retrieve()
                .body(EntityDefinitionDto.class);
    }

    public List<DomainDefinitionDto> findDomains(Long entityId) {
        return restClient.get()
                .uri(BASE_PATH + "/{id}/domains", entityId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public void setDomains(Long entityId, List<Long> domainIds) {
        restClient.put()
                .uri(BASE_PATH + "/{id}/domains", entityId)
                .body(domainIds)
                .retrieve()
                .toBodilessEntity();
    }

    public BulkEntityResult bulkCreate(BulkEntityRequest request) {
        return restClient.post()
                .uri(BASE_PATH + "/bulk-create")
                .body(request)
                .retrieve()
                .body(BulkEntityResult.class);
    }

    public void delete(Long id) {
        restClient.delete()
                .uri(BASE_PATH + "/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }
}
