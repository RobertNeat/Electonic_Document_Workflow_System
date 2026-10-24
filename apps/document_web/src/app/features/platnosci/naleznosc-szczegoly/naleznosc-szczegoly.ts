import { Component, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { catchError, filter, forkJoin, of, switchMap } from 'rxjs';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { ProgressBarModule } from 'primeng/progressbar';
import { MessageModule } from 'primeng/message';
import type {
  Naleznosc,
  Obywatel,
  Sprawa,
  StatusRozliczenia,
  Wplata,
} from '@electronic-document-workflow/document-shared-dto';
import { obywatelWyswietlanaNazwa } from '../../../core/models';
import { komunikatBleduHttp } from '../../../core/api/http-error';
import { PlatnosciApi } from '../../../core/api/platnosci.api';
import { SprawyApi } from '../../../core/api/sprawy.api';
import { ObywateleApi } from '../../../core/api/obywatele.api';

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

interface DaneSzczegolow {
  naleznosc: Naleznosc | null;
  wplaty: Wplata[];
  sprawa: Sprawa | undefined;
  obywatel: Obywatel | undefined;
  status: 'ladowanie' | 'ok' | 'nieznaleziona' | 'blad';
  komunikatBledu: string | null;
}

const STAN_LADOWANIA: DaneSzczegolow = {
  naleznosc: null,
  wplaty: [],
  sprawa: undefined,
  obywatel: undefined,
  status: 'ladowanie',
  komunikatBledu: null,
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
    MessageModule,
  ],
  templateUrl: './naleznosc-szczegoly.html',
  styleUrl: './naleznosc-szczegoly.css',
})
export class NaleznoscSzczegoly {
  private readonly api = inject(PlatnosciApi);
  private readonly sprawyApi = inject(SprawyApi);
  private readonly obywateleApi = inject(ObywateleApi);

  protected readonly etykietyStatusow = ETYKIETY_STATUSOW;
  protected readonly koloryStatusow = KOLORY_STATUSOW;

  readonly id = input<string>();

  private readonly dane = signal<DaneSzczegolow>(STAN_LADOWANIA);

  protected readonly naleznosc = computed(() => this.dane().naleznosc);
  protected readonly wplaty = computed(() => this.dane().wplaty);
  protected readonly status = computed(() => this.dane().status);
  protected readonly komunikatBledu = computed(() => this.dane().komunikatBledu);

  protected readonly nazwaSprawy = computed(() => this.dane().sprawa?.znakSprawy ?? '—');
  protected readonly nazwaObywatela = computed(() => {
    const obywatel = this.dane().obywatel;
    return obywatel ? obywatelWyswietlanaNazwa(obywatel) : '—';
  });

  protected readonly procentRozliczenia = computed(() => {
    const n = this.naleznosc();
    if (!n || n.kwota <= 0) {
      return 0;
    }
    return Math.min(100, Math.round((n.kwotaRozliczona / n.kwota) * 100));
  });

  constructor() {
    toObservable(this.id)
      .pipe(
        filter((id): id is string => !!id),
        switchMap((id) => {
          this.dane.set(STAN_LADOWANIA);
          return this.api.getById(id).pipe(
            switchMap((naleznosc) =>
              // Sprawa/obywatel/wpłaty zależą tylko od samej należności - dociągane dopiero po
              // potwierdzeniu, że należność istnieje, nie przed (inaczej 404 na należności mógłby
              // zgubić się za błędem jednego z zależnych wywołań).
              forkJoin({
                wplaty: this.api
                  .wplaty({ naleznoscId: id })
                  .pipe(catchError(() => of<Wplata[]>([]))),
                sprawa: this.sprawyApi
                  .getById(naleznosc.sprawaId)
                  .pipe(catchError(() => of(undefined))),
                obywatel: this.obywateleApi
                  .getById(naleznosc.obywatelId)
                  .pipe(catchError(() => of(undefined))),
              }).pipe(
                switchMap((zalezne) =>
                  of<DaneSzczegolow>({
                    naleznosc,
                    wplaty: zalezne.wplaty,
                    sprawa: zalezne.sprawa,
                    obywatel: zalezne.obywatel,
                    status: 'ok',
                    komunikatBledu: null,
                  }),
                ),
              ),
            ),
            catchError((err: unknown) => {
              if (err instanceof HttpErrorResponse && err.status === 404) {
                return of<DaneSzczegolow>({ ...STAN_LADOWANIA, status: 'nieznaleziona' });
              }
              return of<DaneSzczegolow>({
                ...STAN_LADOWANIA,
                status: 'blad',
                komunikatBledu: komunikatBleduHttp(err, 'Nie udało się wczytać należności.'),
              });
            }),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((dane) => this.dane.set(dane));
  }
}
