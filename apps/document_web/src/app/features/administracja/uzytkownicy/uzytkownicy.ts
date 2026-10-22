import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { MultiSelectModule } from 'primeng/multiselect';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { RolaUzytkownika, Uzytkownik } from '../../../core/models';
import { MOCK_UZYTKOWNICY, MOCK_WYDZIALY } from '../mock/admin-mock-data';

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

let nastepneId = 100;

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
  ],
  templateUrl: './uzytkownicy.html',
  styleUrl: './uzytkownicy.css',
})
export class Uzytkownicy {
  readonly uzytkownicy = signal<Uzytkownik[]>(MOCK_UZYTKOWNICY);
  readonly wszystkieRole = WSZYSTKIE_ROLE;
  readonly kolorRoli = KOLOR_ROLI;
  readonly wydzialyNazwy = MOCK_WYDZIALY.map((w) => w.nazwa);

  readonly widocznyDialogDodania = signal(false);
  readonly nowyUzytkownik = signal<Partial<Uzytkownik>>(this.pustyFormularz());

  etykietaRoli(rola: RolaUzytkownika): string {
    return this.wszystkieRole.find((r) => r.wartosc === rola)?.etykieta ?? rola;
  }

  otworzDialogDodania(): void {
    this.nowyUzytkownik.set(this.pustyFormularz());
    this.widocznyDialogDodania.set(true);
  }

  zamknijDialogDodania(): void {
    this.widocznyDialogDodania.set(false);
  }

  zapiszNowegoUzytkownika(): void {
    const formularz = this.nowyUzytkownik();
    if (!formularz.login || !formularz.imie || !formularz.nazwisko || !formularz.email) {
      return;
    }
    const uzytkownik: Uzytkownik = {
      id: `u-${nastepneId++}`,
      login: formularz.login,
      imie: formularz.imie,
      nazwisko: formularz.nazwisko,
      email: formularz.email,
      role: formularz.role ?? [],
      wydzial: formularz.wydzial ?? '',
      stanowisko: formularz.stanowisko ?? '',
    };
    this.uzytkownicy.update((lista) => [...lista, uzytkownik]);
    this.widocznyDialogDodania.set(false);
  }

  aktualizujRole(uzytkownik: Uzytkownik, role: RolaUzytkownika[]): void {
    this.uzytkownicy.update((lista) =>
      lista.map((u) => (u.id === uzytkownik.id ? { ...u, role } : u)),
    );
  }

  private pustyFormularz(): Partial<Uzytkownik> {
    return { login: '', imie: '', nazwisko: '', email: '', role: [], wydzial: '', stanowisko: '' };
  }
}
