import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { SelectModule } from 'primeng/select';
import { DatePickerModule } from 'primeng/datepicker';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { catchError, of } from 'rxjs';
import type {
  TypOperacjiAudytu,
  WpisDziennikaZdarzen,
} from '@electronic-document-workflow/document-shared-dto';
import { AdministracjaApi } from '../../../core/api/administracja.api';
import { komunikatBleduHttp } from '../../../core/api/http-error';

// Pełna, zahardkodowana lista - nie wyliczana z wczytanych danych, bo przy małej liczbie
// wpisów dropdown byłby niekompletny. Musi być zgodna z unią TypOperacjiAudytu.
const TYPY_OPERACJI: TypOperacjiAudytu[] = [
  'Rejestracja sprawy',
  'Zmiana stanu sprawy',
  'Podpisanie dokumentu',
  'Rejestracja wpłaty',
  'Wysyłka pisma',
  'Dekretacja',
  'Zmiana danych użytkownika',
  'Wygenerowanie wezwania do zapłaty',
];

@Component({
  selector: 'app-dziennik-zdarzen',
  standalone: true,
  imports: [
    FormsModule,
    TableModule,
    SelectModule,
    DatePickerModule,
    ButtonModule,
    MessageModule,
    DatePipe,
  ],
  templateUrl: './dziennik-zdarzen.html',
  styleUrl: './dziennik-zdarzen.css',
})
export class DziennikZdarzen {
  private readonly api = inject(AdministracjaApi);

  // Numer żądania - chroni przed nadpisaniem stanu przez odpowiedź z nieaktualnego (szybko
  // zmienionego) filtra, analogicznie do wzorca w szablony-strona.
  private numerZadania = 0;

  readonly typyOperacji = TYPY_OPERACJI;

  readonly wpisy = signal<WpisDziennikaZdarzen[]>([]);
  readonly wczytywanie = signal(true);
  readonly komunikatBledu = signal<string | null>(null);

  readonly filtrTypu = signal<TypOperacjiAudytu | null>(null);
  readonly filtrZakresDat = signal<Date[] | null>(null);

  constructor() {
    this.wczytaj();
  }

  zmienFiltrTypu(typ: TypOperacjiAudytu | null): void {
    this.filtrTypu.set(typ);
    this.wczytaj();
  }

  zmienFiltrZakresDat(zakres: Date[] | null): void {
    this.filtrZakresDat.set(zakres);
    this.wczytaj();
  }

  wyczyscFiltry(): void {
    this.filtrTypu.set(null);
    this.filtrZakresDat.set(null);
    this.wczytaj();
  }

  private wczytaj(): void {
    const zakres = this.filtrZakresDat();
    const od = zakres?.[0] ?? null;
    const do_ = zakres && zakres[0] ? (zakres[1] ?? zakres[0]) : null;

    const tenZadanie = ++this.numerZadania;
    this.wczytywanie.set(true);
    this.komunikatBledu.set(null);
    this.api
      .auditLog({ typOperacji: this.filtrTypu(), od, do: do_ })
      .pipe(
        catchError((err: unknown) => {
          if (tenZadanie === this.numerZadania) {
            this.komunikatBledu.set(
              komunikatBleduHttp(err, 'Nie udało się wczytać dziennika zdarzeń.'),
            );
          }
          return of(null as WpisDziennikaZdarzen[] | null);
        }),
      )
      .subscribe((wynik) => {
        if (tenZadanie !== this.numerZadania) {
          return;
        }
        this.wczytywanie.set(false);
        if (wynik !== null) {
          this.wpisy.set(wynik);
        }
      });
  }
}
