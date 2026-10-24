import { Component, effect, inject, signal, untracked } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { DialogModule } from 'primeng/dialog';
import { MessageModule } from 'primeng/message';
import type {
  Naleznosc,
  StatusRozliczenia,
} from '@electronic-document-workflow/document-shared-dto';
import { PlatnosciStore } from '../data/platnosci-store';

interface OpcjaStatusu {
  etykieta: string;
  wartosc: StatusRozliczenia | null;
}

const ETYKIETY_STATUSOW: Record<StatusRozliczenia, string> = {
  nierozliczona: 'Nierozliczona',
  czesciowo_rozliczona: 'Częściowo rozliczona',
  rozliczona: 'Rozliczona',
  przeterminowana: 'Przeterminowana',
};

const KOLORY_STATUSOW: Record<StatusRozliczenia, 'warn' | 'info' | 'success' | 'danger'> = {
  nierozliczona: 'warn',
  czesciowo_rozliczona: 'info',
  rozliczona: 'success',
  przeterminowana: 'danger',
};

@Component({
  selector: 'app-platnosci-strona',
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
  templateUrl: './platnosci-strona.html',
  styleUrl: './platnosci-strona.css',
})
export class PlatnosciStrona {
  private readonly store = inject(PlatnosciStore);

  protected readonly etykietyStatusow = ETYKIETY_STATUSOW;
  protected readonly koloryStatusow = KOLORY_STATUSOW;

  protected readonly opcjeStatusow: OpcjaStatusu[] = [
    { etykieta: 'Wszystkie statusy', wartosc: null },
    { etykieta: ETYKIETY_STATUSOW.nierozliczona, wartosc: 'nierozliczona' },
    { etykieta: ETYKIETY_STATUSOW.czesciowo_rozliczona, wartosc: 'czesciowo_rozliczona' },
    { etykieta: ETYKIETY_STATUSOW.rozliczona, wartosc: 'rozliczona' },
    { etykieta: ETYKIETY_STATUSOW.przeterminowana, wartosc: 'przeterminowana' },
  ];

  protected readonly filtrStatusu = signal<StatusRozliczenia | null>(null);
  protected readonly wezwanieWidoczne = signal(false);
  protected readonly wezwanieNaleznosc = signal<Naleznosc | null>(null);

  protected readonly naleznosci = this.store.naleznosci;
  protected readonly loading = this.store.loading;
  protected readonly blad = this.store.blad;

  constructor() {
    // GET /api/naleznosci zwraca już zdenormalizowane znakSprawy/obywatelNazwa
    // (NaleznoscListElement) - filtrowanie po stronie backendu, bez dodatkowych wywołań API.
    effect(() => {
      const status = this.filtrStatusu();
      untracked(() => this.store.szukajNaleznosci({ status }));
    });
  }

  pozostaloDoZaplaty(n: Naleznosc): number {
    return Math.max(0, n.kwota - n.kwotaRozliczona);
  }

  otworzWezwanie(n: Naleznosc): void {
    this.wezwanieNaleznosc.set(n);
    this.wezwanieWidoczne.set(true);
  }

  potwierdzWezwanie(): void {
    // Mock — rzeczywiste wygenerowanie pisma z szablonu realizuje moduł Dokumenty.
    this.wezwanieWidoczne.set(false);
    this.wezwanieNaleznosc.set(null);
  }
}
