package it.brunasti.dbdadi.frontend.client;

import it.brunasti.dbdadi.frontend.dto.AttributeDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.ColumnDefinitionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AttributeDefinitionClient {

    private static final String BASE_PATH = "/api/v1/attributes";
    private final RestClient restClient;

    public List<AttributeDefinitionDto> findAll() {
        return restClient.get()
                .uri(BASE_PATH)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public AttributeDefinitionDto findById(Long id) {
        return restClient.get()
                .uri(BASE_PATH + "/{id}", id)
                .retrieve()
                .body(AttributeDefinitionDto.class);
    }

    public List<AttributeDefinitionDto> findByEntity(Long entityId) {
        return restClient.get()
                .uri(BASE_PATH + "?entityId={id}", entityId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<ColumnDefinitionDto> findColumns(Long attributeId) {
        return restClient.get()
                .uri("/api/v1/columns?attributeId={id}", attributeId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public AttributeDefinitionDto create(AttributeDefinitionDto dto) {
        return restClient.post()
                .uri(BASE_PATH)
                .body(dto)
                .retrieve()
                .body(AttributeDefinitionDto.class);
    }

    public AttributeDefinitionDto update(Long id, AttributeDefinitionDto dto) {
        return restClient.put()
                .uri(BASE_PATH + "/{id}", id)
                .body(dto)
                .retrieve()
                .body(AttributeDefinitionDto.class);
    }

    public void delete(Long id) {
        restClient.delete()
                .uri(BASE_PATH + "/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }
}
