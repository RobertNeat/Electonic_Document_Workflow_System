import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { MultiSelectModule } from 'primeng/multiselect';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { MessageModule } from 'primeng/message';
import { catchError, firstValueFrom, of } from 'rxjs';
import type {
  RolaUzytkownika,
  Uzytkownik,
  Wydzial,
} from '@electronic-document-workflow/document-shared-dto';
import { AdministracjaApi } from '../../../core/api/administracja.api';
import { AuthService } from '../../../core/services/auth.service';
import { komunikatBleduHttp } from '../../../core/api/http-error';

interface OpcjaRoli {
  etykieta: string;
  wartosc: RolaUzytkownika;
}

const WSZYSTKIE_ROLE: OpcjaRoli[] = [
  { etykieta: 'Administrator', wartosc: 'administrator' },
  { etykieta: 'Kancelaria', wartosc: 'kancelaria' },
  { etykieta: 'Referent', wartosc: 'referent' },
  { etykieta: 'Kierownik', wartosc: 'kierownik' },
  { etykieta: 'Księgowość', wartosc: 'ksiegowosc' },
];

const KOLOR_ROLI: Record<RolaUzytkownika, 'success' | 'info' | 'warn' | 'danger' | 'secondary'> = {
  administrator: 'danger',
  kancelaria: 'info',
  referent: 'success',
  kierownik: 'warn',
  ksiegowosc: 'secondary',
};

@Component({
  selector: 'app-uzytkownicy',
  standalone: true,
  imports: [
    FormsModule,
    TableModule,
    TagModule,
    MultiSelectModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    SelectModule,
    MessageModule,
  ],
  templateUrl: './uzytkownicy.html',
  styleUrl: './uzytkownicy.css',
})
export class Uzytkownicy {
  private readonly api = inject(AdministracjaApi);
  private readonly auth = inject(AuthService);

  readonly wszystkieRole = WSZYSTKIE_ROLE;
  readonly kolorRoli = KOLOR_ROLI;

  // Strona wymaga roli administrator nawet na GET (jedyny wyjątek backendowy od "GET otwarty
  // dla każdej roli") - front sprawdza rolę przed wysłaniem żądania, nie po otrzymaniu 403.
  readonly maDostep = computed(() =>
    (this.auth.uzytkownik()?.role ?? []).includes('administrator'),
  );

  readonly uzytkownicy = signal<Uzytkownik[]>([]);
  readonly wydzialyNazwy = signal<string[]>([]);
  readonly wczytywanie = signal(true);
  readonly komunikatBledu = signal<string | null>(null);

  readonly widocznyDialogDodania = signal(false);
  readonly nowyUzytkownik = signal<Partial<Uzytkownik>>(this.pustyFormularz());
  readonly dodanieBlad = signal<string | null>(null);
  readonly dodanieWTrakcie = signal(false);

  readonly wybrani = signal<Uzytkownik[]>([]);
  readonly widocznyDialogRol = signal(false);
  readonly dodajRoleZbiorczo = signal<RolaUzytkownika[]>([]);
  readonly usunRoleZbiorczo = signal<RolaUzytkownika[]>([]);
  readonly rolZbiorczoBlad = signal<string | null>(null);
  readonly rolZbiorczoWTrakcie = signal(false);

  constructor() {
    if (!this.maDostep()) {
      this.wczytywanie.set(false);
      return;
    }
    this.wczytywanie.set(true);
    this.api
      .uzytkownicy()
      .pipe(
        catchError((err: unknown) => {
          this.komunikatBledu.set(komunikatBleduHttp(err, 'Nie udało się wczytać użytkowników.'));
          return of(null as Uzytkownik[] | null);
        }),
      )
      .subscribe((wynik) => {
        this.wczytywanie.set(false);
        if (wynik !== null) {
          this.uzytkownicy.set(wynik);
        }
      });
    this.api
      .strukturaOrganizacyjna()
      .pipe(catchError(() => of([] as Wydzial[])))
      .subscribe((wydzialy) => this.wydzialyNazwy.set(wydzialy.map((w) => w.nazwa)));
  }

  etykietaRoli(rola: RolaUzytkownika): string {
    return this.wszystkieRole.find((r) => r.wartosc === rola)?.etykieta ?? rola;
  }

  otworzDialogDodania(): void {
    this.nowyUzytkownik.set(this.pustyFormularz());
    this.dodanieBlad.set(null);
    this.widocznyDialogDodania.set(true);
  }

  zamknijDialogDodania(): void {
    this.widocznyDialogDodania.set(false);
  }

