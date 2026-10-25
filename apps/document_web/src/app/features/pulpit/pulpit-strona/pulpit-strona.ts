import { Component, computed, effect, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { TableModule } from 'primeng/table';
import { DividerModule } from 'primeng/divider';
import { MessageModule } from 'primeng/message';
import { forkJoin, catchError, of } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { SprawyApi } from '../../../core/api/sprawy.api';
import { PlatnosciApi } from '../../../core/api/platnosci.api';
import { PrzesylkiApi } from '../../../core/api/przesylki.api';
import { ObywateleApi } from '../../../core/api/obywatele.api';
import { komunikatBleduHttp } from '../../../core/api/http-error';
import { obywatelWyswietlanaNazwa } from '../../../core/models';
import type {
  NaleznoscListElement,
  Obywatel,
  Przesylka,
  Sprawa,
} from '@electronic-document-workflow/document-shared-dto';

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

const STATUSY_NALEZNOSCI_NIEROZLICZONE = new Set([
  'nierozliczona',
  'czesciowo_rozliczona',
  'przeterminowana',
]);
const STATUSY_PRZESYLEK_OCZEKUJACE = new Set(['nadane', 'awizo']);

@Component({
  selector: 'app-pulpit-strona',
  imports: [DatePipe, RouterLink, CardModule, TagModule, TableModule, DividerModule, MessageModule],
  templateUrl: './pulpit-strona.html',
  styleUrl: './pulpit-strona.css',
})
export class PulpitStrona {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly sprawyApi = inject(SprawyApi);
  private readonly platnosciApi = inject(PlatnosciApi);
  private readonly przesylkiApi = inject(PrzesylkiApi);
  private readonly obywateleApi = inject(ObywateleApi);

  protected readonly uzytkownik = computed(() => this.auth.uzytkownik());

  protected readonly wczytywanie = signal(true);
  protected readonly komunikatBledu = signal<string | null>(null);

  /** Pierwsza strona wyników GET /api/sprawy (bez filtra referent/wydzial - backend go nie
   * wspiera, filtrowanie odbywa się niżej lokalnie). rozmiarStrony=200 nie ma dziś górnego
   * limitu na backendzie; jeśli liczbaWszystkich przekroczy 200, część spraw referenta
   * mogłaby nie być widoczna - akceptowalne przy obecnej skali danych demo. */
  private readonly wszystkieSprawy = signal<Sprawa[]>([]);
  private readonly wszystkieNaleznosci = signal<NaleznoscListElement[]>([]);
  private readonly wszystkiePrzesylki = signal<Przesylka[]>([]);

  private readonly obywatelePoId = signal<Map<string, Obywatel>>(new Map());
  private readonly obywatelowePobierane = new Set<string>();

  constructor() {
    this.wczytywanie.set(true);
    forkJoin({
      sprawy: this.sprawyApi.szukaj({ rozmiarStrony: 200 }).pipe(
        catchError((err: unknown) => {
          this.komunikatBledu.set(komunikatBleduHttp(err, 'Nie udało się wczytać spraw.'));
          return of(null);
        }),
      ),
      naleznosci: this.platnosciApi.naleznosci({}).pipe(
        catchError((err: unknown) => {
          this.komunikatBledu.set(komunikatBleduHttp(err, 'Nie udało się wczytać należności.'));
          return of(null as NaleznoscListElement[] | null);
        }),
      ),
      przesylki: this.przesylkiApi.szukaj({}).pipe(
        catchError((err: unknown) => {
          this.komunikatBledu.set(komunikatBleduHttp(err, 'Nie udało się wczytać przesyłek.'));
          return of(null as Przesylka[] | null);
        }),
      ),
    }).subscribe(({ sprawy, naleznosci, przesylki }) => {
      this.wczytywanie.set(false);
      if (sprawy) this.wszystkieSprawy.set(sprawy.dane);
      if (naleznosci) this.wszystkieNaleznosci.set(naleznosci);
      if (przesylki) this.wszystkiePrzesylki.set(przesylki);
    });

    // Dociąga nazwy stron dla spraw referenta (GET /api/obywatele/:id), pojedynczo per id,
    // z pominięciem już zażądanych - unika duplikatów przy kolejnych przeliczeniach computed.
    effect(() => {
      const idy = new Set(this.mojeSprawy().map((s) => s.obywatelId));
      for (const id of idy) {
        if (this.obywatelowePobierane.has(id)) continue;
        this.obywatelowePobierane.add(id);
        this.obywateleApi
          .getById(id)
          .pipe(catchError(() => of(null as Obywatel | null)))
          .subscribe((obywatel) => {
            if (!obywatel) return;
            this.obywatelePoId.update((mapa) => {
              const nowa = new Map(mapa);
              nowa.set(id, obywatel);
              return nowa;
            });
          });
      }
    });
  }

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

  /** Wydział użytkownika nieznany z JWT (Keycloak nie udostępnia tego atrybutu, zob.
   * auth-claims.ts) - przybliżenie z wydziału pierwszej (najnowszej) sprawy referenta.
   * Do poprawienia, gdy auth zostanie uzupełniony o atrybut wydziału. */
  protected readonly przyblizonyWydzial = computed(() => this.mojeSprawy()[0]?.wydzial ?? '');

  /** Nierozliczone należności wydziału referenta (nierozliczona, częściowo rozliczona,
   * przeterminowana) - backend nie wspiera filtra multi-status ani wydzial na /api/naleznosci,
   * więc lista jest pociągana bez filtra i filtrowana lokalnie (join po sprawaId -> wydzial). */
  protected readonly nierozliczoneNaleznosci = computed(() => {
    const wydzial = this.przyblizonyWydzial();
    if (!wydzial) return [];
    const sprawyWydzialu = new Set(
      this.wszystkieSprawy()
        .filter((s) => s.wydzial === wydzial)
        .map((s) => s.id),
    );
    return this.wszystkieNaleznosci().filter(
      (n) => STATUSY_NALEZNOSCI_NIEROZLICZONE.has(n.status) && sprawyWydzialu.has(n.sprawaId),
    );
  });

  protected readonly liczbaNierozliczonychNaleznosci = computed(
    () => this.nierozliczoneNaleznosci().length,
  );

  /** Przesyłki oczekujące na zmianę statusu (nadane lub z awizo) dla wydziału referenta -
   * backend nie wspiera filtra multi-status ani wydzial na /api/przesylki, więc lista jest
   * pociągana bez filtra i filtrowana lokalnie (join po sprawaId -> wydzial). */
  protected readonly przesylkiOczekujace = computed(() => {
    const wydzial = this.przyblizonyWydzial();
    if (!wydzial) return [];
    const sprawyWydzialu = new Set(
      this.wszystkieSprawy()
        .filter((s) => s.wydzial === wydzial)
        .map((s) => s.id),
    );
    return this.wszystkiePrzesylki().filter(
      (p) => STATUSY_PRZESYLEK_OCZEKUJACE.has(p.status) && sprawyWydzialu.has(p.sprawaId),
    );
  });

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
