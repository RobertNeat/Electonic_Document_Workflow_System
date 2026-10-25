import { Component, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { catchError, filter, forkJoin, of, switchMap } from 'rxjs';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { TimelineModule } from 'primeng/timeline';
import { MessageModule } from 'primeng/message';
import type {
  Dokument,
  Obywatel,
  Sprawa,
  WpisMetryki,
} from '@electronic-document-workflow/document-shared-dto';
import { komunikatBleduHttp } from '../../../core/api/http-error';
import { SprawyApi } from '../../../core/api/sprawy.api';
import { przejsciaZeStanu, seweroscStanu, statusTerminu } from '../sprawy.utils';
import { CitizenContextService } from '../../../core/services/citizen-context.service';
import { AuthService } from '../../../core/services/auth.service';
import { obywatelWyswietlanaNazwa } from '../../../core/models';
import { environment } from '../../../../environments/environment';

interface DaneSzczegolow {
  sprawa: Sprawa | null;
  obywatel: Obywatel | undefined;
  dokumenty: Pick<Dokument, 'typDokumentuId'>[];
  metryka: WpisMetryki[];
  status: 'ladowanie' | 'ok' | 'nieznaleziona' | 'blad';
  komunikatBledu: string | null;
}

const STAN_LADOWANIA: DaneSzczegolow = {
  sprawa: null,
  obywatel: undefined,
  dokumenty: [],
  metryka: [],
  status: 'ladowanie',
  komunikatBledu: null,
};

@Component({
  selector: 'app-sprawa-szczegoly',
  imports: [RouterLink, CardModule, TagModule, ButtonModule, TimelineModule, MessageModule],
  templateUrl: './sprawa-szczegoly.html',
  styleUrl: './sprawa-szczegoly.css',
})
export class SprawaSzczegoly {
  private readonly api = inject(SprawyApi);
  private readonly http = inject(HttpClient);
  private readonly citizenContext = inject(CitizenContextService);
  private readonly auth = inject(AuthService);
  private readonly baseUrl = environment.documentApiBaseUrl;

  readonly id = input<string>();

  private readonly dane = signal<DaneSzczegolow>(STAN_LADOWANIA);

  protected readonly sprawa = computed(() => this.dane().sprawa);
  protected readonly status = computed(() => this.dane().status);
  protected readonly komunikatBledu = computed(() => this.dane().komunikatBledu);
  protected readonly metryka = computed(() => this.dane().metryka);
  protected readonly nazwaObywatela = computed(() => {
    const obywatel = this.dane().obywatel;
    return obywatel ? obywatelWyswietlanaNazwa(obywatel) : '—';
  });

  // Wczytywane raz, niezależnie od sprawy - lookup po definicjaObieguId robi `definicjaObiegu`.
  // catchError jest konieczny - toSignal domyślnie (rejectErrors: false) RETHROWUJE błąd źródła
  // przy każdym odczycie sygnału, co zepsułoby cały widok (definicjaObiegu/mozliwePrzejscia czytają
  // ten sygnał w template) przy jednym nieudanym żądaniu GET /api/definicje-obiegow.
  private readonly definicjeObiegow = toSignal(
    this.api.definicjeObiegow().pipe(catchError(() => of([]))),
    { initialValue: [] },
  );

  protected readonly definicjaObiegu = computed(() => {
    const sprawa = this.sprawa();
    return sprawa
      ? this.definicjeObiegow().find((d) => d.id === sprawa.definicjaObieguId)
      : undefined;
  });

  protected readonly indeksAktualnegoStanu = computed(() => {
    const sprawa = this.sprawa();
    const definicja = this.definicjaObiegu();
    if (!sprawa || !definicja) return -1;
    return definicja.stany.indexOf(sprawa.stan);
  });

  // `typyDokumentow` tylko do czytelnej nazwy w komunikacie blokady - patrz komentarz przy
  // `definicjeObiegow` dla uzasadnienia catchError przy toSignal.
  private readonly typyDokumentow = toSignal(
    this.api.typyDokumentow().pipe(catchError(() => of([]))),
    { initialValue: [] },
  );

  /** UWAGA (zgłoszone, nie naprawione tu): backend `SprawaService.zmienStan` NIE sprawdza
   * `wymaganaRola`/`wymaganeTypyDokumentow` - ta blokada jest tylko UX po stronie frontendu,
   * serwer przyjmie każde dozwolone-wg-stanów przejście niezależnie od roli/dokumentów
   * wołającego. */
  protected readonly mozliwePrzejscia = computed(() => {
    const sprawa = this.sprawa();
    const definicja = this.definicjaObiegu();
    const uzytkownik = this.auth.uzytkownik();
    if (!sprawa || !definicja || !uzytkownik) return [];
    return przejsciaZeStanu(
      definicja,
      sprawa.stan,
      uzytkownik,
      this.dane().dokumenty,
      this.typyDokumentow(),
    );
  });

  protected readonly zmianaStanuWToku = signal(false);
  protected readonly bladZmianyStanu = signal<string | null>(null);

  protected readonly seweroscStanu = seweroscStanu;
  protected readonly statusTerminu = statusTerminu;

  constructor() {
    toObservable(this.id)
      .pipe(
        filter((id): id is string => !!id),
        switchMap((id) => {
          this.dane.set(STAN_LADOWANIA);
          return this.api.getById(id).pipe(
            switchMap((sprawa) =>
              forkJoin({
                obywatel: this.http
                  .get<Obywatel>(`${this.baseUrl}/api/obywatele/${sprawa.obywatelId}`)
                  .pipe(catchError(() => of(undefined))),
                dokumenty: this.http
                  .get<Pick<Dokument, 'typDokumentuId'>[]>(`${this.baseUrl}/api/dokumenty`, {
                    params: { sprawaId: sprawa.id },
                  })
                  .pipe(catchError(() => of([]))),
                metryka: this.api.metryka(sprawa.id).pipe(catchError(() => of([]))),
              }).pipe(
                switchMap((zalezne) =>
                  of<DaneSzczegolow>({
                    sprawa,
                    obywatel: zalezne.obywatel,
                    dokumenty: zalezne.dokumenty,
                    metryka: zalezne.metryka,
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
                komunikatBledu: komunikatBleduHttp(err, 'Nie udało się wczytać sprawy.'),
              });
            }),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((dane) => this.dane.set(dane));
  }

  protected przejdzDoStanu(doStanu: string): void {
    const sprawa = this.sprawa();
    if (!sprawa || this.zmianaStanuWToku()) return;

    this.zmianaStanuWToku.set(true);
    this.bladZmianyStanu.set(null);
    this.api.zmienStan(sprawa.id, doStanu).subscribe({
      next: (nowaSprawa) => {
        this.dane.update((d) => ({ ...d, sprawa: nowaSprawa }));
        this.api.metryka(sprawa.id).subscribe({
          next: (metryka) => this.dane.update((d) => ({ ...d, metryka })),
          // Zmiana stanu już się powiodła - odświeżenie metryki jest tylko dociąganiem najnowszego
          // wpisu; błąd tutaj nie powinien przesłonić sukcesu ani wyciekać jako unhandled rejection.
          error: () => {},
        });
        this.zmianaStanuWToku.set(false);
      },
      error: (err: unknown) => {
        this.bladZmianyStanu.set(komunikatBleduHttp(err, 'Nie udało się zmienić stanu sprawy.'));
        this.zmianaStanuWToku.set(false);
      },
    });
  }

  protected przypnijObywatela(): void {
    const obywatel = this.dane().obywatel;
    if (obywatel) {
      this.citizenContext.przypnij(obywatel);
    }
  }
}
