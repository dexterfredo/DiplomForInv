package ru.inversion.LoaderMicexFX.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import ru.inversion.LoaderMicexFX.service.DatabaseConnectionService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DbAuthenticationProviderTest {

    @Mock
    private DatabaseConnectionService databaseConnectionService;

    @InjectMocks
    private DbAuthenticationProvider provider;

    @Test
    void authenticate_rejectsBlankUsername() {
        var token = new UsernamePasswordAuthenticationToken("  ", "secret");
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(token));
        verifyNoInteractions(databaseConnectionService);
    }

    @Test
    void authenticate_checksCredentialsAgainstDatabase() throws Exception {
        var token = new UsernamePasswordAuthenticationToken("postgres", "secret");
        Authentication auth = provider.authenticate(token);
        verify(databaseConnectionService).applyFromCredentials("postgres", "secret");
        assertEquals("postgres", auth.getName());
    }
}
