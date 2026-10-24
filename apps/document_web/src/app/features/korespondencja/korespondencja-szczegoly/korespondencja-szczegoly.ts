import { Component, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { catchError, filter, forkJoin, of, switchMap, timer } from 'rxjs';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { TimelineModule } from 'primeng/timeline';
import { MessageModule } from 'primeng/message';
import type {
  Dokument,
  Obywatel,
  Przesylka,
  Sprawa,
} from '@electronic-document-workflow/document-shared-dto';
import { obywatelWyswietlanaNazwa } from '../../../core/models';
import { komunikatBleduHttp } from '../../../core/api/http-error';
import { PrzesylkiApi } from '../../../core/api/przesylki.api';
import { SprawyApi } from '../../../core/api/sprawy.api';
import { ObywateleApi } from '../../../core/api/obywatele.api';
import { DokumentyApi } from '../../../core/api/dokumenty.api';
import {
  ETYKIETY_KANALOW,
  ETYKIETY_STATUSOW,
  IKONY_KANALOW,
  OPISY_SKUTKOW_KANALOW_WPLYWAJACA,
  OPISY_SKUTKOW_KANALOW_WYCHODZACA,
  SEWERIE_STATUSOW,
} from '../korespondencja.utils';

/** Interwał odpytywania GET /api/przesylki/:id na stronie szczegółów - status przesyłki zmienia
 * się asynchronicznie (zdarzenie RabbitMQ z delivery_mock, zob. PrzesylkaStatusuConsumer w
 * document_api), nie ma REST endpointu do zasubskrybowania zmiany, więc widok sam odświeża dane
 * co 8s. Wartość umiarkowana - wystarczająco częsta, by zmiana statusu była widoczna bez ręcznego
 * odświeżenia strony, ale nie na tyle agresywna, by zbytnio obciążać document_api. */
const INTERWAL_POLLINGU_MS = 8000;

interface DaneSzczegolow {
  przesylka: Przesylka | null;
  sprawa: Sprawa | undefined;
  dokument: Dokument | undefined;
  obywatel: Obywatel | undefined;
  status: 'ladowanie' | 'ok' | 'nieznaleziona' | 'blad';
  komunikatBledu: string | null;
}

const STAN_LADOWANIA: DaneSzczegolow = {
  przesylka: null,
  sprawa: undefined,
  dokument: undefined,
  obywatel: undefined,
  status: 'ladowanie',
  komunikatBledu: null,
};

@Component({
  selector: 'app-korespondencja-szczegoly',
  imports: [
    DatePipe,
    RouterLink,
    CardModule,
    TagModule,
    ButtonModule,
    TimelineModule,
    MessageModule,
  ],
  templateUrl: './korespondencja-szczegoly.html',
  styleUrl: './korespondencja-szczegoly.css',
})
export class KorespondencjaSzczegoly {
  private readonly api = inject(PrzesylkiApi);
  private readonly sprawyApi = inject(SprawyApi);
  private readonly obywateleApi = inject(ObywateleApi);
  private readonly dokumentyApi = inject(DokumentyApi);

  readonly id = input<string>();

  private readonly dane = signal<DaneSzczegolow>(STAN_LADOWANIA);

  protected readonly przesylka = computed(() => this.dane().przesylka);
  protected readonly sprawa = computed(() => this.dane().sprawa);
  protected readonly dokument = computed(() => this.dane().dokument);
  protected readonly obywatel = computed(() => this.dane().obywatel);
  protected readonly status = computed(() => this.dane().status);
  protected readonly komunikatBledu = computed(() => this.dane().komunikatBledu);

  protected readonly nazwaObywatela = computed(() => {
    const obywatel = this.obywatel();
    return obywatel ? obywatelWyswietlanaNazwa(obywatel) : '—';
  });

  protected readonly etykietyKanalow = ETYKIETY_KANALOW;
  protected readonly ikonyKanalow = IKONY_KANALOW;
  protected readonly etykietyStatusow = ETYKIETY_STATUSOW;
  protected readonly sewerieStatusow = SEWERIE_STATUSOW;

  // Opisy skutków prawnych kanału zależą od kierunku — wychodząca to doręczenie
  // adresatowi, wpływająca to wniesienie pisma przez nadawcę. Fallback na wariant
  // wychodzącej, gdy przesyłka jeszcze nie jest znana (bezpieczeństwo typu).
  protected readonly opisySkutkow = computed(() =>
    this.przesylka()?.kierunek === 'wplywajaca'
      ? OPISY_SKUTKOW_KANALOW_WPLYWAJACA
      : OPISY_SKUTKOW_KANALOW_WYCHODZACA,
  );

  constructor() {
    toObservable(this.id)
      .pipe(
        filter((id): id is string => !!id),
        switchMap((id) => {
          this.dane.set(STAN_LADOWANIA);
          let pierwszePobranie = true;

          // timer(0, interwał) odpytuje natychmiast, a potem co INTERWAL_POLLINGU_MS - stop przy
          // zniszczeniu komponentu (takeUntilDestroyed poniżej, poza tym wewnętrznym pipe'em).
          return timer(0, INTERWAL_POLLINGU_MS).pipe(
            switchMap(() =>
              this.api.getById(id).pipe(
                switchMap((przesylka) => {
                  // Sprawa/dokument/obywatel zależą tylko od id przesyłki, nie od statusu -
                  // dociągane raz po pierwszym pobraniu, nie odpytywane ponownie przy każdym ticku.
                  if (!pierwszePobranie) {
                    return of<DaneSzczegolow>({
                      ...this.dane(),
                      przesylka,
                      status: 'ok',
                      komunikatBledu: null,
                    });
                  }
                  pierwszePobranie = false;
                  return forkJoin({
                    sprawa: this.sprawyApi
                      .getById(przesylka.sprawaId)
                      .pipe(catchError(() => of(undefined))),
                    dokument: this.dokumentyApi
                      .getById(przesylka.dokumentId)
                      .pipe(catchError(() => of(undefined))),
                    obywatel: this.obywateleApi
                      .getById(przesylka.obywatelId)
                      .pipe(catchError(() => of(undefined))),
                  }).pipe(
                    switchMap((zalezne) =>
                      of<DaneSzczegolow>({
                        przesylka,
                        sprawa: zalezne.sprawa,
                        dokument: zalezne.dokument,
                        obywatel: zalezne.obywatel,
                        status: 'ok',
                        komunikatBledu: null,
                      }),
                    ),
                  );
                }),
                catchError((err: unknown) => {
                  // Błąd na PÓŹNIEJSZYM tiku (po udanym pierwszym wczytaniu) nie powinien zastąpić
                  // już wyświetlonych danych widokiem błędu - zachowujemy ostatni dobry stan i
                  // spróbujemy znowu za INTERWAL_POLLINGU_MS (catchError musi być W TYM WEWNĘTRZNYM
                  // pipe, nie na zewnętrznym timerze - inaczej jeden nieudany tik zabiłby polling).
                  if (!pierwszePobranie) {
                    return of(this.dane());
                  }
                  pierwszePobranie = false;
                  if (err instanceof HttpErrorResponse && err.status === 404) {
                    return of<DaneSzczegolow>({ ...STAN_LADOWANIA, status: 'nieznaleziona' });
                  }
                  return of<DaneSzczegolow>({
                    ...STAN_LADOWANIA,
                    status: 'blad',
                    komunikatBledu: komunikatBleduHttp(err, 'Nie udało się wczytać przesyłki.'),
                  });
                }),
              ),
            ),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((dane) => this.dane.set(dane));
  }
}
