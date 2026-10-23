package robert_neat.document_api.controller;

import org.springframework.security.oauth2.jwt.Jwt;

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
}
