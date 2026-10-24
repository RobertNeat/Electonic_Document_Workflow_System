import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { catchError, distinctUntilChanged, filter, map, of, switchMap } from 'rxjs';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { MessageModule } from 'primeng/message';
import type { Sprawa } from '@electronic-document-workflow/document-shared-dto';
import { SprawyApi } from '../../../core/api/sprawy.api';
import { komunikatBleduHttp } from '../../../core/api/http-error';
import { KartaObywatelaKontekst } from '../karta-obywatela-kontekst.service';

type StatusWczytywania = 'ladowanie' | 'ok' | 'blad';

/**
 * Zakładka "Sprawy" karty obywatela - woła realne `GET /api/sprawy?obywatelId=<id>` przez
 * `SprawyApi` (ten sam endpoint, co moduł Sprawy), sterowane sygnałem obywatela z
 * `KartaObywatelaKontekst`. `rozmiarStrony: 200` przekazane explicite, żeby nie wpaść na
 * domyślny limit backendu (20) i nie ucinać cicho listy spraw obywatela - ten widok nie ma
 * paginacji UI (zakładany mały wolumen spraw per obywatel), więc zamiast budować paginator
 * po prostu pobieramy od razu dość dużą stronę.
 */
@Component({
  selector: 'app-obywatel-sprawy',
  imports: [TableModule, TagModule, RouterLink, DatePipe, MessageModule],
  templateUrl: './obywatel-sprawy.html',
  styleUrl: './obywatel-sprawy.css',
})
export class ObywatelSprawy {
  private readonly kontekst = inject(KartaObywatelaKontekst);
  private readonly api = inject(SprawyApi);

  private readonly status = signal<StatusWczytywania>('ladowanie');
  protected readonly wczytywanie = computed(() => this.status() === 'ladowanie');
  protected readonly komunikatBledu = signal<string | null>(null);
  protected readonly sprawy = signal<Sprawa[]>([]);

  constructor() {
    toObservable(this.kontekst.obywatel)
      .pipe(
        map((obywatel) => obywatel?.id ?? null),
        filter((id): id is string => !!id),
        distinctUntilChanged(),
        switchMap((obywatelId) => {
          this.status.set('ladowanie');
          this.komunikatBledu.set(null);
          return this.api.szukaj({ obywatelId, rozmiarStrony: 200 }).pipe(
            map((strona): Sprawa[] => strona.dane),
            catchError((err: unknown) => {
              this.status.set('blad');
              this.komunikatBledu.set(
                komunikatBleduHttp(err, 'Nie udało się wczytać spraw obywatela.'),
              );
              return of(null as Sprawa[] | null);
            }),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((wynik) => {
        if (wynik !== null) {
          this.sprawy.set(wynik);
          this.status.set('ok');
        }
      });
  }
}
