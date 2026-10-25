import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { ProgressBarModule } from 'primeng/progressbar';
import { TagModule } from 'primeng/tag';
import { MessageModule } from 'primeng/message';
import { Subscription, catchError, of, switchMap, takeWhile, timer } from 'rxjs';
import type {
  KanalWysylki,
  Sprawa,
  StatusAkcjiZbiorowej,
  SzablonJasper,
  TypAkcjiZbiorowej,
  UruchomAkcjeZbiorowaRequest,
} from '@electronic-document-workflow/document-shared-dto';
import { AkcjeZbioroweApi } from '../../../core/api/akcje-zbiorowe.api';
import { SprawyApi } from '../../../core/api/sprawy.api';
import { DokumentyApi } from '../../../core/api/dokumenty.api';
import { komunikatBleduHttp } from '../../../core/api/http-error';

type TypAkcjiUi = Extract<
  TypAkcjiZbiorowej,
  'ZMIANA_STANU' | 'WYSYLKA_PISM' | 'GENEROWANIE_DOKUMENTOW'
>;

/** Interwał odpytywania GET /api/akcje-zbiorowe/:id/status - operacja aktywnie śledzona przez
 * użytkownika na tej samej stronie (w przeciwieństwie do pollingu statusu przesyłki w tle), stąd
 * krótszy interwał niż 8s używane w KorespondencjaSzczegoly. */
const INTERWAL_POLLINGU_MS = 1200;

const KANALY_WYSYLKI: { etykieta: string; wartosc: KanalWysylki }[] = [
  { etykieta: 'e-Doręczenia', wartosc: 'e-doreczenia' },
  { etykieta: 'Operator pocztowy', wartosc: 'operator_pocztowy' },
  { etykieta: 'E-mail', wartosc: 'email' },
];

interface PrzebiegAkcji {
  uruchamianie: boolean;
  status: StatusAkcjiZbiorowej | null;
  blad: string | null;
}

function pustyPrzebieg(): PrzebiegAkcji {
  return { uruchamianie: false, status: null, blad: null };
}

