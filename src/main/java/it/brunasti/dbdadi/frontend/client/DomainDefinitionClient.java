package it.brunasti.dbdadi.frontend.client;

import it.brunasti.dbdadi.frontend.dto.DomainDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.EntityDefinitionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DomainDefinitionClient {

    private static final String BASE_PATH = "/api/v1/domains";
    private final RestClient restClient;

    public List<DomainDefinitionDto> findAll() {
        return restClient.get()
                .uri(BASE_PATH)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public DomainDefinitionDto findById(Long id) {
        return restClient.get()
                .uri(BASE_PATH + "/{id}", id)
                .retrieve()
                .body(DomainDefinitionDto.class);
    }

    public List<DomainDefinitionDto> findByEntity(Long entityId) {
        return restClient.get()
                .uri(BASE_PATH + "?entityId={id}", entityId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<EntityDefinitionDto> findEntities(Long domainId) {
        return restClient.get()
                .uri(BASE_PATH + "/{id}/entities", domainId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public DomainDefinitionDto create(DomainDefinitionDto dto) {
        return restClient.post()
                .uri(BASE_PATH)
                .body(dto)
                .retrieve()
                .body(DomainDefinitionDto.class);
    }

    public DomainDefinitionDto update(Long id, DomainDefinitionDto dto) {
        return restClient.put()
                .uri(BASE_PATH + "/{id}", id)
                .body(dto)
                .retrieve()
                .body(DomainDefinitionDto.class);
    }

    public void setEntities(Long domainId, List<Long> entityIds) {
        restClient.put()
                .uri(BASE_PATH + "/{id}/entities", domainId)
                .body(entityIds)
                .retrieve()
                .toBodilessEntity();
    }

    public void delete(Long id) {
        restClient.delete()
                .uri(BASE_PATH + "/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }
}
