import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TabsModule } from 'primeng/tabs';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { InputNumberModule } from 'primeng/inputnumber';
import { CheckboxModule } from 'primeng/checkbox';
import { MessageModule } from 'primeng/message';
import { Observable, catchError, of } from 'rxjs';
import type {
  DostawcaWysylki,
  KategoriaArchiwalna,
  PozycjaJrwa,
  StawkaOplaty,
  TypDokumentu,
} from '@electronic-document-workflow/document-shared-dto';

type TypDostawcyWysylki = DostawcaWysylki['typ'];
import { SprawyApi } from '../../../core/api/sprawy.api';
import { AdministracjaApi } from '../../../core/api/administracja.api';
import { AuthService } from '../../../core/services/auth.service';
import { komunikatBleduHttp } from '../../../core/api/http-error';

const KATEGORIE_ARCHIWALNE: KategoriaArchiwalna[] = ['A', 'B5', 'B10', 'BE5', 'BE10'];
const TYPY_DOSTAWCY: TypDostawcyWysylki[] = ['e-doreczenia', 'operator_pocztowy', 'email'];

@Component({
  selector: 'app-slowniki',
  standalone: true,
  imports: [
    FormsModule,
    TabsModule,
    TableModule,
    TagModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    TextareaModule,
    SelectModule,
    InputNumberModule,
    CheckboxModule,
    MessageModule,
  ],
  templateUrl: './slowniki.html',
  styleUrl: './slowniki.css',
})
export class Slowniki {
  private readonly sprawyApi = inject(SprawyApi);
  private readonly api = inject(AdministracjaApi);
  private readonly auth = inject(AuthService);

  // Edycja (4x PUT) wymaga roli administrator po stronie backendu - przyciski edycji są
  // ukrywane dla innych ról, żeby nie prowokować 403 z UI.
  readonly czyAdministrator = computed(() =>
    (this.auth.uzytkownik()?.role ?? []).includes('administrator'),
  );

  readonly jrwa = signal<PozycjaJrwa[]>([]);
  readonly typyDokumentow = signal<TypDokumentu[]>([]);
  readonly dostawcyWysylki = signal<DostawcaWysylki[]>([]);
  readonly stawkiOplat = signal<StawkaOplaty[]>([]);
  readonly wczytywanie = signal(true);
  readonly komunikatBledu = signal<string | null>(null);

  readonly kategorieArchiwalne = KATEGORIE_ARCHIWALNE;
  readonly typyDostawcy = TYPY_DOSTAWCY;

  readonly widocznyDialog = signal<'jrwa' | 'typ-dokumentu' | 'dostawca' | 'stawka' | null>(null);
  readonly edycjaBlad = signal<string | null>(null);
  readonly edycjaWTrakcie = signal(false);

  readonly formularzJrwa = signal<PozycjaJrwa | null>(null);
  readonly formularzTypDokumentu = signal<TypDokumentu | null>(null);
  readonly formularzDostawca = signal<DostawcaWysylki | null>(null);
  readonly formularzStawka = signal<StawkaOplaty | null>(null);

  constructor() {
    this.wczytywanie.set(true);
    this.sprawyApi
      .jrwa()
      .pipe(catchError((err: unknown) => this.obsluzBladInicjalizacji(err)))
      .subscribe((wynik) => wynik && this.jrwa.set(wynik));
    this.sprawyApi
      .typyDokumentow()
      .pipe(catchError((err: unknown) => this.obsluzBladInicjalizacji(err)))
      .subscribe((wynik) => wynik && this.typyDokumentow.set(wynik));
    this.api
      .dostawcyWysylki()
      .pipe(catchError((err: unknown) => this.obsluzBladInicjalizacji(err)))
      .subscribe((wynik) => wynik && this.dostawcyWysylki.set(wynik));
    this.api
      .stawkiOplat()
      .pipe(catchError((err: unknown) => this.obsluzBladInicjalizacji(err)))
      .subscribe((wynik) => {
        this.wczytywanie.set(false);
        if (wynik) this.stawkiOplat.set(wynik);
      });
  }

  private obsluzBladInicjalizacji(err: unknown) {
    this.komunikatBledu.set(komunikatBleduHttp(err, 'Nie udało się wczytać słowników.'));
    return of(null);
  }

  otworzEdycjeJrwa(pozycja: PozycjaJrwa): void {
    this.formularzJrwa.set({ ...pozycja });
    this.edycjaBlad.set(null);
    this.widocznyDialog.set('jrwa');
  }

  otworzEdycjeTypuDokumentu(typ: TypDokumentu): void {
    this.formularzTypDokumentu.set({ ...typ });
    this.edycjaBlad.set(null);
    this.widocznyDialog.set('typ-dokumentu');
  }

  otworzEdycjeDostawcy(dostawca: DostawcaWysylki): void {
    this.formularzDostawca.set({ ...dostawca });
    this.edycjaBlad.set(null);
    this.widocznyDialog.set('dostawca');
  }

  otworzEdycjeStawki(stawka: StawkaOplaty): void {
    this.formularzStawka.set({ ...stawka });
    this.edycjaBlad.set(null);
    this.widocznyDialog.set('stawka');
  }

  zamknijDialog(): void {
    this.widocznyDialog.set(null);
  }

  zapiszJrwa(): void {
    const dane = this.formularzJrwa();
    if (!dane) return;
    this.wykonajZapis(
      this.sprawyApi.aktualizujJrwa(dane.symbol, dane),
      (wynik) =>
        this.jrwa.update((lista) => lista.map((p) => (p.symbol === wynik.symbol ? wynik : p))),
      'Nie udało się zapisać pozycji JRWA.',
    );
  }

  zapiszTypDokumentu(): void {
    const dane = this.formularzTypDokumentu();
    if (!dane) return;
    this.wykonajZapis(
      this.sprawyApi.aktualizujTypDokumentu(dane.id, dane),
      (wynik) =>
        this.typyDokumentow.update((lista) => lista.map((t) => (t.id === wynik.id ? wynik : t))),
      'Nie udało się zapisać typu dokumentu.',
    );
  }

  zapiszDostawce(): void {
    const dane = this.formularzDostawca();
    if (!dane) return;
    this.wykonajZapis(
      this.api.aktualizujDostawceWysylki(dane.id, dane),
      (wynik) =>
        this.dostawcyWysylki.update((lista) => lista.map((d) => (d.id === wynik.id ? wynik : d))),
      'Nie udało się zapisać dostawcy wysyłki.',
    );
  }

  zapiszStawke(): void {
    const dane = this.formularzStawka();
    if (!dane) return;
    this.wykonajZapis(
      this.api.aktualizujStawkeOplaty(dane.id, dane),
      (wynik) =>
        this.stawkiOplat.update((lista) => lista.map((s) => (s.id === wynik.id ? wynik : s))),
      'Nie udało się zapisać stawki opłaty.',
    );
  }

  private wykonajZapis<T>(
    zadanie: Observable<T>,
    naSukces: (wynik: T) => void,
    domyslnyBlad: string,
  ): void {
    this.edycjaBlad.set(null);
    this.edycjaWTrakcie.set(true);
    zadanie
      .pipe(
        catchError((err: unknown) => {
          this.edycjaBlad.set(komunikatBleduHttp(err, domyslnyBlad));
          return of(null as T | null);
        }),
      )
      .subscribe((wynik) => {
        this.edycjaWTrakcie.set(false);
        if (wynik === null) return;
        naSukces(wynik);
        this.widocznyDialog.set(null);
      });
  }
}
