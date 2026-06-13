package ru.inversion.LoaderMicexFX.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import ru.inversion.LoaderMicexFX.service.DatabaseConnectionService;

import java.util.List;

@Component
public class DbAuthenticationProvider implements AuthenticationProvider {

    private final DatabaseConnectionService databaseConnectionService;

    public DbAuthenticationProvider(DatabaseConnectionService databaseConnectionService) {
        this.databaseConnectionService = databaseConnectionService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        Object credentials = authentication.getCredentials();
        String password = credentials == null ? "" : credentials.toString();

        if (username == null || username.isBlank()) {
            throw new BadCredentialsException("Укажите имя пользователя");
        }

        try {
            databaseConnectionService.applyFromCredentials(username, password);
        } catch (Exception e) {
            throw new BadCredentialsException("Не удалось подключиться к БД: " + e.getMessage(), e);
        }

        return new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
