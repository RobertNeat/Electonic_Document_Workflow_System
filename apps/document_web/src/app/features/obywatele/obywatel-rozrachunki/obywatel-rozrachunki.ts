import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { catchError, distinctUntilChanged, filter, forkJoin, map, of, switchMap } from 'rxjs';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { MessageModule } from 'primeng/message';
import type {
  NaleznoscListElement,
  StatusRozliczenia,
  Wplata,
} from '@electronic-document-workflow/document-shared-dto';
import { PlatnosciApi } from '../../../core/api/platnosci.api';
import { komunikatBleduHttp } from '../../../core/api/http-error';
import { KartaObywatelaKontekst } from '../karta-obywatela-kontekst.service';

type StatusWczytywania = 'ladowanie' | 'ok' | 'blad';

const ETYKIETY_STATUSU: Record<StatusRozliczenia, string> = {
  nierozliczona: 'Nierozliczona',
  czesciowo_rozliczona: 'Częściowo rozliczona',
  rozliczona: 'Rozliczona',
  przeterminowana: 'Przeterminowana',
};

const SEVERITY_STATUSU: Record<StatusRozliczenia, 'success' | 'info' | 'warn' | 'danger'> = {
  nierozliczona: 'warn',
  czesciowo_rozliczona: 'info',
  rozliczona: 'success',
  przeterminowana: 'danger',
};

/**
 * Zakładka "Rozrachunki" karty obywatela - woła realne `GET /api/naleznosci?obywatelId=<id>` i
 * `GET /api/wplaty?obywatelId=<id>` przez `PlatnosciApi`, sterowane sygnałem obywatela z
 * `KartaObywatelaKontekst` (ten sam wzorzec reaktywnego pipeline'u co `ObywatelSprawy`/
 * `ObywatelKorespondencja`), ale z DWOMA równoległymi wywołaniami HTTP na zmianę id obywatela
 * (forkJoin), bo widok potrzebuje obu list naraz - wspólny stan wczytywania/błędu, bo oba zestawy
 * danych są renderowane razem w jednym widoku.
 */
@Component({
  selector: 'app-obywatel-rozrachunki',
  imports: [TableModule, TagModule, DatePipe, CurrencyPipe, MessageModule],
  templateUrl: './obywatel-rozrachunki.html',
  styleUrl: './obywatel-rozrachunki.css',
})
export class ObywatelRozrachunki {
  private readonly kontekst = inject(KartaObywatelaKontekst);
  private readonly api = inject(PlatnosciApi);

  private readonly status = signal<StatusWczytywania>('ladowanie');
  protected readonly wczytywanie = computed(() => this.status() === 'ladowanie');
  protected readonly komunikatBledu = signal<string | null>(null);
  protected readonly naleznosci = signal<NaleznoscListElement[]>([]);
  protected readonly wplaty = signal<Wplata[]>([]);

  protected readonly sumaDoZaplaty = computed(() =>
    this.naleznosci().reduce((suma, n) => suma + (n.kwota - n.kwotaRozliczona), 0),
  );

  constructor() {
    toObservable(this.kontekst.obywatel)
      .pipe(
        map((obywatel) => obywatel?.id ?? null),
        filter((id): id is string => !!id),
        distinctUntilChanged(),
        switchMap((obywatelId) => {
          this.status.set('ladowanie');
          this.komunikatBledu.set(null);
          return forkJoin({
            naleznosci: this.api.naleznosci({ obywatelId }),
            wplaty: this.api.wplaty({ obywatelId }),
          }).pipe(
            catchError((err: unknown) => {
              this.status.set('blad');
              this.komunikatBledu.set(
                komunikatBleduHttp(err, 'Nie udało się wczytać rozrachunków obywatela.'),
              );
              return of(null);
            }),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((wynik) => {
        if (wynik !== null) {
          this.naleznosci.set(wynik.naleznosci);
          this.wplaty.set(wynik.wplaty);
          this.status.set('ok');
        }
      });
  }

  protected readonly etykietaStatusu = (status: StatusRozliczenia): string =>
    ETYKIETY_STATUSU[status];
  protected readonly severityStatusu = (status: StatusRozliczenia) => SEVERITY_STATUSU[status];
}