  zapiszNowegoUzytkownika(): void {
    const formularz = this.nowyUzytkownik();
    if (
      !formularz.login ||
      !formularz.imie ||
      !formularz.nazwisko ||
      !formularz.email ||
      !formularz.wydzial ||
      !formularz.stanowisko
    ) {
      this.dodanieBlad.set('Wszystkie pola są wymagane.');
      return;
    }
    this.dodanieBlad.set(null);
    this.dodanieWTrakcie.set(true);
    this.api
      .utworzUzytkownika({
        login: formularz.login,
        imie: formularz.imie,
        nazwisko: formularz.nazwisko,
        email: formularz.email,
        role: formularz.role ?? [],
        wydzial: formularz.wydzial,
        stanowisko: formularz.stanowisko,
      })
      .pipe(
        catchError((err: unknown) => {
          this.dodanieBlad.set(komunikatBleduHttp(err, 'Nie udało się utworzyć użytkownika.'));
          return of(null as Uzytkownik | null);
        }),
      )
      .subscribe((wynik) => {
        this.dodanieWTrakcie.set(false);
        if (wynik === null) {
          return;
        }
        this.uzytkownicy.update((lista) => [...lista, wynik]);
        this.widocznyDialogDodania.set(false);
      });
  }

  aktualizujRole(uzytkownik: Uzytkownik, role: RolaUzytkownika[]): void {
    if (role.length === 0) {
      // AktualizujRoleUzytkownikaRequest.role jest @NotEmpty - backend odrzuciłby pusty zestaw
      // (400), więc nie wysyłamy żądania i przywracamy poprzedni stan multiselecta.
      this.komunikatBledu.set('Użytkownik musi mieć przypisaną co najmniej jedną rolę.');
      this.uzytkownicy.update((lista) =>
        lista.map((u) => (u.id === uzytkownik.id ? { ...u, role: [...u.role] } : u)),
      );
      return;
    }
    this.api
      .aktualizujRoleUzytkownika(uzytkownik.id, role)
      .pipe(
        catchError((err: unknown) => {
          this.komunikatBledu.set(
            komunikatBleduHttp(
              err,
              `Nie udało się zaktualizować ról użytkownika ${uzytkownik.login}.`,
            ),
          );
          // Nowa referencja wiersza wymusza ponowne wyrenderowanie multiselecta do stanu
          // z serwera (bez tego widget zostałby wizualnie przy odrzuconej zmianie).
          this.uzytkownicy.update((lista) =>
            lista.map((u) => (u.id === uzytkownik.id ? { ...u } : u)),
          );
          return of(null as Uzytkownik | null);
        }),
      )
      .subscribe((wynik) => {
        if (wynik === null) {
          return;
        }
        this.uzytkownicy.update((lista) => lista.map((u) => (u.id === wynik.id ? wynik : u)));
      });
  }

  otworzDialogRol(): void {
    this.dodajRoleZbiorczo.set([]);
    this.usunRoleZbiorczo.set([]);
    this.rolZbiorczoBlad.set(null);
    this.widocznyDialogRol.set(true);
  }

  zamknijDialogRol(): void {
    this.widocznyDialogRol.set(false);
  }

  // Backend nie wystawia zbiorczej aktualizacji ról - sekwencyjne (nie równoległe) wywołania
  // PUT /{id}/role, jedno per zaznaczony użytkownik, żeby częściowe niepowodzenie było łatwe
  // do obsłużenia (zbierz wyniki, zaktualizuj tylko powodzenia, zbiorczy komunikat o resztach).
  async zastosujAktualizacjeRolZbiorczo(): Promise<void> {
    const dodaj = this.dodajRoleZbiorczo();
    const usun = new Set(this.usunRoleZbiorczo());
    const docelowi = this.wybrani();

    this.rolZbiorczoBlad.set(null);
    this.rolZbiorczoWTrakcie.set(true);

    const nieudane: string[] = [];
    for (const uzytkownik of docelowi) {
      const zRolaDodana = [...new Set([...uzytkownik.role, ...dodaj])];
      const nowaRola = zRolaDodana.filter((rola) => !usun.has(rola));
      if (nowaRola.length === 0) {
        // AktualizujRoleUzytkownikaRequest.role jest @NotEmpty - backend odrzuciłby pusty
        // zestaw, więc pomijamy to konto i liczymy je jako niepowodzenie.
        nieudane.push(uzytkownik.login);
        continue;
      }
      try {
        const zaktualizowany = await firstValueFrom(
          this.api.aktualizujRoleUzytkownika(uzytkownik.id, nowaRola),
        );
        this.uzytkownicy.update((lista) =>
          lista.map((u) => (u.id === zaktualizowany.id ? zaktualizowany : u)),
        );
      } catch {
        nieudane.push(uzytkownik.login);
      }
    }

    this.rolZbiorczoWTrakcie.set(false);
    if (nieudane.length > 0) {
      this.rolZbiorczoBlad.set(`Nie udało się zaktualizować ról dla: ${nieudane.join(', ')}.`);
      // Zostają wybrane tylko konta które się nie powiodły - ponowna próba nie wymaga
      // ręcznego zaznaczenia od nowa.
      this.wybrani.set(docelowi.filter((u) => nieudane.includes(u.login)));
      return;
    }
    this.wybrani.set([]);
    this.widocznyDialogRol.set(false);
  }

  private pustyFormularz(): Partial<Uzytkownik> {
    return { login: '', imie: '', nazwisko: '', email: '', role: [], wydzial: '', stanowisko: '' };
  }
}
