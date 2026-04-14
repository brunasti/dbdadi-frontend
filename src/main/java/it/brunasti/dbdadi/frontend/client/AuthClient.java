package it.brunasti.dbdadi.frontend.client;

import it.brunasti.dbdadi.frontend.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthClient {

    private final RestClient restClient;

    /**
     * Validates credentials against the backend. Returns the UserDto (with role)
     * on success, or null if credentials are invalid.
     */
    public UserDto login(String username, String password) {
        try {
            return restClient.post()
                    .uri("/api/v1/auth/login")
                    .body(Map.of("username", username, "password", password))
                    .retrieve()
                    .body(UserDto.class);
        } catch (HttpClientErrorException.Unauthorized e) {
            return null;
        }
    }
}
