import {
  ApplicationConfig,
  LOCALE_ID,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localePl from '@angular/common/locales/pl';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { providePrimeNG } from 'primeng/config';
import { AuthConfig, OAuthService, provideOAuthClient } from 'angular-oauth2-oidc';
import { routes } from './app.routes';
import { AppThemePreset } from './core/theme/app-theme.preset';
import { PRIMENG_TLUMACZENIE_PL } from './core/config/primeng-pl.translation';
import { AuthService } from './core/services/auth.service';
import { environment } from '../environments/environment';

registerLocaleData(localePl);

const authConfig: AuthConfig = {
  issuer: environment.keycloakIssuer,
  clientId: environment.keycloakClientId,
  responseType: 'code', // PKCE (S256) jest automatyczne dla responseType 'code' w tej bibliotece
  scope: 'openid profile email',
  redirectUri: window.location.origin + '/',
  // Wdrożenie jest na czystym HTTP (realm ma sslRequired: none, KEYCLOAK_PUBLIC_URL domyślnie
  // http://localhost:10681) - domyślne 'remoteOnly' odrzuciłoby issuer, gdyby nie był localhostem
  // (np. publiczny adres LAN/serwera bez TLS), więc wymuszamy brak wymogu HTTPS explicite.
  requireHttps: false,
};

/**
 * Dołączenie tokenu bearer do wywołań HTTP - `angular-oauth2-oidc`'s resourceServer rejestruje
 * `DefaultOAuthInterceptor` przez klasyczny token DI `HTTP_INTERCEPTORS` (nie standalone
 * `HttpInterceptorFn`), więc `provideHttpClient()` musi mieć `withInterceptorsFromDi()` - inaczej
 * Angular go po cichu ignoruje i żadne żądanie nie dostaje nagłówka Authorization (zweryfikowane
 * czytając fesm2022/angular-oauth2-oidc.mjs - provideOAuthClient rejestruje
 * `{ provide: HTTP_INTERCEPTORS, useClass: DefaultOAuthInterceptor, multi: true }`).
 * Eksportowane jako stała, żeby testy bearer-tokena mogły użyć TYCH SAMYCH providerów, a nie
 * powtórnie zapisanej (i potencjalnie niezgodnej) konfiguracji.
 */
export const HTTP_I_OAUTH_PROVIDERS = [
  provideHttpClient(withInterceptorsFromDi()),
  provideOAuthClient({
    resourceServer: {
      allowedUrls: [environment.documentApiBaseUrl],
      sendAccessToken: true,
    },
  }),
];

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    { provide: LOCALE_ID, useValue: 'pl' },
    ...HTTP_I_OAUTH_PROVIDERS,
    provideRouter(routes, withComponentInputBinding()),
    providePrimeNG({
      theme: {
        preset: AppThemePreset,
        options: {
          darkModeSelector: '.app-dark',
        },
      },
      translation: PRIMENG_TLUMACZENIE_PL,
    }),
    // Musi zakończyć się przed pierwszą nawigacją routera - inaczej authGuard przekierowałby
    // na /logowanie zanim parametry ?code=&state= z powrotu od Keycloaka zostałyby przetworzone
    // (pętla logowania albo zgubione logowanie).
    provideAppInitializer(async () => {
      const oauthService = inject(OAuthService);
      const auth = inject(AuthService);
      oauthService.configure(authConfig);
      try {
        await oauthService.loadDiscoveryDocumentAndTryLogin();
      } catch (err) {
        // Keycloak nieosiągalny lub discovery nieudane - aplikacja ma się uruchomić dalej,
        // authGuard odeśle użytkownika na /logowanie, a przycisk logowania po prostu zawiedzie.
        console.error('Nie udało się zainicjalizować sesji OIDC', err);
      }
      auth.odswiezUzytkownika();
      // Refresh tokenem odświeżającym (nie iframe) - brak infrastruktury pod silent-refresh.html
      // i third-party cookies; access token Keycloaka ma krótki czas życia, bez tego sesja
      // wygasałaby w trakcie korzystania z aplikacji.
      if (oauthService.hasValidAccessToken()) {
        oauthService.setupAutomaticSilentRefresh();
      }
    }),
  ],
};
