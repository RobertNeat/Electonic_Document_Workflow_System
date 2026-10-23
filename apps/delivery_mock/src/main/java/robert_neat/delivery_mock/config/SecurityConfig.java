package robert_neat.delivery_mock.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/** `spring-boot-starter-oauth2-client` (dodany dla client-credentials do document_api) dociąga
 * przechodnio `spring-boot-starter-security`, które domyślnie zablokowałoby panel Thymeleaf i
 * `/actuator/health` (wymagany przez healthcheck w deploy/compose.yml) formularzem logowania.
 * `delivery_mock` NIE jest zabezpieczane - panel symulacji jest wewnętrznym narzędziem
 * obsługowym (zob. main_assumptions.md) - stąd jawny, w pełni otwarty filterChain, żeby
 * autokonfiguracja Boota nie włączyła domyślnego zabezpieczenia. CSRF wyłączone, bo formularz
 * "Symuluj zmianę statusu" nie wysyła tokenu CSRF. */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
