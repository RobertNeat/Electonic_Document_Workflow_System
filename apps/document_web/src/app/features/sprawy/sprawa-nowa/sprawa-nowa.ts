import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { SelectModule } from 'primeng/select';
import { InputTextModule } from 'primeng/inputtext';
import { DatePickerModule } from 'primeng/datepicker';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { MessageModule } from 'primeng/message';
import { SprawyStore, formatujDateLokalna } from '../sprawy.store';
import { SprawyApi } from '../../../core/api/sprawy.api';
import { AuthService } from '../../../core/services/auth.service';
import { MOCK_OBYWATELE } from '../../../core/mock/mock-data';
import { obywatelWyswietlanaNazwa } from '../../../core/models';
import type { KategoriaArchiwalna } from '@electronic-document-workflow/document-shared-dto';

interface OpcjaJrwa {
  label: string;
  value: string;
  kategoriaArchiwalna: KategoriaArchiwalna;
}

interface OpcjaObiegu {
  label: string;
  value: string;
  stanPoczatkowy: string;
}

interface OpcjaObywatela {
  label: string;
  value: string;
}

/** Wydziały wywnioskowane z danych mockowych / słownika backendu (WydzialSymbolDictionary) -
 * brak dedykowanego endpointu słownikowego do wyboru wydziału w formularzu. */
const WYDZIALY = [
  'Wydział Ochrony Środowiska',
  'Wydział Organizacyjny',
  'Wydział Księgowości',
  'Wydział Komunikacji',
];

@Component({
  selector: 'app-sprawa-nowa',
  imports: [
    FormsModule,
    RouterLink,
    SelectModule,
    InputTextModule,
    DatePickerModule,
    ButtonModule,
    CardModule,
    MessageModule,
  ],
  templateUrl: './sprawa-nowa.html',
  styleUrl: './sprawa-nowa.css',
})
export class SprawaNowa implements OnInit {
  private readonly store = inject(SprawyStore);
  private readonly api = inject(SprawyApi);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  // Obywatel/podmiot pozostaje na danych mockowych - to wybór cross-modułowy (moduł Obywatele
  // nie jest w zakresie tego cyklu), seed demo document_api używa tych samych id (ob-1/ob-2/ob-3).
  protected readonly opcjeObywateli: OpcjaObywatela[] = MOCK_OBYWATELE.map((o) => ({
    label: obywatelWyswietlanaNazwa(o),
    value: o.id,
  }));

  protected readonly opcjeWydzialow = WYDZIALY;

  protected readonly opcjeObiegow = signal<OpcjaObiegu[]>([]);
  protected readonly opcjeJrwa = signal<OpcjaJrwa[]>([]);

  protected readonly tytul = signal('');
  protected readonly definicjaObieguId = signal<string | null>(null);
  protected readonly jrwa = signal<string | null>(null);
  protected readonly kategoriaArchiwalna = signal<KategoriaArchiwalna | null>(null);
  protected readonly obywatelId = signal<string | null>(null);
  protected readonly wydzial = signal<string | null>(null);
  protected readonly referent = signal('');
  protected readonly terminUstawowy = signal<Date | null>(null);
  protected readonly blad = signal<string | null>(null);
  protected readonly zapisywanie = signal(false);

  ngOnInit(): void {
    const uzytkownik = this.auth.uzytkownik();
    if (uzytkownik) {
      this.referent.set(`${uzytkownik.imie} ${uzytkownik.nazwisko}`.trim() || uzytkownik.login);
      // wydzial/stanowisko realnego użytkownika Keycloak nie są obecnie przenoszone w claimach
      // (zob. main_assumptions.md) - pole pozostaje wymagane do wyboru manualnego, nie jest
      // domyślnie wypełniane pustym ciągiem (backend odrzuciłby pustą wartość walidacją @NotBlank).
    }

    this.api.definicjeObiegow().subscribe({
      next: (definicje) => {
        this.opcjeObiegow.set(
          definicje.map((d) => ({ label: d.nazwa, value: d.id, stanPoczatkowy: d.stanPoczatkowy })),
        );
      },
      error: () => this.blad.set('Nie udało się wczytać definicji obiegów.'),
    });

    this.api.jrwa().subscribe({
      next: (pozycje) => {
        this.opcjeJrwa.set(
          pozycje.map((j) => ({
            label: `${j.symbol} — ${j.haslo}`,
            value: j.symbol,
            kategoriaArchiwalna: j.kategoriaArchiwalna,
          })),
        );
      },
      error: () => this.blad.set('Nie udało się wczytać pozycji JRWA.'),
    });
  }

  protected naWybranaPozycjaJrwa(symbol: string | null): void {
    this.jrwa.set(symbol);
    const pozycja = this.opcjeJrwa().find((o) => o.value === symbol);
    if (pozycja) {
      this.kategoriaArchiwalna.set(pozycja.kategoriaArchiwalna);
    }
  }

  protected async zapisz(): Promise<void> {
    if (this.zapisywanie()) {
      return; // zapobiega podwójnej rejestracji przy dwukrotnym kliknięciu
    }

    const definicjaId = this.definicjaObieguId();
    const jrwa = this.jrwa();
    const kategoria = this.kategoriaArchiwalna();
    const obywatelId = this.obywatelId();
    const wydzial = this.wydzial();

    if (
      !this.tytul().trim() ||
      !definicjaId ||
      !jrwa ||
      !kategoria ||
      !obywatelId ||
      !wydzial ||
      !this.referent().trim()
    ) {
      this.blad.set('Wypełnij wszystkie wymagane pola.');
      return;
    }

    this.zapisywanie.set(true);
    this.blad.set(null);
    try {
      const termin = this.terminUstawowy();
      const wynik = await this.store.dodajSprawe({
        tytul: this.tytul().trim(),
        jrwa,
        kategoriaArchiwalna: kategoria,
        definicjaObieguId: definicjaId,
        obywatelId,
        wydzial,
        referent: this.referent().trim(),
        terminUstawowy: termin ? formatujDateLokalna(termin) : undefined,
      });

      if (typeof wynik === 'string') {
        this.blad.set(wynik);
        return;
      }

      this.router.navigate(['/sprawy', wynik.id]);
    } finally {
      this.zapisywanie.set(false);
    }
  }
}
