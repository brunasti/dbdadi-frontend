package it.brunasti.dbdadi.frontend.security;

import it.brunasti.dbdadi.frontend.client.AuthClient;
import it.brunasti.dbdadi.frontend.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BackendAuthenticationProvider implements AuthenticationProvider {

    private final AuthClient authClient;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        UserDto user;
        try {
            user = authClient.login(username, password);
        } catch (Exception e) {
            log.error("Error contacting backend auth endpoint", e);
            throw new BadCredentialsException("Authentication service unavailable");
        }

        if (user == null) {
            throw new BadCredentialsException("Invalid username or password");
        }
        if (!user.isEnabled()) {
            throw new DisabledException("User account is disabled");
        }

        String springRole = "ROLE_" + user.getRole().name();
        return new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority(springRole)));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
