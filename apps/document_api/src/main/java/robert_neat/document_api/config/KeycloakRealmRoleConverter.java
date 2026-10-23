package robert_neat.document_api.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Mapuje role realmu Keycloak (`realm_access.roles` w tokenie JWT) na uprawnienia Spring Security
 * (`ROLE_&lt;nazwa&gt;`, zgodnie z konwencją wymaganą przez hasRole/hasAnyRole). Brak lub niepoprawny
 * kształt claimu nie jest błędem - token bez żadnej roli domenowej jest poprawnym stanem (np. konto
 * techniczne), więc zwracana jest po prostu pusta kolekcja uprawnień. */
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (!(realmAccess instanceof Map<?, ?> realmAccessMap)) {
            return List.of();
        }
        Object roles = realmAccessMap.get(ROLES_CLAIM);
        if (!(roles instanceof Collection<?> rolesCollection)) {
            return List.of();
        }
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Object role : rolesCollection) {
            if (role instanceof String roleName && !roleName.isBlank()) {
                authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + roleName));
            }
        }
        return authorities;
    }
}
