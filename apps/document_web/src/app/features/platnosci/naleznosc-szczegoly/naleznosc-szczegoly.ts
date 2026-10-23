import { Component, computed, inject, input } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { ProgressBarModule } from 'primeng/progressbar';
import { StatusRozliczenia } from '../../../core/models';
import { PlatnosciStore } from '../data/platnosci-store';

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
  selector: 'app-naleznosc-szczegoly',
  imports: [
    CurrencyPipe,
    DatePipe,
    RouterLink,
    TableModule,
    TagModule,
    ButtonModule,
    ProgressBarModule,
  ],
  templateUrl: './naleznosc-szczegoly.html',
  styleUrl: './naleznosc-szczegoly.css',
})
export class NaleznoscSzczegoly {
  private readonly store = inject(PlatnosciStore);

  protected readonly etykietyStatusow = ETYKIETY_STATUSOW;
  protected readonly koloryStatusow = KOLORY_STATUSOW;

  readonly id = input<string>();

  // teraz reaktywne: withComponentInputBinding() aktualizuje id() przy każdej nawigacji,
  // wcześniej id był odczytywany jednorazowo z route.snapshot (brak reaktywności na zmianę trasy).
  protected readonly naleznosc = computed(() => this.store.naleznosc(this.id() ?? ''));
  protected readonly wplaty = computed(() => this.store.wplatyDlaNaleznosci(this.id() ?? ''));

  protected readonly procentRozliczenia = computed(() => {
    const n = this.naleznosc();
    if (!n || n.kwota <= 0) {
      return 0;
    }
    return Math.min(100, Math.round((n.kwotaRozliczona / n.kwota) * 100));
  });
}
