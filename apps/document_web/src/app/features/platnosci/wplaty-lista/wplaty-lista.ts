import { Component, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { DialogModule } from 'primeng/dialog';
import { Wplata } from '../../../core/models';
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
  ],
  templateUrl: './wplaty-lista.html',
  styleUrl: './wplaty-lista.css',
})
export class WplatyLista {
  private readonly store = inject(PlatnosciStore);

  protected readonly wplaty = this.store.wplaty;
  protected readonly naleznosciNierozliczone = this.store.naleznosciNierozliczone;

  protected readonly dialogWidoczny = signal(false);
  protected readonly wplataDoDopasowania = signal<Wplata | null>(null);
  protected readonly wybranaNaleznoscId = signal<string | null>(null);

  protected readonly opcjeNaleznosci = computed(() =>
    this.naleznosciNierozliczone().map((n) => ({
      etykieta: `${n.numerReferencyjny} — ${n.tytul} (${n.kwota - n.kwotaRozliczona} PLN pozostało)`,
      wartosc: n.id,
    })),
  );

  naleznoscTytul(id: string | undefined): string | null {
    if (!id) {
      return null;
    }
    return this.store.naleznosc(id)?.tytul ?? null;
  }

  otworzDopasowanie(w: Wplata): void {
    this.wplataDoDopasowania.set(w);
    this.wybranaNaleznoscId.set(null);
    this.dialogWidoczny.set(true);
  }

  potwierdzDopasowanie(): void {
    const wplata = this.wplataDoDopasowania();
    const naleznoscId = this.wybranaNaleznoscId();
    if (wplata && naleznoscId) {
      this.store.dopasujRecznie(wplata.id, naleznoscId);
    }
    this.dialogWidoczny.set(false);
    this.wplataDoDopasowania.set(null);
  }
}
