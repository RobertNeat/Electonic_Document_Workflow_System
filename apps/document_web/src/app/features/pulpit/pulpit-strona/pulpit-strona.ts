import { Component, computed, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { TableModule } from 'primeng/table';
import { DividerModule } from 'primeng/divider';
import { AuthService } from '../../../core/services/auth.service';
import {
  MOCK_SPRAWY,
  MOCK_NALEZNOSCI,
  MOCK_PRZESYLKI,
  MOCK_OBYWATELE,
} from '../../../core/mock/mock-data';
import { Obywatel, Sprawa, obywatelWyswietlanaNazwa } from '../../../core/models';
import { PULPIT_DODATKOWE_SPRAWY } from '../pulpit-mock';

/** Różnica w dniach między dziś (lokalna północ) a datą ISO terminu. Ujemna = przeterminowane. */
function dniDoTerminu(iso: string): number {
  const [y, m, d] = iso.split('-').map(Number);
  const termin = new Date(y, m - 1, d);
  const dzis = new Date();
  dzis.setHours(0, 0, 0, 0);
  termin.setHours(0, 0, 0, 0);
  return Math.round((termin.getTime() - dzis.getTime()) / 86_400_000);
}

interface SprawaZTerminem {
  sprawa: Sprawa;
  dniDoTerminu: number;
}

@Component({
  selector: 'app-pulpit-strona',
  imports: [DatePipe, RouterLink, CardModule, TagModule, TableModule, DividerModule],
  templateUrl: './pulpit-strona.html',
  styleUrl: './pulpit-strona.css',
})
export class PulpitStrona {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly uzytkownik = computed(() => this.auth.uzytkownik());

  private readonly wszystkieSprawy = computed<Sprawa[]>(() => [
    ...MOCK_SPRAWY,
    ...PULPIT_DODATKOWE_SPRAWY,
  ]);

  private readonly obywatelePoId = computed<Map<string, Obywatel>>(
    () => new Map(MOCK_OBYWATELE.map((o) => [o.id, o])),
  );

  /** Sprawy w toku przypisane do zalogowanego referenta (po imieniu i nazwisku). */
  protected readonly mojeSprawy = computed<Sprawa[]>(() => {
    const u = this.uzytkownik();
    if (!u) return [];
    const pelneImie = `${u.imie} ${u.nazwisko}`;
    return this.wszystkieSprawy()
      .filter((s) => s.referent === pelneImie && !s.zamknieta)
      .sort((a, b) => b.dataWszczecia.localeCompare(a.dataWszczecia));
  });

  protected readonly liczbaSprawWToku = computed(() => this.mojeSprawy().length);

  /** Sprawy referenta z terminem ustawowym, posortowane rosnąco (najpierw przeterminowane). */
  protected readonly terminyUstawowe = computed<SprawaZTerminem[]>(() =>
    this.mojeSprawy()
      .filter((s) => !!s.terminUstawowy)
      .map((s) => ({ sprawa: s, dniDoTerminu: dniDoTerminu(s.terminUstawowy!) }))
      .sort((a, b) => a.dniDoTerminu - b.dniDoTerminu),
  );

  protected readonly liczbaTerminowKrytycznych = computed(
    () => this.terminyUstawowe().filter((t) => t.dniDoTerminu <= 7).length,
  );

  /** Nierozliczone należności wydziału referenta (nierozliczona, częściowo rozliczona, przeterminowana). */
  protected readonly nierozliczoneNaleznosci = computed(() => {
    const u = this.uzytkownik();
    const sprawyWydzialu = new Set(
      this.wszystkieSprawy()
        .filter((s) => !u || s.wydzial === u.wydzial)
        .map((s) => s.id),
    );
    return MOCK_NALEZNOSCI.filter(
      (n) => n.status !== 'rozliczona' && sprawyWydzialu.has(n.sprawaId),
    );
  });

  protected readonly liczbaNierozliczonychNaleznosci = computed(
    () => this.nierozliczoneNaleznosci().length,
  );

  /** Przesyłki oczekujące na zmianę statusu (nadane lub z awizo). */
  protected readonly przesylkiOczekujace = computed(() =>
    MOCK_PRZESYLKI.filter((p) => p.status === 'nadane' || p.status === 'awizo'),
  );

  protected readonly liczbaPrzesylekOczekujacych = computed(
    () => this.przesylkiOczekujace().length,
  );

  protected otworzSprawe(sprawaId: string): void {
    this.router.navigate(['/sprawy', sprawaId]);
  }

  protected nazwaObywatela(obywatelId: string): string {
    const obywatel = this.obywatelePoId().get(obywatelId);
    return obywatel ? obywatelWyswietlanaNazwa(obywatel) : '—';
  }

  protected severityTerminu(dni: number): 'danger' | 'warn' | 'success' {
    if (dni < 0) return 'danger';
    if (dni <= 7) return 'warn';
    return 'success';
  }

  protected etykietaTerminu(dni: number): string {
    if (dni < 0) return `Przeterminowane (${Math.abs(dni)} dni)`;
    if (dni === 0) return 'Dziś';
    return `${dni} dni`;
  }
}
