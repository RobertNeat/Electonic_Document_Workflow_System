package robert_neat.document_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/** Autoryzacja oparta na rolach realmu Keycloak (`administrator`, `kancelaria`, `referent`,
 * `kierownik`, `ksiegowosc`), mapowanych na uprawnienia Spring Security przez
 * {@link KeycloakRealmRoleConverter}. Odczyty (GET) są otwarte dla każdego uwierzytelnionego
 * użytkownika niezależnie od roli - role w tym systemie rozstrzygają o tym, kto może wykonywać
 * akcje mutujące, nie o tym, kto może widzieć dane (zob. main_assumptions.md). Reguły mutujące są
 * scentralizowane tutaj (requestMatchers) zamiast rozproszone po kontrolerach jako @PreAuthorize,
 * dla łatwiejszego audytu całej macierzy uprawnień w jednym miejscu. */
@Configuration
public class SecurityConfig {

    @Value("${cors.allowed-origins}")
    private String corsAllowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                        // sprawy: rejestracja i zmiana stanu - obsługa spraw i nadzór nad nimi.
                        .requestMatchers(HttpMethod.POST, "/api/sprawy")
                            .hasAnyRole("referent", "kierownik", "administrator")
                        .requestMatchers(HttpMethod.PATCH, "/api/sprawy/{id}/stan")
                            .hasAnyRole("referent", "kierownik", "administrator")

                        // dokumenty: podpisanie PAdES - ta sama populacja co obsługa spraw/wysyłka
                        // pism (podpisany dokument jest warunkiem wysyłki, zob. AkcjaZbiorowaProcessingService).
                        .requestMatchers(HttpMethod.POST, "/api/dokumenty/{id}/podpisz")
                            .hasAnyRole("referent", "kierownik", "administrator")

                        // definicje obiegów: konfiguracja procesu jest funkcją administracyjną.
                        .requestMatchers(HttpMethod.PUT, "/api/definicje-obiegow/{id}")
                            .hasRole("administrator")

                        // korespondencja: nadanie pisma - domena kancelarii (obsługa wpływu/wypływu),
                        // wspierana przez referentów/kierowników prowadzących sprawę, z którą
                        // przesyłka jest związana. Aktualizacja statusu nie jest tu endpointem REST -
                        // odbywa się asynchronicznie, odbiorem zdarzenia RabbitMQ z delivery_mock
                        // (PrzesylkaStatusuConsumer), bez udziału Spring Security.
                        .requestMatchers(HttpMethod.POST, "/api/przesylki")
                            .hasAnyRole("kancelaria", "referent", "kierownik", "administrator")

                        // płatności: księgowość (+ administrator) rozlicza należności i wpłaty.
                        .requestMatchers(HttpMethod.POST, "/api/wplaty/{id}/dopasuj")
                            .hasAnyRole("ksiegowosc", "administrator")
                        .requestMatchers(HttpMethod.POST, "/api/import-camt053")
                            .hasAnyRole("ksiegowosc", "administrator")
                        .requestMatchers(HttpMethod.POST, "/api/naleznosci/{id}/wezwanie")
                            .hasAnyRole("ksiegowosc", "administrator")

                        // wymiana: import komunikatów sprawy (referent/kierownik, jak przy sprawach);
                        // import faktur UBL dodatkowo otwarty dla księgowości (to jej dokument kosztowy).
                        .requestMatchers(HttpMethod.POST, "/api/wymiana/xml")
                            .hasAnyRole("referent", "kierownik", "administrator")
                        .requestMatchers(HttpMethod.POST, "/api/wymiana/ubl")
                            .hasAnyRole("referent", "kierownik", "ksiegowosc", "administrator")

                        // akcje zbiorowe: ta sama populacja co mutacje spraw - akcja uruchamia
                        // mutacje na sprawach/obywatelach/użytkownikach.
                        .requestMatchers(HttpMethod.POST, "/api/akcje-zbiorowe")
                            .hasAnyRole("referent", "kierownik", "administrator")

                        // użytkownicy: zarządzanie kontami/rolami domenowymi - wyłącznie administrator,
                        // również odczyt (lista użytkowników to dane administracyjne konta, nie dane
                        // biznesowe sprawy/obywatela objęte ogólną zasadą otwartych odczytów powyżej).
                        .requestMatchers("/api/uzytkownicy", "/api/uzytkownicy/**")
                            .hasRole("administrator")

                        // słowniki administracyjne: edycja JRWA/typów dokumentów/dostawców/stawek.
                        .requestMatchers(HttpMethod.PUT, "/api/slowniki/jrwa/{symbol}")
                            .hasRole("administrator")
                        .requestMatchers(HttpMethod.PUT, "/api/slowniki/typy-dokumentow/{id}")
                            .hasRole("administrator")
                        .requestMatchers(HttpMethod.PUT, "/api/slowniki/dostawcy-wysylki/{id}")
                            .hasRole("administrator")
                        .requestMatchers(HttpMethod.PUT, "/api/slowniki/stawki-oplat/{id}")
                            .hasRole("administrator")

                        // szablony Jasper: zarządzanie szablonami to konfiguracja systemowa, nie
                        // operacja biznesowa - wyłącznie administrator.
                        .requestMatchers(HttpMethod.POST, "/api/szablony")
                            .hasRole("administrator")

                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }

    /** CORS dla originów przeglądarki document_web (dev `ng serve` i build serwowany przez
     * kontener) - bez credentials (sesja oparta na bearer tokenie, nie cookie), stąd
     * allowCredentials(false). Musi być w filter chain (nie WebMvcConfigurer.addCorsMappings),
     * żeby odpowiedź na preflight OPTIONS dostała nagłówki CORS zanim oauth2ResourceServer
     * zdążyłby odrzucić żądanie jako nieuwierzytelnione. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(corsAllowedOrigins.split(",")).stream().map(String::trim).toList());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
