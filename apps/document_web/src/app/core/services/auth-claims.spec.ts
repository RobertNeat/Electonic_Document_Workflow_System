import { mapujClaimyNaUzytkownika, wyodrebnijRoleZAccessTokenu } from './auth-claims';

/** Buduje fałszywy JWT (base64url(header).base64url(payload).sig) - podpis nieważny,
 * wystarczy do testu samego dekodowania payloadu. */
function zbudujJwt(payload: Record<string, unknown>): string {
  const header = { alg: 'none', typ: 'JWT' };
  const koduj = (obj: unknown) =>
    btoa(JSON.stringify(obj)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${koduj(header)}.${koduj(payload)}.sig`;
}

describe('wyodrebnijRoleZAccessTokenu', () => {
  it('filtruje role Keycloaka do znanych wartości RolaUzytkownika', () => {
    const token = zbudujJwt({
      realm_access: {
        roles: [
          'referent',
          'kierownik',
          'default-roles-document-workflow',
          'offline_access',
          'uma_authorization',
        ],
      },
    });

    expect(wyodrebnijRoleZAccessTokenu(token)).toEqual(['referent', 'kierownik']);
  });

  it('zwraca pustą tablicę, gdy brak claimu realm_access', () => {
    const token = zbudujJwt({});
    expect(wyodrebnijRoleZAccessTokenu(token)).toEqual([]);
  });

  it('zwraca pustą tablicę, gdy realm_access.roles ma niepoprawny kształt', () => {
    const token = zbudujJwt({ realm_access: { roles: 'referent' } });
    expect(wyodrebnijRoleZAccessTokenu(token)).toEqual([]);
  });

  it('zwraca pustą tablicę dla niepoprawnego tokenu', () => {
    expect(wyodrebnijRoleZAccessTokenu('not-a-jwt')).toEqual([]);
  });
});

describe('mapujClaimyNaUzytkownika', () => {
  it('mapuje claimy id_tokenu i role access tokenu na Uzytkownik', () => {
    const idTokenClaims = {
      sub: 'abc-123',
      preferred_username: 'demo.ksiegowosc',
      given_name: 'Demo',
      family_name: 'Księgowość',
      email: 'demo.ksiegowosc@urzad.gov.pl',
    };
    const accessToken = zbudujJwt({
      realm_access: { roles: ['ksiegowosc', 'offline_access'] },
    });

    const uzytkownik = mapujClaimyNaUzytkownika(idTokenClaims, accessToken);

    expect(uzytkownik).toEqual({
      id: 'abc-123',
      login: 'demo.ksiegowosc',
      imie: 'Demo',
      nazwisko: 'Księgowość',
      email: 'demo.ksiegowosc@urzad.gov.pl',
      role: ['ksiegowosc'],
      wydzial: '',
      stanowisko: '',
    });
  });

  it('zwraca puste pola tekstowe dla brakujących claimów, bez wyjątku', () => {
    const uzytkownik = mapujClaimyNaUzytkownika({}, zbudujJwt({}));

    expect(uzytkownik.id).toBe('');
    expect(uzytkownik.login).toBe('');
    expect(uzytkownik.role).toEqual([]);
    expect(uzytkownik.wydzial).toBe('');
    expect(uzytkownik.stanowisko).toBe('');
  });
});
