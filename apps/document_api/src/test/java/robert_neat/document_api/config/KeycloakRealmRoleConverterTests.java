package robert_neat.document_api.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testy jednostkowe (bez kontekstu Springa) weryfikujące mapowanie claimu `realm_access.roles`
 * na uprawnienia ROLE_* - w tym przypadki defensywne (brak/zły kształt claimu), które muszą
 * zwracać pustą kolekcję, a nie wyjątek. */
class KeycloakRealmRoleConverterTests {

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    @Test
    void mapujeRoleZRealmAccessNaAutorytetyZPrefiksemRole() {
        Jwt jwt = jwtBuilder()
                .claim("realm_access", Map.of("roles", List.of("administrator", "kierownik")))
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        Set<String> authorityNames = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        assertEquals(Set.of("ROLE_administrator", "ROLE_kierownik"), authorityNames);
    }

    @Test
    void zwracaPustaKolekcjeGdyBrakClaimuRealmAccess() {
        Jwt jwt = jwtBuilder().build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertTrue(authorities.isEmpty());
    }

    @Test
    void zwracaPustaKolekcjeGdyRealmAccessNieJestMapa() {
        Jwt jwt = jwtBuilder()
                .claim("realm_access", "nieprawidlowy-ksztalt")
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertTrue(authorities.isEmpty());
    }

    @Test
    void zwracaPustaKolekcjeGdyBrakListyRoles() {
        Jwt jwt = jwtBuilder()
                .claim("realm_access", Map.of("inny-klucz", "wartosc"))
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertTrue(authorities.isEmpty());
    }

    @Test
    void zwracaPustaKolekcjeGdyRolesNieJestLista() {
        Jwt jwt = jwtBuilder()
                .claim("realm_access", Map.of("roles", "administrator"))
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertTrue(authorities.isEmpty());
    }

    @Test
    void zwracaPustaKolekcjeGdyRolesPusta() {
        Jwt jwt = jwtBuilder()
                .claim("realm_access", Map.of("roles", List.of()))
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertTrue(authorities.isEmpty());
    }

    private Jwt.Builder jwtBuilder() {
        return Jwt.withTokenValue("token-value")
                .header("alg", "none")
                .claim("sub", "test-subject")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60));
    }
}
