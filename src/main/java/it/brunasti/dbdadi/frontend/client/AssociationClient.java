package it.brunasti.dbdadi.frontend.client;

import it.brunasti.dbdadi.frontend.dto.AssociationDto;
import it.brunasti.dbdadi.frontend.dto.GenerateAssociationsResult;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AssociationClient {

    private static final String BASE_PATH = "/api/v1/associations";
    private final RestClient restClient;

    public List<AssociationDto> findAll() {
        return restClient.get()
                .uri(BASE_PATH)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<AssociationDto> findByEntity(Long entityId) {
        return restClient.get()
                .uri(BASE_PATH + "?entityId={id}", entityId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public AssociationDto findById(Long id) {
        return restClient.get()
                .uri(BASE_PATH + "/{id}", id)
                .retrieve()
                .body(AssociationDto.class);
    }

    public AssociationDto create(AssociationDto dto) {
        return restClient.post()
                .uri(BASE_PATH)
                .body(dto)
                .retrieve()
                .body(AssociationDto.class);
    }

    public AssociationDto update(Long id, AssociationDto dto) {
        return restClient.put()
                .uri(BASE_PATH + "/{id}", id)
                .body(dto)
                .retrieve()
                .body(AssociationDto.class);
    }

    public GenerateAssociationsResult generateFromDomain(Long domainId) {
        return restClient.post()
                .uri(BASE_PATH + "/generate-from-relations?domainId={id}", domainId)
                .retrieve()
                .body(GenerateAssociationsResult.class);
    }

    public void delete(Long id) {
        restClient.delete()
                .uri(BASE_PATH + "/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }
}
