import { TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { OAuthEvent, OAuthService } from 'angular-oauth2-oidc';
import { AuthService } from './auth.service';

function zbudujJwt(payload: Record<string, unknown>): string {
  const koduj = (obj: unknown) =>
    btoa(JSON.stringify(obj)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${koduj({ alg: 'none' })}.${koduj(payload)}.sig`;
}

describe('AuthService', () => {
  let events: Subject<OAuthEvent>;
  let oauthStub: {
    events: Subject<OAuthEvent>;
    hasValidAccessToken: () => boolean;
    getIdentityClaims: () => Record<string, unknown>;
    getAccessToken: () => string;
    initCodeFlow: ReturnType<typeof vi.fn>;
    logOut: ReturnType<typeof vi.fn>;
  };
  let validToken = false;

  beforeEach(() => {
    events = new Subject<OAuthEvent>();
    validToken = false;
    oauthStub = {
      events,
      hasValidAccessToken: () => validToken,
      getIdentityClaims: () => ({ preferred_username: 'demo.referent', sub: 'u-1' }),
      getAccessToken: () => zbudujJwt({ realm_access: { roles: ['referent'] } }),
      initCodeFlow: vi.fn(),
      logOut: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [{ provide: OAuthService, useValue: oauthStub }],
    });
  });

  it('czyZalogowany() odzwierciedla hasValidAccessToken()', () => {
    const auth = TestBed.inject(AuthService);
    expect(auth.czyZalogowany()).toBe(false);

    validToken = true;
    expect(auth.czyZalogowany()).toBe(true);
  });

  it('zaloguj() woła initCodeFlow() bez poświadczeń', () => {
    const auth = TestBed.inject(AuthService);
    auth.zaloguj();
    expect(oauthStub.initCodeFlow).toHaveBeenCalled();
  });

  it('wyloguj() woła logOut()', () => {
    const auth = TestBed.inject(AuthService);
    auth.wyloguj();
    expect(oauthStub.logOut).toHaveBeenCalled();
  });

  it('uzupełnia sygnał uzytkownik po zdarzeniu token_received', () => {
    const auth = TestBed.inject(AuthService);
    validToken = true;

    events.next({ type: 'token_received' } as OAuthEvent);

    expect(auth.uzytkownik()).toEqual({
      id: 'u-1',
      login: 'demo.referent',
      imie: '',
      nazwisko: '',
      email: '',
      role: ['referent'],
      wydzial: '',
      stanowisko: '',
    });
  });

  it('odswiezUzytkownika() wypełnia sygnał bez zdarzenia - ścieżka odświeżenia strony', () => {
    // Przy odświeżeniu strony tokeny wracają z localStorage i żadne zdarzenie OAuthService
    // (token_received itp.) się nie odpala - app-initializer woła tę metodę explicite.
    const auth = TestBed.inject(AuthService);
    validToken = true;

    auth.odswiezUzytkownika();

    expect(auth.uzytkownik()?.login).toBe('demo.referent');
  });

  it('czyści sygnał uzytkownik po zdarzeniu logout', () => {
    const auth = TestBed.inject(AuthService);
    validToken = true;
    events.next({ type: 'token_received' } as OAuthEvent);
    expect(auth.uzytkownik()).not.toBeNull();

    validToken = false;
    events.next({ type: 'logout' } as OAuthEvent);

    expect(auth.uzytkownik()).toBeNull();
  });
});
