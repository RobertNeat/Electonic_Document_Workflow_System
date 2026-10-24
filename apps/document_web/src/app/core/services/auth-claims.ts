import type {
  RolaUzytkownika,
  Uzytkownik,
} from '@electronic-document-workflow/document-shared-dto';

const ZNANE_ROLE: ReadonlySet<string> = new Set<RolaUzytkownika>([
  'administrator',
  'kancelaria',
  'referent',
  'kierownik',
  'ksiegowosc',
]);

/** Dekoduje payload JWT (base64url) bez weryfikacji podpisu - tylko odczyt claimów do
 * wypełnienia UI. id_token przechodzi walidację iss/aud/exp/nonce w angular-oauth2-oidc
 * (domyślny NullValidationHandler nie sprawdza podpisu po stronie klienta), a każde
 * wywołanie API jest i tak weryfikowane kryptograficznie przez serwer zasobów
 * (document_api, OAuth2 Resource Server). */
function dekodujPayloadJwt(token: string): Record<string, unknown> {
  const czescPayload = token.split('.')[1];
  if (!czescPayload) return {};
  const base64 = czescPayload.replace(/-/g, '+').replace(/_/g, '/');
  const dopelnienie = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
  try {
    const binarny = atob(dopelnienie);
    const bajty = Uint8Array.from(binarny, (znak) => znak.charCodeAt(0));
    const json = new TextDecoder('utf-8').decode(bajty);
    return JSON.parse(json) as Record<string, unknown>;
  } catch {
    return {};
  }
}

/** Wyciąga role realmu z accessTokenu (claim `realm_access.roles`, ta sama ścieżka co
 * KeycloakRealmRoleConverter po stronie document_api) i filtruje do znanych wartości
 * RolaUzytkownika — Keycloak dorzuca każdemu użytkownikowi też
 * `default-roles-document-workflow`, `offline_access`, `uma_authorization`, które nie są
 * poprawnymi rolami domenowymi i zepsułyby typowanie oraz filtrowanie menu. */
export function wyodrebnijRoleZAccessTokenu(accessToken: string): RolaUzytkownika[] {
  const payload = dekodujPayloadJwt(accessToken);
  const realmAccess = payload['realm_access'];
  if (typeof realmAccess !== 'object' || realmAccess === null) return [];
  const role = (realmAccess as Record<string, unknown>)['roles'];
  if (!Array.isArray(role)) return [];
  return role.filter((r): r is RolaUzytkownika => typeof r === 'string' && ZNANE_ROLE.has(r));
}

/** Mapuje claimy id_tokenu + role z access tokenu na Uzytkownik. `wydzial`/`stanowisko`
 * nie mają odpowiednika w tokenie Keycloak - puste do przyszłego uzupełnienia (np.
 * atrybut użytkownika w Keycloak lub dane z document_api), poza zakresem tego cyklu. */
export function mapujClaimyNaUzytkownika(
  idTokenClaims: Record<string, unknown>,
  accessToken: string,
): Uzytkownik {
  return {
    id: String(idTokenClaims['sub'] ?? ''),
    login: String(idTokenClaims['preferred_username'] ?? ''),
    imie: String(idTokenClaims['given_name'] ?? ''),
    nazwisko: String(idTokenClaims['family_name'] ?? ''),
    email: String(idTokenClaims['email'] ?? ''),
    role: wyodrebnijRoleZAccessTokenu(accessToken),
    wydzial: '',
    stanowisko: '',
  };
}