@Component({
  selector: 'app-akcje-zbiorowe',
  imports: [
    FormsModule,
    TableModule,
    CardModule,
    ButtonModule,
    SelectModule,
    ProgressBarModule,
    TagModule,
    MessageModule,
  ],
  templateUrl: './akcje-zbiorowe.html',
  styleUrl: './akcje-zbiorowe.css',
})
export class AkcjeZbiorowe {
  private readonly api = inject(AkcjeZbioroweApi);
  private readonly sprawyApi = inject(SprawyApi);
  private readonly dokumentyApi = inject(DokumentyApi);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);

  private readonly pollingi = new Map<TypAkcjiUi, Subscription>();

  protected readonly sprawy = signal<Sprawa[]>([]);
  protected readonly szablony = signal<SzablonJasper[]>([]);
  protected readonly kanaly = KANALY_WYSYLKI;
  protected readonly stany = [
    'Zarejestrowana',
    'W toku',
    'Do akceptacji',
    'Zaakceptowana',
    'Zakończona',
    'Zamknięta',
  ];

  protected readonly wybraneZmianaStanu = signal<Sprawa[]>([]);
  protected readonly wybraneWysylka = signal<Sprawa[]>([]);
  protected readonly wybraneGenerowanie = signal<Sprawa[]>([]);

  protected nowyStan: string | null = null;
  protected kanalWysylki: KanalWysylki | null = null;
  protected szablonId: string | null = null;

  // Aplikacja jest zoneless - stan przebiegu musi być signalem, by @if/interpolacje się odświeżały.
  protected readonly przebiegi = signal<Record<TypAkcjiUi, PrzebiegAkcji>>({
    ZMIANA_STANU: pustyPrzebieg(),
    WYSYLKA_PISM: pustyPrzebieg(),
    GENEROWANIE_DOKUMENTOW: pustyPrzebieg(),
  });

  constructor() {
    this.sprawyApi
      .szukaj({ rozmiarStrony: 200 })
      .pipe(catchError(() => of({ dane: [], strona: 0, rozmiarStrony: 0, liczbaWszystkich: 0 })))
      .subscribe((strona) => this.sprawy.set(strona.dane));

    this.dokumentyApi
      .szablony()
      .pipe(catchError(() => of([] as SzablonJasper[])))
      .subscribe((szablony) => this.szablony.set(szablony));

    this.destroyRef.onDestroy(() => {
      for (const subskrypcja of this.pollingi.values()) {
        subskrypcja.unsubscribe();
      }
      this.pollingi.clear();
    });
  }

  protected znakSprawy(idCelu: string): string {
    return this.sprawy().find((s) => s.id === idCelu)?.znakSprawy ?? idCelu;
  }

  protected uruchomZmianeStanu(): void {
    if (this.wybraneZmianaStanu().length === 0 || !this.nowyStan) {
      return;
    }
    const request: UruchomAkcjeZbiorowaRequest<'ZMIANA_STANU'> = {
      typAkcji: 'ZMIANA_STANU',
      typCelu: 'SPRAWA',
      idCelow: this.wybraneZmianaStanu().map((s) => s.id),
      parametry: { nowyStan: this.nowyStan },
    };
    this.uruchomAkcje('ZMIANA_STANU', request);
  }

  protected uruchomWysylke(): void {
    if (this.wybraneWysylka().length === 0 || !this.kanalWysylki) {
      return;
    }
    const request: UruchomAkcjeZbiorowaRequest<'WYSYLKA_PISM'> = {
      typAkcji: 'WYSYLKA_PISM',
      typCelu: 'SPRAWA',
      idCelow: this.wybraneWysylka().map((s) => s.id),
      parametry: { kanal: this.kanalWysylki },
    };
    this.uruchomAkcje('WYSYLKA_PISM', request);
  }

  protected uruchomGenerowanie(): void {
    if (this.wybraneGenerowanie().length === 0 || !this.szablonId) {
      return;
    }
    const request: UruchomAkcjeZbiorowaRequest<'GENEROWANIE_DOKUMENTOW'> = {
      typAkcji: 'GENEROWANIE_DOKUMENTOW',
      typCelu: 'SPRAWA',
      idCelow: this.wybraneGenerowanie().map((s) => s.id),
      parametry: { szablonId: this.szablonId },
    };
    this.uruchomAkcje('GENEROWANIE_DOKUMENTOW', request);
  }

  private uruchomAkcje(typ: TypAkcjiUi, request: UruchomAkcjeZbiorowaRequest): void {
    this.pollingi.get(typ)?.unsubscribe();

    this.przebiegi.update((aktualne) => ({
      ...aktualne,
      [typ]: { uruchamianie: true, status: null, blad: null },
    }));

    this.api
      .uruchom(request)
      .pipe(
        catchError((err: unknown) => {
          this.przebiegi.update((aktualne) => ({
            ...aktualne,
            [typ]: {
              uruchamianie: false,
              status: null,
              blad: komunikatBleduHttp(err, 'Nie udało się uruchomić akcji zbiorowej.'),
            },
          }));
          return of(null);
        }),
      )
      .subscribe((odpowiedz) => {
        if (odpowiedz === null) {
          return;
        }
        this.przebiegi.update((aktualne) => ({
          ...aktualne,
          [typ]: { uruchamianie: false, status: null, blad: null },
        }));
        this.rozpocznijPolling(typ, odpowiedz.akcjaId);
      });
  }

  private rozpocznijPolling(typ: TypAkcjiUi, akcjaId: string): void {
    const subskrypcja = timer(0, INTERWAL_POLLINGU_MS)
      .pipe(
        switchMap(() =>
          this.api.status(akcjaId).pipe(catchError(() => of(null as StatusAkcjiZbiorowej | null))),
        ),
        takeWhile((status) => status === null || status.status === 'trwajaca', true),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((status) => {
        if (status === null) {
          return;
        }
        this.przebiegi.update((aktualne) => ({
          ...aktualne,
          [typ]: { uruchamianie: false, status, blad: null },
        }));
      });
    this.pollingi.set(typ, subskrypcja);
  }

  protected wrocDoPrzegladu(): void {
    this.router.navigate(['/wymiana']);
  }
}
