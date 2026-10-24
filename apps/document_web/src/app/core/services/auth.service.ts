import { Injectable, inject, signal } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import type { Uzytkownik } from '@electronic-document-workflow/document-shared-dto';
import { mapujClaimyNaUzytkownika } from './auth-claims';

/**
 * Logowanie przez Keycloak (OIDC Authorization Code + PKCE, realm `document-workflow`,
 * klient `document-web`). `directAccessGrantsEnabled` jest wyłączone na tym kliencie,
 * więc logowanie loginem/hasłem w tej aplikacji jest niemożliwe - `zaloguj()` przekierowuje
 * do strony logowania Keycloaka.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly oauthService = inject(OAuthService);

  private readonly zalogowanyUzytkownik = signal<Uzytkownik | null>(null);

  readonly uzytkownik = this.zalogowanyUzytkownik.asReadonly();

  constructor() {
    // Zdarzenia OAuthService po logowaniu i po każdym silent/automatic refresh -
    // utrzymuje sygnał uzytkownik w zgodzie z aktualnym tokenem (nie tylko przy starcie).
    this.oauthService.events.subscribe((event) => {
      if (event.type === 'token_received' || event.type === 'token_refreshed') {
        this.odswiezUzytkownika();
      }
      if (event.type === 'logout' || event.type === 'session_terminated') {
        this.zalogowanyUzytkownik.set(null);
      }
    });
  }

  /** Odczytuje bieżące claimy i zapisuje w sygnale. Wołane z app-initializera po
   * loadDiscoveryDocumentAndTryLogin oraz przy kolejnych zdarzeniach tokenu. */
  odswiezUzytkownika(): void {
    if (!this.oauthService.hasValidAccessToken()) {
      this.zalogowanyUzytkownik.set(null);
      return;
    }
    const idTokenClaims = this.oauthService.getIdentityClaims() ?? {};
    const accessToken = this.oauthService.getAccessToken();
    this.zalogowanyUzytkownik.set(mapujClaimyNaUzytkownika(idTokenClaims, accessToken));
  }

  czyZalogowany(): boolean {
    return this.oauthService.hasValidAccessToken();
  }

  /** Przekierowuje do strony logowania Keycloaka (Authorization Code + PKCE). */
  zaloguj(): void {
    this.oauthService.initCodeFlow();
  }

  wyloguj(): void {
    this.oauthService.logOut();
  }
}
