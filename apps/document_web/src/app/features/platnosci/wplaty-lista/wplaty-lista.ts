import { Component, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { DialogModule } from 'primeng/dialog';
import { MessageModule } from 'primeng/message';
import { catchError, of } from 'rxjs';
import type { NaleznoscListElement } from '@electronic-document-workflow/document-shared-dto';
import type { Wplata } from '@electronic-document-workflow/document-shared-dto';
import { PlatnosciApi } from '../../../core/api/platnosci.api';
import { komunikatBleduHttp } from '../../../core/api/http-error';
import { PlatnosciStore } from '../data/platnosci-store';

@Component({
  selector: 'app-wplaty-lista',
  imports: [
    CurrencyPipe,
    DatePipe,
    RouterLink,
    FormsModule,
    TableModule,
    TagModule,
    ButtonModule,
    SelectModule,
    DialogModule,
    MessageModule,
  ],
  templateUrl: './wplaty-lista.html',
  styleUrl: './wplaty-lista.css',
})
export class WplatyLista {
  private readonly store = inject(PlatnosciStore);
  private readonly api = inject(PlatnosciApi);

  protected readonly wplaty = this.store.wplaty;
  protected readonly loading = this.store.loading;
  protected readonly blad = this.store.blad;

  // GET /api/naleznosci filtruje po JEDNEJ wartości status na raz - nie ma OR/multi-value.
  // Pobranie pełnej (nieprzefiltrowanej) listy i filtrowanie po stronie klienta zamiast dwóch
  // zapytań (nierozliczona + czesciowo_rozliczona) jest tu prostsze i wystarcza przy skali danych
  // demo; ta sama lista służy też do rozwiązania tytułu należności (naleznoscTytul) dla wpłat już
  // dopasowanych do należności W PEŁNI rozliczonej - przefiltrowana lista by jej nie zawierała.
  private readonly wszystkieNaleznosci = signal<NaleznoscListElement[]>([]);
  protected readonly bladNaleznosci = signal<string | null>(null);

  protected readonly naleznosciNierozliczone = computed(() =>
    this.wszystkieNaleznosci().filter(
      (n) => n.status === 'nierozliczona' || n.status === 'czesciowo_rozliczona',
    ),
  );

  protected readonly opcjeNaleznosci = computed(() =>
    this.naleznosciNierozliczone().map((n) => ({
      etykieta: `${n.numerReferencyjny} — ${n.tytul} (${n.kwota - n.kwotaRozliczona} PLN pozostało)`,
      wartosc: n.id,
    })),
  );

  protected readonly dialogWidoczny = signal(false);
  protected readonly wplataDoDopasowania = signal<Wplata | null>(null);
  protected readonly wybranaNaleznoscId = signal<string | null>(null);
  protected readonly dopasowywanie = signal(false);
  protected readonly bladDopasowania = signal<string | null>(null);

  constructor() {
    this.odswiezWplaty();
    this.odswiezNaleznosci();
  }

  private odswiezWplaty(): void {
    this.store.szukajWplat({});
  }

  private odswiezNaleznosci(): void {
    this.api
      .naleznosci({})
      .pipe(
        catchError((err: unknown) => {
          this.bladNaleznosci.set(
            komunikatBleduHttp(err, 'Nie udało się wczytać listy należności.'),
          );
          return of(null);
        }),
      )
      .subscribe((wynik) => {
        if (wynik) this.wszystkieNaleznosci.set(wynik);
      });
  }

  naleznoscTytul(id: string | undefined): string | null {
    if (!id) {
      return null;
    }
    return this.wszystkieNaleznosci().find((n) => n.id === id)?.tytul ?? null;
  }

  otworzDopasowanie(w: Wplata): void {
    this.wplataDoDopasowania.set(w);
    this.wybranaNaleznoscId.set(null);
    this.bladDopasowania.set(null);
    this.dialogWidoczny.set(true);
  }

  potwierdzDopasowanie(): void {
    const wplata = this.wplataDoDopasowania();
    const naleznoscId = this.wybranaNaleznoscId();
    if (!wplata || !naleznoscId) {
      return;
    }
    this.bladDopasowania.set(null);
    this.dopasowywanie.set(true);
    this.store.dopasujWplate(wplata.id, naleznoscId).then((wynik) => {
      this.dopasowywanie.set(false);
      if (typeof wynik === 'string') {
        this.bladDopasowania.set(wynik);
        return;
      }
      this.dialogWidoczny.set(false);
      this.wplataDoDopasowania.set(null);
      // Dopasowanie mogło zmienić status należności (np. na rozliczona) - odświeżamy obie listy,
      // żeby UI odzwierciedlał realny stan backendu, nie dane sprzed akcji.
      this.odswiezWplaty();
      this.odswiezNaleznosci();
    });
  }
}
