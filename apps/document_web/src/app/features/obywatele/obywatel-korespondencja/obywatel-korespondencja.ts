import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { catchError, distinctUntilChanged, filter, map, of, switchMap } from 'rxjs';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { MessageModule } from 'primeng/message';
import type {
  StatusPrzesylki,
  KanalWysylki,
  Przesylka,
} from '@electronic-document-workflow/document-shared-dto';
import { PrzesylkiApi } from '../../../core/api/przesylki.api';
import { komunikatBleduHttp } from '../../../core/api/http-error';
import { KartaObywatelaKontekst } from '../karta-obywatela-kontekst.service';

type StatusWczytywania = 'ladowanie' | 'ok' | 'blad';

const ETYKIETY_STATUSU: Record<StatusPrzesylki, string> = {
  nadane: 'Nadane',
  doreczone: 'Doręczone',
  odebrane: 'Odebrane',
  zwrot: 'Zwrot',
  awizo: 'Awizo',
};

const SEVERITY_STATUSU: Record<StatusPrzesylki, 'success' | 'info' | 'warn' | 'danger'> = {
  nadane: 'info',
  doreczone: 'success',
  odebrane: 'success',
  zwrot: 'danger',
  awizo: 'warn',
};

const ETYKIETY_KANALU: Record<KanalWysylki, string> = {
  'e-doreczenia': 'e-Doręczenia',
  operator_pocztowy: 'Operator pocztowy',
  email: 'E-mail',
};

const ETYKIETY_KIERUNKU: Record<Przesylka['kierunek'], string> = {
  wychodzaca: 'Wychodząca',
  wplywajaca: 'Wpływająca',
};

const SEVERITY_KIERUNKU: Record<Przesylka['kierunek'], 'secondary' | 'info'> = {
  wychodzaca: 'secondary',
  wplywajaca: 'info',
};

const IKONY_KIERUNKU: Record<Przesylka['kierunek'], string> = {
  wychodzaca: 'pi pi-arrow-up-right',
  wplywajaca: 'pi pi-arrow-down-left',
};

/**
 * Zakładka "Korespondencja" karty obywatela - woła realne `GET /api/przesylki?obywatelId=<id>`
 * przez `PrzesylkiApi`, sterowane sygnałem obywatela z `KartaObywatelaKontekst` (ten sam wzorzec
 * reaktywnego pipeline'u co `ObywatelSprawy`).
 */
@Component({
  selector: 'app-obywatel-korespondencja',
  imports: [TableModule, TagModule, DatePipe, MessageModule],
  templateUrl: './obywatel-korespondencja.html',
  styleUrl: './obywatel-korespondencja.css',
})
export class ObywatelKorespondencja {
  private readonly kontekst = inject(KartaObywatelaKontekst);
  private readonly api = inject(PrzesylkiApi);

  private readonly status = signal<StatusWczytywania>('ladowanie');
  protected readonly wczytywanie = computed(() => this.status() === 'ladowanie');
  protected readonly komunikatBledu = signal<string | null>(null);
  protected readonly przesylki = signal<Przesylka[]>([]);

  constructor() {
    toObservable(this.kontekst.obywatel)
      .pipe(
        map((obywatel) => obywatel?.id ?? null),
        filter((id): id is string => !!id),
        distinctUntilChanged(),
        switchMap((obywatelId) => {
          this.status.set('ladowanie');
          this.komunikatBledu.set(null);
          return this.api.szukaj({ obywatelId }).pipe(
            catchError((err: unknown) => {
              this.status.set('blad');
              this.komunikatBledu.set(
                komunikatBleduHttp(err, 'Nie udało się wczytać korespondencji obywatela.'),
              );
              return of(null as Przesylka[] | null);
            }),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((wynik) => {
        if (wynik !== null) {
          this.przesylki.set(wynik);
          this.status.set('ok');
        }
      });
  }

  protected readonly etykietaStatusu = (status: StatusPrzesylki): string =>
    ETYKIETY_STATUSU[status];
  protected readonly severityStatusu = (status: StatusPrzesylki) => SEVERITY_STATUSU[status];
  protected readonly etykietaKanalu = (kanal: KanalWysylki): string => ETYKIETY_KANALU[kanal];

  protected readonly etykietaKierunku = (kierunek: Przesylka['kierunek']): string =>
    ETYKIETY_KIERUNKU[kierunek];
  protected readonly severityKierunku = (kierunek: Przesylka['kierunek']) =>
    SEVERITY_KIERUNKU[kierunek];
  protected readonly ikonaKierunku = (kierunek: Przesylka['kierunek']): string =>
    IKONY_KIERUNKU[kierunek];
}
