package it.brunasti.dbdadi.frontend.client;

import it.brunasti.dbdadi.frontend.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserManagementClient {

    private static final String BASE_PATH = "/api/v1/users";
    private final RestClient restClient;

    public List<UserDto> findAll() {
        return restClient.get().uri(BASE_PATH).retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public UserDto create(UserDto dto) {
        return restClient.post().uri(BASE_PATH).body(dto).retrieve()
                .body(UserDto.class);
    }

    public UserDto update(Long id, UserDto dto) {
        return restClient.put().uri(BASE_PATH + "/{id}", id).body(dto).retrieve()
                .body(UserDto.class);
    }

    public void delete(Long id) {
        restClient.delete().uri(BASE_PATH + "/{id}", id).retrieve().toBodilessEntity();
    }
}
