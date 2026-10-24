import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Subject } from 'rxjs';
import { OAuthEvent, OAuthService } from 'angular-oauth2-oidc';
import { HTTP_I_OAUTH_PROVIDERS } from './app.config';
import { environment } from '../environments/environment';

/**
 * Weryfikuje, że `HTTP_I_OAUTH_PROVIDERS` (provideHttpClient + provideOAuthClient z app.config.ts)
 * faktycznie dołącza nagłówek Authorization do żądań - to jest sedno tego cyklu: resourceServer
 * z `angular-oauth2-oidc` rejestruje interceptor przez klasyczny token DI `HTTP_INTERCEPTORS`
 * (`DefaultOAuthInterceptor`), więc bez `provideHttpClient(withInterceptorsFromDi())` Angular
 * go po cichu ignoruje - żądanie wyszłoby bez tokenu i serwer odpowiedziałby 401, bez żadnego
 * sygnału w konsoli wskazującego na przyczynę. Ten test importuje DOKŁADNIE te providery, które
 * używa aplikacja (nie powtarza konfiguracji), żeby regresja w app.config.ts została wykryta tu.
 */
describe('HTTP_I_OAUTH_PROVIDERS - dołączanie bearer tokena', () => {
  let events: Subject<OAuthEvent>;
  let accessToken: string | null;

  beforeEach(() => {
    events = new Subject<OAuthEvent>();
    accessToken = 'token-abc-123';

    const oauthStub = {
      events,
      getAccessToken: () => accessToken,
      waitForTokenInMsec: 0,
    };

    TestBed.configureTestingModule({
      providers: [
        ...HTTP_I_OAUTH_PROVIDERS,
        provideHttpClientTesting(),
        { provide: OAuthService, useValue: oauthStub },
      ],
    });
  });

  it('dołącza nagłówek Authorization do żądania na documentApiBaseUrl', () => {
    const http = TestBed.inject(HttpClient);
    const httpMock = TestBed.inject(HttpTestingController);

    http.get(`${environment.documentApiBaseUrl}/api/sprawy`).subscribe();

    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/sprawy`);
    expect(req.request.headers.get('Authorization')).toBe('Bearer token-abc-123');

    httpMock.verify();
  });

  it('NIE dołącza nagłówka Authorization do żądania poza allowedUrls', () => {
    const http = TestBed.inject(HttpClient);
    const httpMock = TestBed.inject(HttpTestingController);

    http.get('https://inny-serwis.example.com/dane').subscribe();

    const req = httpMock.expectOne('https://inny-serwis.example.com/dane');
    expect(req.request.headers.has('Authorization')).toBe(false);

    httpMock.verify();
  });
});
