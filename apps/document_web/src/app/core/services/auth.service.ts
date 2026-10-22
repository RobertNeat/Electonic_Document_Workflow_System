import { Injectable, signal } from '@angular/core';
import { Uzytkownik } from '../models';
import { MOCK_UZYTKOWNIK } from '../mock/mock-data';

/**
 * Tymczasowy mock uwierzytelniania. Docelowo logowanie odbywa się przez Keycloak
 * (OIDC Authorization Code + PKCE) — formularz logowania w tej aplikacji nie może
 * zastąpić strony logowania Keycloaka, patrz plan.md.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly zalogowanyUzytkownik = signal<Uzytkownik | null>(null);

  readonly uzytkownik = this.zalogowanyUzytkownik.asReadonly();

  czyZalogowany(): boolean {
    return this.zalogowanyUzytkownik() !== null;
  }

  zaloguj(login: string, _haslo: string): boolean {
    this.zalogowanyUzytkownik.set({ ...MOCK_UZYTKOWNIK, login });
    return true;
  }

  wyloguj(): void {
    this.zalogowanyUzytkownik.set(null);
  }
}
