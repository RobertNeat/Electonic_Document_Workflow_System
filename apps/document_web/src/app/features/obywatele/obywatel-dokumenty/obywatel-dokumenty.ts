import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { catchError, distinctUntilChanged, filter, map, of, switchMap } from 'rxjs';
import { TableModule } from 'primeng/table';
import { MessageModule } from 'primeng/message';
import type { DokumentObywatelaListElement } from '@electronic-document-workflow/document-shared-dto';
import { DokumentyApi } from '../../../core/api/dokumenty.api';
import { komunikatBleduHttp } from '../../../core/api/http-error';
import { KartaObywatelaKontekst } from '../karta-obywatela-kontekst.service';

type StatusWczytywania = 'ladowanie' | 'ok' | 'blad';

/**
 * Zakładka "Dokumenty" karty obywatela - woła realne `GET /api/dokumenty?obywatelId=<id>`
 * przez `DokumentyApi`, sterowane sygnałem obywatela z `KartaObywatelaKontekst` (ten sam
 * reaktywny pipeline co `ObywatelSprawy`/`ObywatelKorespondencja`).
 */
@Component({
  selector: 'app-obywatel-dokumenty',
  imports: [TableModule, DatePipe, MessageModule],
  templateUrl: './obywatel-dokumenty.html',
  styleUrl: './obywatel-dokumenty.css',
})
export class ObywatelDokumenty {
  private readonly kontekst = inject(KartaObywatelaKontekst);
  private readonly api = inject(DokumentyApi);

  private readonly status = signal<StatusWczytywania>('ladowanie');
  protected readonly wczytywanie = computed(() => this.status() === 'ladowanie');
  protected readonly komunikatBledu = signal<string | null>(null);
  protected readonly dokumenty = signal<DokumentObywatelaListElement[]>([]);

  constructor() {
    toObservable(this.kontekst.obywatel)
      .pipe(
        map((obywatel) => obywatel?.id ?? null),
        filter((id): id is string => !!id),
        distinctUntilChanged(),
        switchMap((obywatelId) => {
          this.status.set('ladowanie');
          this.komunikatBledu.set(null);
          return this.api.listaDlaObywatela(obywatelId).pipe(
            catchError((err: unknown) => {
              this.status.set('blad');
              this.komunikatBledu.set(
                komunikatBleduHttp(err, 'Nie udało się wczytać dokumentów obywatela.'),
              );
              return of(null as DokumentObywatelaListElement[] | null);
            }),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((wynik) => {
        if (wynik !== null) {
          this.dokumenty.set(wynik);
          this.status.set('ok');
        }
      });
  }
}
