import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SprawyStore } from '../sprawy.store';
import { seweroscStanu, statusTerminu } from '../sprawy.utils';

/** Wydziały wywnioskowane z danych mockowych / słownika backendu (WydzialSymbolDictionary) -
 * brak dedykowanego endpointu słownikowego do wyboru wydziału jako filtra listy. */
const WYDZIALY = [
  'Wydział Ochrony Środowiska',
  'Wydział Organizacyjny',
  'Wydział Księgowości',
  'Wydział Komunikacji',
];

@Component({
  selector: 'app-sprawy-strona',
  imports: [
    FormsModule,
    TableModule,
    TagModule,
    ButtonModule,
    SelectModule,
    InputTextModule,
    MessageModule,
  ],
  templateUrl: './sprawy-strona.html',
  styleUrl: './sprawy-strona.css',
})
export class SprawyStrona implements OnInit {
  private readonly store = inject(SprawyStore);
  private readonly router = inject(Router);

  protected readonly sprawy = this.store.sprawy;
  protected readonly liczbaWszystkich = this.store.liczbaWszystkich;
  protected readonly rozmiarStrony = this.store.rozmiarStrony;
  protected readonly loading = this.store.loading;
  protected readonly blad = this.store.blad;
  protected readonly seweroscStanu = seweroscStanu;
  protected readonly statusTerminu = statusTerminu;

  protected readonly opcjeStanow = signal<string[]>([]);
  protected readonly opcjeWydzialow = WYDZIALY;

  protected readonly szukaj = signal('');
  protected readonly filtrStan = signal<string | null>(null);
  protected readonly filtrWydzial = signal<string | null>(null);

  // Wiązany do [first] p-table - jedyny sposób programowego powrotu paginatora na stronę 0 po
  // zmianie filtra, zgodny z tym, co pokazuje aktualnie wczytana strona (store.strona()).
  protected readonly first = signal(0);

  ngOnInit(): void {
    // Lista stanów do filtra pochodzi z definicji obiegów (nie z aktualnej, przefiltrowanej
    // strony wyników), żeby wybranie filtra nie zawężało samo siebie do jednej opcji.
    this.store.definicjeObiegow().then((definicje) => {
      const stany = new Set<string>();
      for (const definicja of definicje) {
        for (const stan of definicja.stany) {
          stany.add(stan);
        }
      }
      this.opcjeStanow.set(Array.from(stany).sort());
    });
  }

  /** PrimeNG p-table w trybie `lazy` woła to przy inicjalizacji i przy każdej zmianie
   * strony/liczby wierszy - jedyne miejsce, z którego wołamy SprawyStore.szukaj(). */
  protected naLazyLoad(event: TableLazyLoadEvent): void {
    const rozmiarStrony = event.rows ?? 20;
    const strona = rozmiarStrony > 0 ? Math.floor((event.first ?? 0) / rozmiarStrony) : 0;
    this.first.set(event.first ?? 0);
    const sort =
      typeof event.sortField === 'string' && event.sortField
        ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
        : null;
    this.store.szukaj({
      stan: this.filtrStan(),
      wydzial: this.filtrWydzial(),
      q: this.szukaj().trim() || null,
      strona,
      rozmiarStrony,
      sort,
    });
  }

  protected otworzSprawe(id: string): void {
    this.router.navigate(['/sprawy', id]);
  }

  protected nowaSprawa(): void {
    this.router.navigate(['/sprawy', 'nowa']);
  }

  /** Każda zmiana filtra wraca na stronę 0 - inaczej np. filtrowanie na stronie 3 mogłaby pokazać
   * puste wyniki, jeśli przefiltrowana lista ma mniej niż 3 strony. `[first]` na p-table jest
   * zwykłym inputem bez efektu bocznego (sprawdzone w primeng-table.mjs - `set first()` tylko
   * przypisuje pole, NIE emituje `onLazyLoad`) - więc wołamy `naLazyLoad` wprost, zamiast liczyć
   * na to, że zmiana `[first]` sama odpali zapytanie. */
  protected zastosujFiltry(): void {
    this.naLazyLoad({ first: 0, rows: this.store.rozmiarStrony() });
  }

  protected wyczyscFiltry(): void {
    this.szukaj.set('');
    this.filtrStan.set(null);
    this.filtrWydzial.set(null);
    this.zastosujFiltry();
  }
}
