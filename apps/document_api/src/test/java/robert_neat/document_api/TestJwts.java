package robert_neat.document_api;

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import robert_neat.document_api.config.KeycloakRealmRoleConverter;

import java.util.List;
import java.util.Map;

/** Pomocnik testowy do mockowania JWT z rolami realmu Keycloak w testach MockMvc.
 * `SecurityMockMvcRequestPostProcessors.jwt()` nie uruchamia samo z siebie
 * {@link KeycloakRealmRoleConverter} (buduje token autoryzacji z gotowych uprawnień), więc jest on
 * wywoływany tu explicite na claimie `realm_access` - to ten sam kod produkcyjny co w
 * SecurityConfig, dzięki czemu testy weryfikują faktyczne mapowanie ról, nie tylko gotowe
 * ROLE_* podane na sztywno. */
public final class TestJwts {

    private static final KeycloakRealmRoleConverter ROLE_CONVERTER = new KeycloakRealmRoleConverter();

    private TestJwts() {
    }

    /** JWT z podanymi rolami realmu (np. "administrator", "referent"). */
    public static RequestPostProcessor jwtWithRole(String... roles) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.claim("realm_access", Map.of("roles", List.of(roles))))
                .authorities(ROLE_CONVERTER);
    }

    /** JWT uwierzytelniony, ale bez żadnej roli domenowej (brak realm_access.roles). */
    public static RequestPostProcessor jwtWithNoRoles() {
        return SecurityMockMvcRequestPostProcessors.jwt().authorities(ROLE_CONVERTER);
    }
}
