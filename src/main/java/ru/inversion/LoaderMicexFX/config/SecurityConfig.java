package ru.inversion.LoaderMicexFX.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import ru.inversion.LoaderMicexFX.security.DbAuthenticationProvider;
import ru.inversion.LoaderMicexFX.service.DatabaseConnectionService;
import ru.inversion.LoaderMicexFX.service.ExchangeWorkerService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final DbAuthenticationProvider dbAuthenticationProvider;
    private final DatabaseConnectionService databaseConnectionService;
    private final ExchangeWorkerService exchangeWorkerService;

    public SecurityConfig(
            DbAuthenticationProvider dbAuthenticationProvider,
            DatabaseConnectionService databaseConnectionService,
            ExchangeWorkerService exchangeWorkerService) {
        this.dbAuthenticationProvider = dbAuthenticationProvider;
        this.databaseConnectionService = databaseConnectionService;
        this.exchangeWorkerService = exchangeWorkerService;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/error").permitAll()
                        .requestMatchers("/css/**", "/img/**", "/webjars/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/loader", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .addLogoutHandler(this::onLogout)
                        .permitAll())
                .authenticationProvider(dbAuthenticationProvider);
        return http.build();
    }

    private void onLogout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {
        exchangeWorkerService.stopApi();
        databaseConnectionService.disconnect();
    }
}
