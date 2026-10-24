package robert_neat.document_api.controller;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Rozwiązanie "osoby wykonującej operację" z JWT - wspólne dla kontrolerów, które przekazują
 * aktora do serwisu (np. do metryki sprawy albo do dziennika zdarzeń). */
final class JwtActorUtil {

    private JwtActorUtil() {
    }

    static String osobaFromJwt(Jwt jwt) {
        if (jwt == null) {
            return "system";
        }
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        if (preferredUsername != null && !preferredUsername.isBlank()) {
            return preferredUsername;
        }
        return jwt.getSubject() != null ? jwt.getSubject() : "system";
    }

    /** Role realmu Keycloak (`realm_access.roles`) wołającego, bez prefiksu "ROLE_" - do walidacji
     * domenowej w serwisach. Brak/niepoprawny claim -> pusty zbiór (fail-closed). */
    static Set<String> roleFromJwt(Jwt jwt) {
        if (jwt == null) {
            return Set.of();
        }
        Object realmAccess = jwt.getClaim("realm_access");
        if (!(realmAccess instanceof Map<?, ?> realmAccessMap)) {
            return Set.of();
        }
        Object roles = realmAccessMap.get("roles");
        if (!(roles instanceof Collection<?> rolesCollection)) {
            return Set.of();
        }
        Set<String> wynik = new LinkedHashSet<>();
        for (Object role : rolesCollection) {
            if (role instanceof String roleName && !roleName.isBlank()) {
                wynik.add(roleName);
            }
        }
        return wynik;
    }
}
